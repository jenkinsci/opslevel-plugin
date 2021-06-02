package io.jenkins.plugins.opslevel.workflow;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.*;
import io.jenkins.plugins.opslevel.GlobalConfigUI;
import io.jenkins.plugins.opslevel.JobListener;
import io.jenkins.plugins.opslevel.OpsLevelConfig;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class PipelineNotifyStep extends Step {

    private static final Logger logger = LoggerFactory.getLogger(JobListener.class);

    private String webHookUrl = "";
    private String serviceAlias = "";
    private String environment = "";
    private String description = "";
    private String deployUrl = "";
    private String deployerId = "";
    private String deployerEmail = "";
    private String deployerName = "";

    @DataBoundConstructor
    public PipelineNotifyStep() {
    }

    @DataBoundSetter
    public void setWebHookUrl(String webHookUrl) {
        this.webHookUrl = webHookUrl;
    }

    @DataBoundSetter
    public void setServiceAlias(String serviceAlias) {
        this.serviceAlias = serviceAlias;
    }

    @DataBoundSetter
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @DataBoundSetter
    public void setDeployUrl(String deployUrl) {
        this.deployUrl = deployUrl;
    }

    @DataBoundSetter
    public void setDeployerId(String deployerId) {
        this.deployerId = deployerId;
    }

    @DataBoundSetter
    public void setDeployerEmail(String deployerEmail) {
        this.deployerEmail = deployerEmail;
    }

    @DataBoundSetter
    public void setDeployerName(String deployerName) {
        this.deployerName = deployerName;
    }

    @Override
    public StepExecution start(StepContext context) {
        OpsLevelConfig config = new OpsLevelConfig();
        config.webHookUrl = this.webHookUrl;
        config.serviceAlias = this.serviceAlias;
        config.environment = this.environment;
        config.description = this.description;
        config.deployUrl = this.deployUrl;
        config.deployerId = this.deployerId;
        config.deployerEmail = this.deployerEmail;
        config.deployerName = this.deployerName;
        return new OpsLevelNotifyParamsExecute(context, config);
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
            return getFunctionName();
        }
    }




    public static class OpsLevelNotifyParamsExecute extends SynchronousStepExecution<StepExecution> {

        private final OpsLevelConfig config;
        private Run run = null;
        private TaskListener listener = null;

        OpsLevelNotifyParamsExecute(StepContext context, OpsLevelConfig config) {
            super(context);
            this.config = config;
            TaskListener listener = null;
            try {
                this.run = context.get(Run.class);
                this.listener = context.get(TaskListener.class);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            logger.error("$$$$$$$$$ run: {}", run);
        }

        @Override
        protected StepExecution run() throws Exception {
            if (this.run == null || this.listener == null) {
                return null;
            }

            logger.error("######################### RUNNING!\n {}", this.config);

            OpsLevelJobProperty jobProp = new OpsLevelJobProperty();
            try {
                this.run.getParent().addProperty(jobProp);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            OpsLevelConfig globalConfig = new GlobalConfigUI.DescriptorImpl().getOpsLevelConfig();
            this.config.populateEmptyValuesFrom(globalConfig);
            new JobListener().postSuccessfulDeployToOpsLevel(this.run, this.listener, this.config);

            logger.error("######################### DONE!");

            return null;
        }
    }
}
