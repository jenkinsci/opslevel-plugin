package io.jenkins.plugins;

public class OpsLevelConfig {
    public String webHookUrl;
    public String serviceAlias;
    public String environment;
    public String description;
    public String deployUrl;
    public String deployerId;
    public String deployerEmail;
    public String deployerName;

    public OpsLevelConfig() {
        webHookUrl = "";
        serviceAlias = "";
        environment = "";
        description = "";
        deployUrl = "";
        deployerId = "";
        deployerEmail = "";
        deployerName = "";
    }

    @Override
    public String toString() {
        return "OpsLevelConfig{" +
                "webHookUrl='" + webHookUrl + '\'' +
                ", serviceAlias='" + serviceAlias + '\'' +
                ", environment='" + environment + '\'' +
                ", description='" + description + '\'' +
                ", deployUrl='" + deployUrl + '\'' +
                ", deployerId='" + deployerId + '\'' +
                ", deployerEmail='" + deployerEmail + '\'' +
                ", deployerName='" + deployerName + '\'' +
                '}';
    }
}
