package io.jenkins.plugins;

import hudson.FilePath;
import hudson.model.Result;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import org.jvnet.hudson.test.JenkinsRule;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Assert;

import io.jenkins.plugins.JobListener;

import static org.hamcrest.CoreMatchers.containsString;

public class WebhookPostTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void first() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        // TODO: change this to use HtmlUnit
        String logOutput = FileUtils.readFileToString(build.getLogFile());
//        MatcherAssert.assertThat(s, contains("+ echo hello"));
//        Assert.assertEquals(s, "+ echo hello");
        Assert.assertThat(logOutput, containsString("hello"));
    }

//    @Test
//    public void testSomethingAtLest() throws Exception {
//        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "my-jenkins-build");
//
//        p.setDefinition(new CpsFlowDefinition("def hook = registerWebhook(token: \"test-token\")\necho \"token=${hook.token}\"\ndef data = waitForWebhook(webhookToken: hook)\necho \"${data}\""));
//        JobListener jl = new JobListener();
//        WorkflowRun r = p.scheduleBuild2(0).waitForStart();
//
//        j.assertBuildStatus(null, r);
//
//        j.postJSON("webhook-step/test-token", content);
//
//        j.waitForCompletion(r);
//        j.assertBuildStatus(Result.SUCCESS, r);
//        j.assertLogContains("token=test-token", r);
//        j.assertLogContains("\"action\":\"done\"", r);
//    }
}