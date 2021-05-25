# jenkins-plugin-with-ui

## Introduction

TODO Describe what your plugin does here

## Getting started

TODO Tell users how to configure your plugin here, include screenshots, pipeline examples and
configuration-as-code examples.

## Issues

TODO Decide where you're going to host your issues, the default is Jenkins JIRA, but you can also enable GitHub issues,
If you use GitHub issues there's no need for this section; else add the following line:

Report issues and enhancements in the [Jenkins issue tracker](https://issues.jenkins-ci.org/).

## Contributing

TODO review the default [CONTRIBUTING](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md) file and make sure it is appropriate for your plugin, if not then add your own one adapted from the base file

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## Running Locally

Clone this repo, install Java & Maven, and run:
```
    env -i PATH=$PATH mvn hpi:run
```
That optional `env -i PATH=$PATH` removes all env vars except `PATH`. Handy if (like me) you aren't sure which are created by Jenkins.

## Create plugin package
To create a compliled/packaged plugin that you can import directly into jenkins:
```
    mvn package
```

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
