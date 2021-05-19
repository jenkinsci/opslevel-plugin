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
import javax.annotation.Nonnull;

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

        // Format the payload into a format OpsLevel can natively digest
        // DeployPayload payload = formatDeployPayload(publisher, build);
        try {
            JsonObject json = buildDeployPayload(publisher, build);
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
        log.info("Sending payload:\n{}", jsonString);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonString);
        Request request = new Request.Builder().url(url).post(body).build();
        try {
            Response response = client.newCall(request).execute();
            log.info("Invocation of webhook {} successful", url);
            log.info("Response: {}", response.body().string());
        } catch (Exception e) {
            log.info("Invocation of webhook {} failed", url, e);
        }
    }

    private JsonObject buildDeployPayload(WebHookPublisher publisher, AbstractBuild build) throws InterruptedException, IOException {
        // Leaving a sample payload here for visibility while developing.
        // {
        //     "dedup_id": "9ae54794-dfc5-4ac8-b1b5-78789f20f3f8",
        //     "service": "shopping_cart",
        //     "deployer": {
        //       "id": "1a9f841f-9a3d-4423-a05a-7e9c31a02b16",
        //       "email": "mscott@example.com",
        //       "name": "Michael Scott"
        //     },
        //     "deployed_at": "'"$(date -u '+%FT%TZ')"'",
        //     "environment": "Production",
        //     "description": "Deployed by CI Pipeline: Deploy #234",
        //     "deploy_url": "https://heroku.deploys.com",
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

        EnvVars env = build.getEnvironment();
        // XXX: Printing env variables
        if (env != null) {
            for (String key : env.keySet()) {
                log.info(key + ": " + env.get(key));
            }
        }

        // Default to UUID. Perhaps allow this to be set with envVars ${JOB_NAME}_${BUILD_ID} / ${BUILD_TAG}
        String dedup_id = UUID.randomUUID().toString();

        // Couple possibilities for service name:
        //   ${JOB_NAME}
        //   based on GIT url -> https://github.com/repo/service_name
        //   Could open opslevel.yml and get it directly
        String service = env.get("JOB_NAME");
        if(publisher.serviceName != null) {
            service = publisher.serviceName;
        }

        // TODO: Fixup deployer section. Unofortunately Git plugins don't seem to push the commiter info into env :(
        String deployer_email = "jenkins@example.com";
        String deployer_name= "jenkins";

        // format datetime for deployed_at
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        String deployed_at = ZonedDateTime.now().format(dtf);

        // environment: typically Test/Staging/Production
        // TODO: explore what env variables we could pull this from
        String environment = "Production";
        if(publisher.envName != null) {
            environment = publisher.envName;
        }

        // TODO: Add publisher.description override
        String description = "Jenkins Deploy #" + env.get("BUILD_NUMBER");

        // TODO: This requires jenkins ROOT URL to be set! This is fragile and we should first check if it has been set.
        String deploy_url = build.getAbsoluteUrl();
        String deploy_number = env.get("BUILD_NUMBER");

        // TODO: fixup commit section - we don't currently have access to committer/author bits
        String commit_sha = env.get("GIT_COMMIT");
        String commit_branch = env.get("GIT_BRANCH");


        // Build the JSON string
        JsonObject json = Json.createObjectBuilder()
            .add("service", service)
            .add("deployer", Json.createObjectBuilder()
                    .add("email", deployer_email)
                    .add("name", deployer_name))
            .add("deployed_at", deployed_at)
            .add("environment", environment)
            .add("description", description)
            .add("deploy_url", deploy_url)
            .add("deploy_number", deploy_number)
            .add("commit", Json.createObjectBuilder()
                .add("sha", commit_sha)
                .add("branch", commit_branch))
            .build();

        return json;
    }
}
