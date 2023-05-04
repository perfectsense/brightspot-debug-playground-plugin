# Brightspot Debug Playground Plugin

## Introduction

Here is an IntelliJ plugin that allows for _debug/code testing to be done from your browser.

Here is a link to the confluence page with more information and demos of the plugin: https://perfectsense.atlassian.net/wiki/spaces/DEL/blog/2023/01/26/3244785691/Brightspot+Debug+Playground+Plugin

Note: this plugin can be used on any file, not just scratch files, but it is only configured to evaluate java files in the same way the code debugger evaluates them.

## Setup

This section may be out of date if you are installing through the jetbrains marketplace.
To setup the plugin first download the correct jar file matching your IntelliJ version. Then go to preferences (⌘ ,) and navigate to Plugins. Click the gear icon to Install Plugin from Disk.
If you get an error like the following:
```agsl
Plugin 'Brightspot Utils' is not compatible with the
current version of the ide
```
Then either you need to update IntelliJ or the plugin is out of date and needs to be updated for the new version.
## Configuring Environments
The first step to use the plugin is configuring environments. To do so go to preferences (⌘ ,) → Tools → Debug Code Environments and add a new environment. You can either add a local environment or add a QA environment. Make sure you get the debug credentials from the project’s slack channel.
## Debugging Code
Now that you have configured one or more environment, you can run code from IntelliJ. Simply navigate to a project file or scratch file and right click on the editor tab and find Run in Debug Code That should open a submenu with your environments and you can choose which environment to run it in. Once your code finishes executing, you will get a dialog popup with your response.
## Disclaimer
This tool is very much use at your own risk and has a number of different issues and bugs. Please comment any issues you run into while using and I will try to fix them when I get time. Additionally this tool poses some potential issues if you are trying to connect to a cloud environment and it is intended for use on the local environment.
## Upgrading Versions
If the plugin is out of date, upgrading it is easy and a few things might need to be updated. First you need to update the IntelliJ version in the build.gradle

```
// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  version = '2022.3'
}
```
Next you may have to upgrade the IntelliJ plugin dependancy
```
plugins {
  id 'org.jetbrains.intellij' version '1.12.0'
}
```
You might need to upgrade the java version depending which version of java future IntelliJ runs on
```
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
```
And finally you may need to update the gradle version you run on in gradle → gradle-wrapper.properties

distributionUrl=https://services.gradle.org/distributions/gradle-7.3-bin.zip
Then run ./gradlew build and get your jar from build/libs
## Known Bugs
Do not run with the code debugger also running, this will cause a gridlock in the ide requiring you to restart intelliJ
