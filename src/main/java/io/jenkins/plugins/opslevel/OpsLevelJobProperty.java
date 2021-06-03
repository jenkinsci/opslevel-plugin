package io.jenkins.plugins.opslevel;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

public class OpsLevelJobProperty extends JobProperty {

    // Attaching this to a job indicates a notification has already been sent.
    // The global notifier will ignore jobs with this property

    public OpsLevelJobProperty() {
    }

    @Extension
    public static class OpsLevelJobPropertyDescriptorImpl extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }
}
