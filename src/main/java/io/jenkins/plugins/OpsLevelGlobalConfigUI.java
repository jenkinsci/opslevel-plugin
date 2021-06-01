package io.jenkins.plugins;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;


@Extension
public class OpsLevelGlobalConfigUI implements Describable<OpsLevelGlobalConfigUI> {

    private static final Logger logger = Logger.getLogger(OpsLevelGlobalConfigUI.class.getName());

    public Descriptor<OpsLevelGlobalConfigUI> getDescriptor() {
        return getDescriptorImpl();
    }

    public DescriptorImpl getDescriptorImpl() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(OpsLevelGlobalConfigUI.class);
    }

    @Extension @Symbol("OpsLevelGlobalNotifier")
    public static class DescriptorImpl extends Descriptor<OpsLevelGlobalConfigUI> {

//        public static final OpsLevelConfig globalConfig = OpsLevelGlobalConfig.get();
        public static String webHookUrlGlobal = "";
        public static String environment = "";
        public static String description = "";
        public static String deployerId = "";
        public static String deployerEmail = "";
        public static String deployerName = "";

        public DescriptorImpl() {
            try {
                load();
            } catch(NullPointerException e) {
                e.printStackTrace();
                logger.warning("^^^ Failed to load() ^^^");
            }
        }

        public static String getWebHookUrlGlobal() {
            return webHookUrlGlobal;
        }

        public static String getEnvironment() {
            return environment;
        }

        public static String getDescription() {
            return description;
        }

        public static String getDeployerId() {
            return deployerId;
        }

        public static String getDeployerEmail() {
            return deployerEmail;
        }

        public static String getDeployerName() {
            return deployerName;
        }

        @DataBoundSetter
        public void setWebHookUrlGlobal(String webHookUrl) {
            this.webHookUrlGlobal = webHookUrl;
            // save();
        }

        @DataBoundSetter
        public void setEnvironment(String environment) {
            this.environment = environment;
            // save();
        }

        @DataBoundSetter
        public void setDescription(String description) {
            this.description = description;
            // save();
        }

        @DataBoundSetter
        public void setDeployerId(String deployerId) {
            this.deployerId = deployerId;
            // save();
        }

        @DataBoundSetter
        public void setDeployerEmail(String deployerEmail) {
            this.deployerEmail = deployerEmail;
            // save();
        }

        @DataBoundSetter
        public void setDeployerName(String deployerName) {
            this.deployerName = deployerName;
            // save();
        }

        public OpsLevelConfig generateOpsLevelConfig() {
            load();
            OpsLevelConfig config = new OpsLevelConfig();
            config.webHookUrl = webHookUrlGlobal;
            config.environment = environment;
            config.description = description;
            config.deployerId = deployerId;
            config.deployerEmail = deployerEmail;
            config.deployerName = deployerName;
            return config;
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
