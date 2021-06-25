package io.jenkins.plugins.opslevel.workflow;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.opslevel.JobListener;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.*;
import org.junit.Assert;
import okhttp3.mockwebserver.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.Reader;
import java.io.StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class PostBuildActionTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    MockWebServer server = new MockWebServer();

    @Test
    public void testSuccessSimpliestCase() throws Exception {
        /*
            Ensure our plugin behaves correctly with no git and no overrides - just a URL
        */
        server.start();
        server.enqueue(new MockResponse().setBody("{\"result\": \"ok\"}"));
        String webhookUrl = server.url("").toString(); // .url("") means root path. Result will be http://<host>:<port>/
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new PostBuildAction(
                true,
                webhookUrl,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // The build console is the output shown to the user in Jenkins' UI
        Reader reader = build.getLogText().readAll();
        String consoleOutput = IOUtils.toString(reader);
        reader.close();
        log.debug("Build console output:\n{}", consoleOutput);

        assertThat(consoleOutput, containsString("Publishing deploy to OpsLevel via: " + webhookUrl));
        assertThat(consoleOutput, containsString("Response: {\"result\": \"ok\"}"));

        RecordedRequest request = server.takeRequest();
        String httpRequestUrl = request.toString();
        Assert.assertThat(httpRequestUrl, startsWith("POST /?agent=jenkins"));
        Assert.assertThat(httpRequestUrl, endsWith("HTTP/1.1"));

        String requestBody = request.getBody().readUtf8();
        JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        JsonObject payload = jsonReader.readObject();
        jsonReader.close();
        log.debug("Post Body:\n{}", requestBody);

        Assert.assertTrue(payload.containsKey("dedup_id"));
        Assert.assertEquals(payload.getString("deploy_number"), "1");
        assertThat (payload.getString("deploy_url"),  endsWith("/jenkins/job/test0/1/"));
        Assert.assertTrue(payload.containsKey("deployed_at"));
        Assert.assertEquals(payload.getString("description"), "Jenkins Deploy #1");
        Assert.assertEquals(payload.getString("environment"), "Production");
        Assert.assertEquals(payload.getString("service"), "jenkins:test0");

        server.shutdown();
    }

    @Test
    public void testSuccessWithGit() throws Exception {
        /*
            Ensure our plugin behaves correctly for a build with git
        */

        server.start();
        server.enqueue(new MockResponse().setBody("{\"result\": \"ok\"}"));
        String webhookUrl = server.url("").toString(); // .url("") means root path. Result will be http://<host>:<port>/
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new PostBuildAction(
                true,
                webhookUrl,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));

        project.setScm(new ExtractResourceSCM(getClass().getResource("/project-with-git.zip")));
        mockJenkinsEnvVar("GIT_COMMIT", "500ca67ed52a9ca20f3181e618347e61f86a0625");
        mockJenkinsEnvVar("GIT_BRANCH", "origin/master");

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // The build console is the output shown to the user in Jenkins' UI
        Reader reader = build.getLogText().readAll();
        String consoleOutput = IOUtils.toString(reader);
        reader.close();
        log.debug("Build console output:\n{}", consoleOutput);

        assertThat(consoleOutput, containsString("Publishing deploy to OpsLevel via: " + webhookUrl));
        assertThat(consoleOutput, containsString("Response: {\"result\": \"ok\"}"));

        RecordedRequest request = server.takeRequest();
        String httpRequestUrl = request.toString();
        Assert.assertThat(httpRequestUrl, startsWith("POST /?agent=jenkins"));
        Assert.assertThat(httpRequestUrl, endsWith("HTTP/1.1"));

        String requestBody = request.getBody().readUtf8();
        JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        JsonObject payload = jsonReader.readObject();
        jsonReader.close();
        log.debug("Post Body:\n{}", requestBody);

        Assert.assertTrue(payload.containsKey("dedup_id"));
        Assert.assertEquals(payload.getString("deploy_number"), "1");
        assertThat (payload.getString("deploy_url"),  endsWith("/jenkins/job/test0/1/"));
        Assert.assertTrue(payload.containsKey("deployed_at"));
        Assert.assertEquals(payload.getString("description"), "Fix typo");
        Assert.assertEquals(payload.getString("environment"), "Production");
        Assert.assertEquals(payload.getString("service"), "jenkins:test0");
        JsonObject commitJson = payload.getJsonObject("commit");
        Assert.assertEquals(commitJson.getString("sha"), "500ca67ed52a9ca20f3181e618347e61f86a0625");
        Assert.assertEquals(commitJson.getString("branch"), "origin/master");
        Assert.assertEquals(commitJson.getString("message"), "Fix typo");

        server.shutdown();
    }

    @Test
    public void testSuccessWithGitAndOverrides() throws Exception {
        /*
            Ensure our plugin behaves correctly for a build with git and overrides
        */

        server.start();
        server.enqueue(new MockResponse().setBody("{\"result\": \"ok\"}"));
        String webhookUrl = server.url("").toString(); // .url("") means root path. Result will be http://<host>:<port>/
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new PostBuildAction(
                true,
                webhookUrl,
                "the-lake-house-device",
                "staging",
                "Deploy to staging from Jenkins build #${BUILD_NUMBER}",
                "http://staging.example.org/",
                "Shlorpian-24601",
                "yumyulack@example.org",
                "Yumyulack"
        ));

        project.setScm(new ExtractResourceSCM(getClass().getResource("/project-with-git.zip")));
        mockJenkinsEnvVar("GIT_COMMIT", "500ca67ed52a9ca20f3181e618347e61f86a0625");
        mockJenkinsEnvVar("GIT_BRANCH", "origin/master");

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // The build console is the output shown to the user in Jenkins' UI
        Reader reader = build.getLogText().readAll();
        String consoleOutput = IOUtils.toString(reader);
        reader.close();
        log.debug("Build console output:\n{}", consoleOutput);

        assertThat(consoleOutput, containsString("Publishing deploy to OpsLevel via: " + webhookUrl));
        assertThat(consoleOutput, containsString("Response: {\"result\": \"ok\"}"));

        RecordedRequest request = server.takeRequest();
        String httpRequestUrl = request.toString();
        Assert.assertThat(httpRequestUrl, startsWith("POST /?agent=jenkins"));
        Assert.assertThat(httpRequestUrl, endsWith("HTTP/1.1"));

        String requestBody = request.getBody().readUtf8();
        JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        JsonObject payload = jsonReader.readObject();
        jsonReader.close();
        log.debug("Post Body:\n{}", requestBody);

        Assert.assertTrue(payload.containsKey("dedup_id"));
        Assert.assertEquals(payload.getString("deploy_number"), "1");
        Assert.assertEquals(payload.getString("deploy_url"),  "http://staging.example.org/");
        Assert.assertTrue(payload.containsKey("deployed_at"));
        Assert.assertEquals(payload.getString("description"), "Deploy to staging from Jenkins build #1");
        Assert.assertEquals(payload.getString("environment"), "staging");
        Assert.assertEquals(payload.getString("service"), "the-lake-house-device");
        JsonObject commitJson = payload.getJsonObject("commit");
        Assert.assertEquals(commitJson.getString("sha"), "500ca67ed52a9ca20f3181e618347e61f86a0625");
        Assert.assertEquals(commitJson.getString("branch"), "origin/master");
        Assert.assertThat(commitJson.getString("message"), startsWith("Fix typo"));

        server.shutdown();
    }

    @Test
    public void testShowsPostErrorInConsole() throws Exception {
        /*
            Ensure our plugin shows errors in the Jenkins build console
        */

        server.start();
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"error\":\"Example not found\"}"));
        String webhookUrl = server.url("").toString(); // .url("") means root path. Result will be http://<host>:<port>/
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new PostBuildAction(
                true,
                webhookUrl,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // The build console is the output shown to the user in Jenkins' UI
        Reader reader = build.getLogText().readAll();
        String consoleOutput = IOUtils.toString(reader);
        reader.close();
        log.debug("Build console output:\n{}", consoleOutput);

        assertThat(consoleOutput, containsString("Publishing deploy to OpsLevel via: " + webhookUrl));
        assertThat(consoleOutput, containsString("Response: {\"error\":\"Example not found\"}"));

        server.shutdown();
    }

    private void mockJenkinsEnvVar(String name, String value) {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put(name, value);
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

}
