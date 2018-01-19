# 2018-vision-code

Vision processing code that's made to run on the Raspberry Pi.

## Prerequisites
- A Raspberry Pi

## Using Gradle

Using the Gradle build tool makes building easy. Simply run `gradlew`
from the command line to get all the build tasks for this project. Or,
if you are in an IDE such as eclipse or IntelliJ, add the `build.gradle`
to the Gradle Projects view.

The main build task to remember is `gradlew deploy`. This deploys the code
to the Pi and runs the camera vision automatically.
