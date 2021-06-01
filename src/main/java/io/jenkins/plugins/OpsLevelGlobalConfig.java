package io.jenkins.plugins;

import java.util.HashMap;

public final class OpsLevelGlobalConfig {
    private static OpsLevelConfig globalConfig = null;

    public static OpsLevelConfig get() {
        if (globalConfig == null) {
            globalConfig = new OpsLevelConfig();
        }
        return globalConfig;
    }

    @Override
    public String toString() {
        return globalConfig.toString();
    }
}
