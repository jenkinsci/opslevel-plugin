# OpsLevel plugin for Jenkins

Provides Jenkins integration with OpsLevel. This allows you to notify OpsLevel when a deploy succeeds.

## Getting started

1. Get an OpsLevel account <https://opslevel.com>
2. Make sure you have a deploy endpoint set up, or create a new one <https://opslevel.com/integrations>
3. Install this plugin on your Jenkins server:
    1.  From the Jenkins homepage navigate to `Manage Jenkins`
    2.  Navigate to `Manage Plugins`,
    3.  Change the tab to `Available`,
    4.  Search for `opslevel`,
    5.  Check the box next to install.


### Pipelines

Not supported yet.

### Freestyle job

1. Navigate to your job
2. Click 'Configure' in the left sidebar
3. Add our post-build action 'Publish successful build to OpsLevel'
4. Configure the 'Deploy WebHook URL'. You can find this URL in your OpsLevel Deploy Integration <https://opslevel.com/integrations>

![](/docs/opslevel_post_build_action.png)

## Developer Instructions

Refer to jenkins plugin guidelines: [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

Install Maven and JDK.

```shell
$ mvn -version | grep -v home
Apache Maven 3.8.1 (05c21c65bdfed0f71a2f2ada8b84da59348c4c5d)
Java version: 15.0.2, vendor: N/A, runtime: /usr/local/Cellar/openjdk/15.0.2/libexec/openjdk.jdk/Contents/Home
Default locale: en_CA, platform encoding: UTF-8
OS name: "mac os x", version: "10.15.7", arch: "x86_64", family: "mac"
```

Run unit tests

```shell
mvn test
```

## Create plugin package
Create an HPI file to install in Jenkins

```shell
mvn clean package
```

## Running Locally

Clone this repo, install Java & Maven, and run:
```
    env -i PATH=$PATH mvn hpi:run
```
That optional `env -i PATH=$PATH` removes all env vars except `PATH`.

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
