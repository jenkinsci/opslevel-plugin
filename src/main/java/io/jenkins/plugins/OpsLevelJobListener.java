package io.jenkins.plugins;

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.annotation.Nonnull;

import io.jenkins.plugins.workflow.OpsLevelFreestylePostBuildAction;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class OpsLevelJobListener extends RunListener<AbstractBuild> {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;

    private static final Logger log = LoggerFactory.getLogger(OpsLevelJobListener.class);

    public OpsLevelJobListener() {
        super(AbstractBuild.class);
        client = new OkHttpClient();
    }

    @Override
    public void onCompleted(AbstractBuild build, @Nonnull TaskListener listener) {
        PrintStream buildConsole = listener.getLogger();

        Result result = build.getResult();
        if (result == null) {
            log.debug("OpsLevel notifier: stop because build has no result");
            return;
        }

        // Send the webhook on successful deploys. UNSTABLE could be successful depending on how the pipeline is set up
        if (!result.equals(Result.SUCCESS) && !result.equals(Result.UNSTABLE) ) {
            log.debug("OpsLevel notifier: stop because build status is " + result.toString());
            return;
        }

        OpsLevelConfig opsLevelConfig;
        OpsLevelFreestylePostBuildAction publisher = GetWebHookPublisher(build);
        if (publisher == null) {
             log.debug("OpsLevel notifier: publisher not found on this build");
            opsLevelConfig = new OpsLevelConfig();
        } else {
            opsLevelConfig = publisher.generateOpsLevelConfig();
        }

        OpsLevelConfig globalConfig = new OpsLevelGlobalConfigUI.DescriptorImpl().getOpsLevelConfig();
        if (opsLevelConfig.webHookUrl.isEmpty() && globalConfig.webHookUrl.isEmpty()) {
            buildConsole.println("OpsLevel notifier: stop because webhook URL not configured");
            return;
        }

        opsLevelConfig.populateEmptyValuesFrom(globalConfig);

        postSuccessfulDeployToOpsLevel(build, listener, opsLevelConfig);
    }


    public void postSuccessfulDeployToOpsLevel(AbstractBuild build, @Nonnull TaskListener listener,
                                               OpsLevelConfig opsLevelConfig) {
        PrintStream buildConsole = listener.getLogger();

        try {
            JsonObject payload = buildDeployPayload(opsLevelConfig, build, listener);
            String webHookUrl = opsLevelConfig.webHookUrl;
            buildConsole.print("Publishing deploy to OpsLevel via: " + webHookUrl + "\n");
            httpPost(webHookUrl, payload, buildConsole);
        }
        catch(Exception e) {
            String message = e.toString() + ". Could not publish deploy to OpsLevel.\n";
            log.error(message);
            buildConsole.print("Error :" + message);
        }
    }

    private OpsLevelFreestylePostBuildAction GetWebHookPublisher(AbstractBuild build) {
        for (Object publisher : build.getProject().getPublishersList().toMap().values()) {
            if (publisher instanceof OpsLevelFreestylePostBuildAction) {
                return (OpsLevelFreestylePostBuildAction) publisher;
            }
        }
        return null;
    }

    private void httpPost(String webHookUrl, JsonObject payload, PrintStream buildConsole) throws IOException {
        // Get the plugin version to pass through as a request parameter
        final Properties properties = new Properties();
        String version = "";
        try {
            // TODO: In development this seems to pull from src/main/config.properties, instead of target/classes/properties
            //       Once the plugin is compiled it will get the correct version string, but we could not figure out how
            //       to get it looking at the right place in development
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            version = properties.getProperty("plugin.version");
        }
        catch (IOException e) {
            log.error("Project properties does not exist. {}", e.toString());
        }

        if (webHookUrl == null) {
            throw new InputMismatchException("Webhook URL is missing");
        }
        HttpUrl httpUrl = HttpUrl.parse(webHookUrl);
        if (httpUrl == null) {
            throw new InputMismatchException("Webhook URL is invalid");
        }
        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        // Append plugin version as query param for visibility
        String agent = "jenkins-" + version;
        HttpUrl url = httpBuilder.addQueryParameter("agent", agent).build();

        // Build the body
        String jsonString = payload.toString();
        log.info("Sending OpsLevel Integration payload:\n{}", jsonString);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonString);

        // Finally, put the request together
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();

        try {
            Response response = client.newCall(request).execute();
            log.debug("Invocation of OpsLevel webhook {} successful", url);
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String message = "OpsLevel Response: " + responseBody.string() + "\n";
                buildConsole.print(message);
                log.info(message);
            }
        } catch (Exception e) {
            log.warn("Invocation of OpsLevel webhook {} failed: {}", url, e.toString());
            throw e;
        }
    }

    private JsonObject buildDeployPayload(OpsLevelConfig opsLevelConfig, AbstractBuild build, TaskListener listener)
    throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);

        // Default to UUID. Perhaps allow this to be set with envVars ${JOB_NAME}_${BUILD_ID} / ${BUILD_TAG}
        String dedupId = UUID.randomUUID().toString();

        // It didn't make sense to allow overriding deploy number. Use the value from Jenkin
        String deployNumber = env.get("BUILD_NUMBER");

        // URL of the asset that was just deployed
        String deployUrl = stringSub(opsLevelConfig.deployUrl, env);
        if (deployUrl.isEmpty()) {
            deployUrl = getDeployUrl(build);
        }

        // ISO datetime with no milliseconds
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        String deployedAt = ZonedDateTime.now().format(dtf);

        // Typically Test/Staging/Production
        String environment = stringSub(opsLevelConfig.environment, env);
        if (environment.isEmpty()) {
            environment = "Production";
        }

        // Conform to kubernetes conventions with this prefix
        String serviceAlias = stringSub(opsLevelConfig.serviceAlias, env);
        if (serviceAlias.isEmpty()) {
            serviceAlias = "jenkins:" + env.get("JOB_NAME");
        }

        // Details of who deployed, if available
        JsonObject deployerJson = buildDeployerJson(opsLevelConfig, env);

        // Details of the commit, if available
        JsonObject commitJson = buildCommitJson(env);

        // Description that is hopefully meaningful
        String description = stringSub(opsLevelConfig.description, env);
        if (description.isEmpty()) {
            if (commitJson != null && commitJson.containsKey("message")) {
                description = commitJson.getString("message");
            } else {
                description = stringSub("Jenkins Deploy #${BUILD_NUMBER}", env);
            }
        }

        JsonObjectBuilder payload = Json.createObjectBuilder();
        payload.add("dedup_id", dedupId);
        payload.add("deploy_number", deployNumber);
        payload.add("deploy_url", deployUrl);
        payload.add("deployed_at", deployedAt);
        payload.add("description", description);
        payload.add("environment", environment);
        payload.add("service", serviceAlias);

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

    private JsonObject buildDeployerJson(OpsLevelConfig opsLevelConfig, EnvVars env) {
        // TODO: how to access the Jenkins user who triggered this build?
        String deployerId = opsLevelConfig.deployerId;
        String deployerName = opsLevelConfig.deployerName;
        String deployerEmail = opsLevelConfig.deployerEmail;

        if (deployerId.isEmpty() && deployerName.isEmpty() && deployerEmail.isEmpty()) {
            return null;
        }

        JsonObjectBuilder deployer = Json.createObjectBuilder();

        if (!deployerId.isEmpty()) {
            deployer.add("id", stringSub(deployerId, env));
        }

        if (!deployerName.isEmpty()) {
            deployer.add("name", stringSub(deployerName, env));
        }

        if (!deployerEmail.isEmpty()) {
            deployer.add("email", stringSub(deployerEmail, env));
        }

        return deployer.build();
    }

    private JsonObject buildCommitJson(EnvVars env) {
        String commitHash = env.get("GIT_COMMIT");
        if (commitHash == null) {
            // This build doesn't use git
            return null;
        }
        JsonObjectBuilder commitJson = Json.createObjectBuilder();
        commitJson.add("sha", commitHash);

        String commitBranch = env.get("GIT_BRANCH");
        if (commitBranch != null) {
            commitJson.add("branch", commitBranch);
        }

        String commitMessage = getGitCommitMessage(env);
        if (commitMessage != null) {
            commitJson.add("message", commitMessage);
        }

        return commitJson.build();
    }

    private String getGitCommitMessage(EnvVars env) {
        String output = execCmd(env, "git", "show", "--pretty=%s");
        if (output == null) {
            return null;
        }
        String[] result = output.split(System.lineSeparator(), 2);
        return result[0];
    }

    private static String execCmd(EnvVars env, String... cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(env.get("WORKSPACE")));
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int exitCode = 0;
        try {
            exitCode = p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        if (exitCode != 0) {
            String stderr = null;
            try {
                stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String strCmd = String.join(" ", cmd);
            log.warn("Failed to execute command: {}. Exit code: {}. Stderr:: {}", strCmd, exitCode, stderr);
        }

        try {
            String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            return stdout;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
