package io.jenkins.plugins;

import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;


@Extension
public class GlobalOpsLevelNotifier extends RunListener<Run<?, ?>> implements Describable<GlobalOpsLevelNotifier> {

    private static final Logger logger = Logger.getLogger(GlobalOpsLevelNotifier.class.getName());

    public Descriptor<GlobalOpsLevelNotifier> getDescriptor() {
        return getDescriptorImpl();
    }

    public DescriptorImpl getDescriptorImpl() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(GlobalOpsLevelNotifier.class);
    }

    @Extension @Symbol("GlobalOpsLevelNotifier")
    public static final class DescriptorImpl extends Descriptor<GlobalOpsLevelNotifier> {
        private String webHookUrl;
        private String environment;
        private String description;
        private String deployerId;
        private String deployerEmail;
        private String deployerName;

        public String getWebHookUrl() {
            return webHookUrl;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getDescription() {
            return description;
        }

        public String getDeployerId() {
            return deployerId;
        }

        public String getDeployerEmail() {
            return deployerEmail;
        }

        public String getDeployerName() {
            return deployerName;
        }

        @DataBoundSetter
        public void setWebHookUrl(String webHookUrl) {
            this.webHookUrl = webHookUrl;
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

        public DescriptorImpl() {
            try {
                load();
            } catch(NullPointerException e) {

            }
        }

        public String getDefaultEnvironment() {
            return "Prod Buddy";
        }

        public String getDisplayName() {
            return "Global OpsLevel Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            req.bindJSON(this, formData);
            save();
            return true;
        }

    }
}
