package io.jenkins.plugins.workflow;

import io.jenkins.plugins.OpsLevelConfig;
import io.jenkins.plugins.OpsLevelGlobalConfigUI;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tasks.Notifier;
import hudson.tasks.BuildStepMonitor;
import hudson.Extension;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.model.AbstractProject;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kohsuke.stapler.DataBoundSetter;

public class OpsLevelFreestylePostBuildAction extends Notifier {

    private static final Logger logger = LoggerFactory.getLogger(OpsLevelGlobalConfigUI.class.getName());

    public String webHookUrl;
    public String serviceAlias;
    public String environment;
    public String description;
    public String deployUrl;
    public String deployerId;
    public String deployerEmail;
    public String deployerName;

    @DataBoundConstructor
    public OpsLevelFreestylePostBuildAction(String webHookUrl, String serviceAlias, String environment,
                                            String description, String deployUrl, String deployerId,
                                            String deployerEmail, String deployerName) {
        super();
        this.webHookUrl = cleanupValue(webHookUrl);
        this.serviceAlias = cleanupValue(serviceAlias);
        this.environment = cleanupValue(environment);
        this.description = cleanupValue(description);
        this.deployUrl = cleanupValue(deployUrl);
        this.deployerId = cleanupValue(deployerId);
        this.deployerEmail = cleanupValue(deployerEmail);
        this.deployerName = cleanupValue(deployerName);
    }

    private String cleanupValue(String someValue) {
        if (someValue == null) {
            return "";
        }
        return someValue.trim();
    }

    public OpsLevelConfig generateOpsLevelConfig() {
        OpsLevelConfig config = new OpsLevelConfig();
        config.webHookUrl = this.webHookUrl;
        config.serviceAlias = this.serviceAlias;
        config.environment = this.environment;
        config.description = this.description;
        config.deployUrl = this.deployUrl;
        config.deployerId = this.deployerId;
        config.deployerEmail = this.deployerEmail;
        config.deployerName = this.deployerName;
        return config;
    }

    public String getWebHookUrl() {
        logger.warn("GETTING WEBHOOk <<<<<<<<<<<<<");
        return this.webHookUrl;
    }

    @DataBoundSetter
    public void setWebHookUrl(String webHookUrl) {
        // TODO: setters may not be used? Confirm this
        logger.warn("SETTING WEBHOOK <<<<<<<<<<<<<<<<<<<");
        this.webHookUrl = webHookUrl;
    }

    public String getServiceAlias() {
        return this.serviceAlias;
    }

    @DataBoundSetter
    public void setServiceAlias(String serviceAlias) {
        this.serviceAlias = serviceAlias;
    }

    public String getEnvironment() {
        return this.environment;
    }

    @DataBoundSetter
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDescription() {
        return this.description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeployUrl() {
        return this.deployUrl;
    }

    @DataBoundSetter
    public void setDeployUrl(String deployUrl) {
        this.deployUrl = deployUrl;
    }

    public String getDeployerId() {
        return this.deployerId;
    }

    @DataBoundSetter
    public void setDeployerId(String deployerId) {
        this.deployerId = deployerId;
    }

    public String getDeployerEmail() {
        return this.deployerEmail;
    }

    @DataBoundSetter
    public void setDeployerEmail(String deployerEmail) {
        this.deployerEmail = deployerEmail;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

   @Override
   public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
   throws InterruptedException, IOException {
       listener.getLogger().println("Running OpsLevel Integration plugin...");
       return true;
   }

    @Override
    public WebHookPublisherDescriptor getDescriptor() {
        return (WebHookPublisherDescriptor) super.getDescriptor();
    }
    @Extension
    public static class WebHookPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Modify OpsLevel notifications for this build";
        }
    }
}
