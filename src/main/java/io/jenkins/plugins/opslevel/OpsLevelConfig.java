package io.jenkins.plugins.opslevel;

public class OpsLevelConfig {
    public boolean run;
    public String webHookUrl;
    public String serviceAlias;
    public String environment;
    public String description;
    public String deployUrl;
    public String deployerId;
    public String deployerEmail;
    public String deployerName;
    public String ignoreList;

    public OpsLevelConfig() {
        run = true;
        webHookUrl = "";
        serviceAlias = "";
        environment = "";
        description = "";
        deployUrl = "";
        deployerId = "";
        deployerEmail = "";
        deployerName = "";
        ignoreList = "";
    }

    @Override
    public String toString() {
        return "OpsLevelConfig{" +
                "run=" + run +
                ", webHookUrl='" + webHookUrl + '\'' +
                ", serviceAlias='" + serviceAlias + '\'' +
                ", environment='" + environment + '\'' +
                ", description='" + description + '\'' +
                ", deployUrl='" + deployUrl + '\'' +
                ", deployerId='" + deployerId + '\'' +
                ", deployerEmail='" + deployerEmail + '\'' +
                ", deployerName='" + deployerName + '\'' +
                ", ignoreList='" + ignoreList + '\'' +
                '}';
    }

    public void populateEmptyValuesFrom(OpsLevelConfig otherConfig) {
        // Bring in values from another config, preferring to keep our own
        // this.run is purposely excluded from this merge

        if (this.webHookUrl.isEmpty()) {
            this.webHookUrl = otherConfig.webHookUrl;
        }
        if (this.serviceAlias.isEmpty()) {
            this.serviceAlias = otherConfig.serviceAlias;
        }
        if (this.environment.isEmpty()) {
            this.environment = otherConfig.environment;
        }
        if (this.description.isEmpty()) {
            this.description = otherConfig.description;
        }
        if (this.deployUrl.isEmpty()) {
            this.deployUrl = otherConfig.deployUrl;
        }
        if (this.deployerId.isEmpty()) {
            this.deployerId = otherConfig.deployerId;
        }
        if (this.deployerEmail.isEmpty()) {
            this.deployerEmail = otherConfig.deployerEmail;
        }
        if (this.deployerName.isEmpty()) {
            this.deployerName = otherConfig.deployerName;
        }
        if (this.ignoreList.isEmpty()) {
            this.ignoreList = otherConfig.ignoreList;
        }
    }
}
