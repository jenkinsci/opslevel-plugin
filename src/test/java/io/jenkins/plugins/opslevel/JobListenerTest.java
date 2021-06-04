package io.jenkins.plugins.opslevel;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.opslevel.JobListener;
import io.jenkins.plugins.opslevel.workflow.PostBuildAction;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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
import java.io.StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobListenerTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    MockWebServer server = new MockWebServer();

    @Test
    public void testPipelineStepRunsAndStopsGlobalNotifier() throws Exception {
        server.start();
        server.enqueue(new MockResponse().setBody("{\"result\": \"ok\"}"));
        String webhookUrl = server.url("").toString(); // .url("") means root path. Result will be http://<host>:<port>/

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);

        project.setDefinition(new CpsFlowDefinition("opsLevelNotify webhookUrl: \"" + webhookUrl + "\"", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        // The build console is the output shown to the user in Jenkins' UI
        String consoleOutput = IOUtils.toString(build.getLogText().readAll());
        log.debug("Build console output:\n{}", consoleOutput);

        assertThat(consoleOutput, containsString("Publishing deploy to OpsLevel via: " + webhookUrl));
        assertThat(consoleOutput, containsString("Response: {\"result\": \"ok\"}"));

        RecordedRequest request = server.takeRequest();
        String httpRequestUrl = request.toString();
        Assert.assertEquals(httpRequestUrl, "POST /?agent=jenkins-1.0.0-SNAPSHOT HTTP/1.1");
        Assert.assertEquals(server.getRequestCount(), 1);

        server.shutdown();
    }
}