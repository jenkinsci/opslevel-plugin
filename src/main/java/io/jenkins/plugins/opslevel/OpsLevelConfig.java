package io.jenkins.plugins.opslevel;

public class OpsLevelConfig {
    public boolean run;
    public String webhookUrl;
    public String serviceAlias;
    public String serviceAliasTemplate;
    public String environment;
    public String description;
    public String deployUrl;
    public String deployerId;
    public String deployerEmail;
    public String deployerName;
    public String ignoreList;

    public OpsLevelConfig() {
        run = true;
        webhookUrl = "";
        serviceAlias = "";
        serviceAliasTemplate = "";
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
               ", webhookUrl='" + webhookUrl + '\'' +
               ", serviceAlias='" + serviceAlias + '\'' +
               ", serviceAliasTemplate='" + serviceAliasTemplate + '\'' +
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

        if (this.webhookUrl.isEmpty()) {
            this.webhookUrl = otherConfig.webhookUrl;
        }
        if (this.serviceAlias.isEmpty()) {
            this.serviceAlias = otherConfig.serviceAlias;
        }
        if (this.serviceAliasTemplate.isEmpty()) {
            this.serviceAliasTemplate = otherConfig.serviceAliasTemplate;
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
