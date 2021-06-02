package io.jenkins.plugins.opslevel.workflow;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.*;
import io.jenkins.plugins.opslevel.JobListener;
import io.jenkins.plugins.opslevel.OpsLevelConfig;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PipelineNotifyStep extends Step {

    private static final Logger logger = LoggerFactory.getLogger(JobListener.class);

    private final OpsLevelConfig config = new OpsLevelConfig();

    @DataBoundConstructor
    public PipelineNotifyStep() {
    }

    @DataBoundSetter
    public void setWebHookUrl(String webHookUrl) {
        config.webHookUrl = webHookUrl;
    }

    @DataBoundSetter
    public void setServiceAlias(String serviceAlias) {
        config.serviceAlias = serviceAlias;
    }

    @DataBoundSetter
    public void setEnvironment(String environment) {
        config.environment = environment;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        config.description = description;
    }

    @DataBoundSetter
    public void setDeployUrl(String deployUrl) {
        config.deployUrl = deployUrl;
    }

    @DataBoundSetter
    public void setDeployerId(String deployerId) {
        config.deployerId = deployerId;
    }

    @DataBoundSetter
    public void setDeployerEmail(String deployerEmail) {
        config.deployerEmail = deployerEmail;
    }

    @DataBoundSetter
    public void setDeployerName(String deployerName) {
        config.deployerName = deployerName;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new OpsLevelNotifyParamsExecute(context, this.config);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "opsLevelNotifyParams";
        }

        @Override
        public String getDisplayName() {
            return "What is this display name?";
        }
    }


    public static class OpsLevelNotifyParamsExecute extends SynchronousNonBlockingStepExecution<StepExecution> {

        OpsLevelNotifyParamsExecute(StepContext context, OpsLevelConfig config) {
            super(context);
        }

        @Override
        protected StepExecution run() throws Exception {
            logger.error("######################### RUNNING!");
            return null;
        }
    }
}
