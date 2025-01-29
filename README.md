# RARS Nova - RARS, modernized

This repository is a fork of the [RARS](https://github.com/TheThirdOne/rars)
project.

Main goals of this fork are:

- modernization of the codebase
- introduction of external dependencies to simplify the codebase
- improving the user experience in various areas (i.e. the code editor).

This project currently has achieved the following:

- migration to Java 21, removal of deprecated APIs from the standard library
- migration to Gradle as the build tool
- addition of JUnit 5 for testing
- replacement of the old Bitmap Display tool with a dedicated syscall
- introduction of a new code editor ([RSyntaxTextArea]) that improves
  syntax highlighting, adds code folding and commenting capabilities
- redesign of the editor settings UI
- addition of the [JetBrains Annotations] library to improve nullity analysis
- overall API changes.

Despite all the changes, the project is still in a stage, where the internal
API is heavily subject to change. However, it is very much usable, so feel free
to try it out.

## Building the project

You only require a JDK 21 or newer to build the project, as there is a
Gradle wrapper included in the repository.

To build the project on Unix-like systems, run the following command:

```shell
./gradlew shadowJar
```

On Windows, run the following command:

```shell
.\gradlew.bat shadowJar
```

The resulting JAR file will be located in the `build/libs` directory.

## Running the project

To run the project, you only require a JRE 21 or newer. Simply invoke:

```shell
java -jar "<path-to-jar>"
```

from the command line. On Windows, you can also double-click the JAR file.

<!-- links: -->

[RSyntaxTextArea]: https://github.com/bobbylight/RSyntaxTextArea

[JetBrains Annotations]: https://github.com/JetBrains/java-annotations
