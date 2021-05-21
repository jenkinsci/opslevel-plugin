package io.jenkins.plugins;

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import okhttp3.*;

import java.util.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.annotation.Nonnull;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class JobListener extends RunListener<AbstractBuild> {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    public JobListener() {
        super(AbstractBuild.class);
        client = new OkHttpClient();
    }

    @Override
    public void onCompleted(AbstractBuild build, @Nonnull TaskListener listener) {
        WebHookPublisher publisher = GetWebHookPublisher(build);
        if (publisher == null) {
            return;
        }
        Result result = build.getResult();
        if (result == null) {
            return;
        }

        try {
            JsonObject json = buildDeployPayload(publisher, build, listener);
            // Send the payload
            String webHookUrl = publisher.webHookUrl;
            httpPost(webHookUrl, json);
        }
        catch(Exception e) {
            log.error("Error: {}. Could not publish deploy to OpsLevel", e.toString());
        }

    }

    private WebHookPublisher GetWebHookPublisher(AbstractBuild build) {
        for (Object publisher : build.getProject().getPublishersList().toMap().values()) {
            if (publisher instanceof WebHookPublisher) {
                return (WebHookPublisher) publisher;
            }
        }
        return null;
    }

    private void httpPost(String url, JsonObject json) {
        String jsonString = json.toString();
        log.info("Sending OpsLevel Integration payload:\n{}", jsonString);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonString);
        Request request = new Request.Builder().url(url).post(body).build();
        try {
            Response response = client.newCall(request).execute();
            log.info("Invocation of webhook {} successful", url);
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                log.info("Response: {}", responseBody.string());
            }
        } catch (Exception e) {
            log.info("Invocation of webhook {} failed", url, e);
        }
    }

    private JsonObject buildDeployPayload(WebHookPublisher publisher, AbstractBuild build, TaskListener listener) throws InterruptedException, IOException {
        // Leaving a sample payload here for visibility while developing.
        // {
        //     "dedup_id": "9ae54794-dfc5-4ac8-b1b5-78789f20f3f8",
        //     "service": "shopping_cart",                            // CAN OVERRIDE
        //     "deployer": {
        //       "id": "1a9f841f-9a3d-4423-a05a-7e9c31a02b16",        // CAN OVERRIDE
        //       "email": "mscott@example.com",                       // CAN OVERRIDE
        //       "name": "Michael Scott"                              // CAN OVERRIDE
        //     },
        //     "deployed_at": "'"$(date -u '+%FT%TZ')"'",
        //     "environment": "Production",                           // CAN OVERRIDE
        //     "description": "Deployed by CI Pipeline: Deploy #234", // CAN OVERRIDE - needs var subs
        //     "deploy_url": "https://heroku.deploys.com",            // CAN OVERRIDE - needs var subss
        //     "deploy_number": "234",
        //     "commit": {
        //       "sha": "38d02f1d7aab64678a7ad3eeb2ad2887ce7253f5",
        //       "message": "Merge branch 'fix-tax-rate' into 'master'",
        //       "branch": "master",
        //       "date": "'"$(date -u '+%FT%TZ')"'",
        //       "committer_name": "Michael Scott",
        //       "committer_email": "mscott@example.com",
        //       "author_name": "Michael Scott",
        //       "author_email": "mscott@example.com",
        //       "authoring_date": "'"$(date -u '+%FT%TZ')"'"
        //     }

        EnvVars env = build.getEnvironment(listener);

        // TODO: remove debugging: Printing env variables
        for (String key : env.keySet()) {
            log.info(key + ": " + env.get(key));
        }

        // Default to UUID. Perhaps allow this to be set with envVars ${JOB_NAME}_${BUILD_ID} / ${BUILD_TAG}
        String dedupId = UUID.randomUUID().toString();

        // It didn't make sense to allow overriding deploy number. Use the value from Jenkin
        String deployNumber = env.get("BUILD_NUMBER");

        // URL of the asset that was just deployed
        String deployUrl = stringSub(publisher.deployUrl, env);
        if (deployUrl == null) {
            deployUrl = getDeployUrl(build);
        }

        // ISO datetime with no milliseconds
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        String deployedAt = ZonedDateTime.now().format(dtf);

        // Description. Hopefully a useful default
        String description = stringSub(publisher.description, env);
        if (description == null) {
            description = stringSub("Jenkins Deploy #${BUILD_NUMBER}", env);
        }

        // Typically Test/Staging/Production
        String environment = stringSub(publisher.environment, env);
        if (environment == null) {
            environment = "Production";
        }

        // Couple possibilities for service name:
        //   ${JOB_NAME}
        //   based on GIT url -> https://github.com/repo/service_name
        //   Could open opslevel.yml and get it directly (TODO: how to do this?)
        String service = env.get("JOB_NAME");
        if(publisher.serviceName != null) {
            service = stringSub(publisher.serviceName, env);
        }

        // Details of who deployed, if available
        JsonObject deployerJson = buildDeployerJson(publisher, env);

        // Details of the commit, if available
        JsonObject commitJson = buildCommitJson(env);

        JsonObjectBuilder payload = Json.createObjectBuilder();
        payload.add("dedup_id", dedupId);
        payload.add("deploy_number", deployNumber);
        payload.add("deploy_url", deployUrl);
        payload.add("deployed_at", deployedAt);
        payload.add("description", description);
        payload.add("environment", environment);
        payload.add("service", service);

        if (deployerJson != null) {
            payload.add("deployer", deployerJson);
        }

        if (commitJson != null) {
            payload.add("commit", commitJson);
        }

        return payload.build();
    }

    private String stringSub(String templateString, EnvVars env) {
        StringSubstitutor sub = new StringSubstitutor(env);
        return sub.replace(templateString);
    }

    private String getDeployUrl(AbstractBuild build) {
        try {
            // Full URL, if Jenkins Location is set (on /configure page)
            // By default the UI shows http://localhost:8080/jenkins/
            // but the actual value is unset so this function throws an exception
            return build.getAbsoluteUrl();
        }
        catch(java.lang.IllegalStateException e) {
            // build.getUrl() always works but returns a relative path
            return "http://jenkins-location-is-not-set.local/" + build.getUrl();
        }
    }

    private JsonObject buildDeployerJson(WebHookPublisher publisher, EnvVars env) {
        JsonObjectBuilder deployer = Json.createObjectBuilder();
        // TODO: how to access details from the various GIT plugins and BitBucket plugin?
        String deployerId = publisher.deployerId;
        String deployerName = publisher.deployerName; // "jenkins";
        String deployerEmail = publisher.deployerEmail; // "jenkins@example.com";

        if (deployerId == null && deployerName == null && deployerEmail == null) {
            return null;
        }

        if (deployerId != null) {
            deployer.add("id", stringSub(deployerId, env));
        }

        if (deployerName != null) {
            deployer.add("name", stringSub(deployerName, env));
        }

        if (deployerEmail != null) {
            deployer.add("email", stringSub(deployerEmail, env));
        }

        return deployer.build();
    }

    private JsonObject buildCommitJson(EnvVars env) {
        String commitHash = env.get("GIT_COMMIT");
        if (commitHash == null) {
            return null;
        }
        JsonObjectBuilder commit = Json.createObjectBuilder();
        commit.add("sha", commitHash);

        String commitBranch = env.get("GIT_BRANCH");
        if (commitBranch != null) {
            commit.add("branch", commitBranch);
        }

        return commit.build();
    }
}
