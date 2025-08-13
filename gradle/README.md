# Gradle Build Scripts Organization

This directory contains modular Gradle scripts that are applied to the main build.gradle to keep the build configuration clean and organized.

## Scripts

* **version-management.gradle**: All version-related tasks and functions
  * `generateVersionConstants`: Creates DioclesConstants.java
  * `currentVersion`: Shows version and validates generated constants
  * `bumpPatch/bumpMinor/bumpMajor`: Version increment tasks
  * `bumpVersion()`: Internal version bumping logic

## Future Scripts

Future scripts could include:

* **dependencies.gradle**: Centralized dependency management
* **publishing.gradle**: Maven/publishing configuration
* **testing.gradle**: Test configuration and custom test tasks
* **quality.gradle**: Code quality, linting, formatting tasks
