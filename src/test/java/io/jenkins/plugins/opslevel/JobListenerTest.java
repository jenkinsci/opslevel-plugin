package io.jenkins.plugins.opslevel;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import org.junit.Assert;
import okhttp3.mockwebserver.*;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
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
        Assert.assertEquals(server.getRequestCount(), 1);

        server.shutdown();
    }
}
