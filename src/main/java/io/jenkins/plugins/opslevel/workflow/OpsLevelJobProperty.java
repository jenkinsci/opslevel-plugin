package io.jenkins.plugins.opslevel.workflow;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import io.jenkins.plugins.opslevel.JobListener;
import io.jenkins.plugins.opslevel.OpsLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpsLevelJobProperty extends JobProperty {
    // Attaching this to a job indicates a notification has already been sent.
    // The global notifier will NOT notify when it sees this property

    private static final Logger logger = LoggerFactory.getLogger(JobListener.class);

    public final OpsLevelConfig config;

    public OpsLevelJobProperty(OpsLevelConfig config) {
        this.config = config;
        logger.error("SETTING OPSLEVEL CONFIG: {}", this.config);
    }

    @Extension
    public static class OpsLevelJobPropertyDescriptorImpl extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }
}
