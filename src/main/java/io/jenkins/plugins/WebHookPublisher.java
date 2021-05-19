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
    public String serviceName;
    public String envName;

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    @DataBoundConstructor
    public WebHookPublisher(String webHookUrl, String serviceName, String envName) {
        super();
        this.webHookUrl = webHookUrl;
        this.serviceName = serviceName;
        this.envName = envName;
        log.error("$$$$$$$$ {}, {}, {}", webHookUrl, serviceName, envName);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
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
            return "OpsLevel Integration";
        }
    }
}
