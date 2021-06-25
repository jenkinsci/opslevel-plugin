package io.jenkins.plugins.opslevel;

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
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

import io.jenkins.plugins.opslevel.workflow.PostBuildAction;
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
public class JobListener extends RunListener<Run<?, ?>> {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;

    private static final Logger logger = LoggerFactory.getLogger(JobListener.class);

    public JobListener() {
        super();
        client = new OkHttpClient();
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        PrintStream buildConsole = listener.getLogger();

        Result result = run.getResult();
        if (result == null) {
            logger.debug("OpsLevel notifier: skipping because this run has no result");
            return;
        }

        // Send the webhook on successful deploys. UNSTABLE could be successful depending on how the pipeline is set up
        if (!result.equals(Result.SUCCESS) && !result.equals(Result.UNSTABLE) ) {
            logger.debug("OpsLevel notifier: skipping because run status is " + result.toString());
            return;
        }

        Job project = run.getParent();
        OpsLevelConfig jobConfig = null;
        OpsLevelConfig globalConfig = new GlobalConfigUI.DescriptorImpl().getOpsLevelConfig();

        // Freestyle builds can add our notifier as a post-build action.
        // If present it may contain params
        if (project instanceof FreeStyleProject) {
            jobConfig = GetOpsLevelConfigFromFreestyleJob((FreeStyleProject) project);
        }
        if (jobConfig == null) {
            if (!globalConfig.run) {
                logger.debug("OpsLevel notifier: skipping because it's disabled globally");
                return;
            }
            logger.debug("OpsLevel notifier: publisher not found on this project");
            jobConfig = new OpsLevelConfig();
        } else if (!jobConfig.run) {
            String message = "OpsLevel notifier: skipping because this project disabled notify";
            buildConsole.println(message);
            logger.debug(message);
            return;
        }

        // Notifications can be disabled based on project name
        if (!globalConfig.ignoreList.isEmpty()) {
            String[] ignoredJobs = globalConfig.ignoreList.split(",");
            String thisJobName = project.getFullDisplayName().trim();
            for (String jobName : ignoredJobs) {
                if (jobName.trim().equals(thisJobName)) {
                    String message = "OpsLevel notifier: skipping because global configuration says to ignore " +
                                     "builds named \"" + jobName + "\"";
                    buildConsole.println(message);
                    logger.debug(message);
                    return;
                }
            }
        }

        jobConfig.populateEmptyValuesFrom(globalConfig);

        if (jobConfig.webhookUrl.isEmpty()) {
            logger.warn("OpsLevel notifier: skipping because webhook URL not configured");
            return;
        }

        // Pipelines are different from freestyle builds. Pipelines can notify multiple times, or not at
        // all (suppress the global notifier)
        // If this property is null, respect the global notifier. If it's non-null, our notifier appeared
        // somewhere in the pipeline script and has been handled already - nothing to do here.
        if (project.getProperty(OpsLevelJobProperty.class) != null) {
            logger.debug("OpsLevel notifier: skipping because pipeline contained OpsLevel notify step");
            return;
        }

        postDeployToOpsLevel(run, listener, jobConfig);
    }

    public void postDeployToOpsLevel(Run run, @Nonnull TaskListener listener,
                                     OpsLevelConfig opsLevelConfig) {
        PrintStream buildConsole = listener.getLogger();

        String webhookUrl = opsLevelConfig.webhookUrl;
        try {
            JsonObject payload = buildDeployPayload(opsLevelConfig, run, listener);
            buildConsole.println("Publishing deploy to OpsLevel via: " + webhookUrl);
            httpPost(webhookUrl, payload, buildConsole);
        } catch(Exception e) {
            String message = e.toString() + ". Could not publish deploy to OpsLevel.";
            logger.error(message);
            buildConsole.println("Error :" + message);
        }
    }

    private OpsLevelConfig GetOpsLevelConfigFromFreestyleJob(FreeStyleProject project) {
        for (Object publisher : project.getPublishersList().toMap().values()) {
            if (publisher instanceof PostBuildAction) {
                return ((PostBuildAction) publisher).generateOpsLevelConfig();
            }
        }
        return null;
    }

    private void httpPost(String webhookUrl, JsonObject payload, PrintStream buildConsole) throws IOException {
        // Get the plugin version to pass through as a request parameter
        final Properties properties = new Properties();
        String version = "";
        try {
            // TODO: In development this seems to pull from src/main/config.properties, instead of target/classes/properties
            //       Once the plugin is compiled it will get the correct version string, but we could not figure out how
            //       to get it looking at the right place in development
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            version = properties.getProperty("plugin.version");
        } catch (IOException e) {
            logger.error("Project properties does not exist. {}", e.toString());
        }

        HttpUrl httpUrl = HttpUrl.parse(webhookUrl);
        if (httpUrl == null) {
            throw new InputMismatchException("Webhook URL is invalid");
        }
        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        // Append plugin version as query param for visibility
        String agent = "jenkins-" + version;
        HttpUrl url = httpBuilder.addQueryParameter("agent", agent).build();

        // Build the body
        String jsonString = payload.toString();
        logger.debug("Sending OpsLevel Integration payload:\n{}", jsonString);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonString);

        // Finally, put the request together
        Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();

        try {
            Response response = client.newCall(request).execute();
            logger.debug("Invocation of OpsLevel webhook {} successful", url);
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String message = "OpsLevel Response: " + responseBody.string() + "\n";
                buildConsole.print(message);
                logger.info(message);
            }
        } catch (Exception e) {
            logger.warn("Invocation of OpsLevel webhook {} failed: {}", url, e.toString());
            throw e;
        }
    }

    private JsonObject buildDeployPayload(OpsLevelConfig opsLevelConfig, Run run, TaskListener listener)
    throws InterruptedException, IOException {
        EnvVars env = run.getEnvironment(listener);

        // Default to UUID. Perhaps allow this to be set with envVars ${JOB_NAME}_${BUILD_ID} / ${BUILD_TAG}
        String dedupId = UUID.randomUUID().toString();

        // It didn't make sense to allow overriding deploy number. Use the value from Jenkin
        String deployNumber = env.get("BUILD_NUMBER");

        // URL of the asset that was just deployed
        String deployUrl = stringSub(opsLevelConfig.deployUrl, env);
        if (deployUrl.isEmpty()) {
            deployUrl = getDeployUrl(run);
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

    private String getDeployUrl(Run run) {
        try {
            // Full URL, if Jenkins Location is set (on /configure page)
            // By default the UI shows http://localhost:8080/jenkins/
            // but the actual value is unset so this function throws an exception
            return run.getAbsoluteUrl();
        } catch(java.lang.IllegalStateException e) {
            // run.getUrl() always works but returns a relative path
            return "http://jenkins-location-is-not-set.local/" + run.getUrl();
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
        String[] result = output.split("\n", 2);
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
            logger.warn("Failed to execute command: {}. Exit code: {}. Stderr:: {}", strCmd, exitCode, stderr);
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
