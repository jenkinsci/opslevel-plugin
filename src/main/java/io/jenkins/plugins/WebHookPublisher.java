package io.jenkins.plugins;

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

public class WebHookPublisher extends Notifier {
    public String webHookUrl;
    public String serviceAlias;
    public String environment;
    public String description;
    public String deployUrl;
    public String deployerId;
    public String deployerEmail;
    public String deployerName;

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    @DataBoundConstructor
    public WebHookPublisher(String webHookUrl, String serviceAlias, String environment, String description,
                            String deployUrl, String deployerId, String deployerEmail, String deployerName) {
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
        log.error("DEALING WITH VALUE: \"{}\"", someValue);
        if (someValue == null) {
            return null;
        }
        if (someValue.trim().isEmpty()) {
            return null;
        }
        return someValue.trim();
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

    public String getDefaultserviceAlias() {
        return "from a function";
    }

    @Extension
    public static class WebHookPublisherDescriptor extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish succesful build to OpsLevel";
        }
    }
}
