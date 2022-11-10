# MR-loader

## How to use

1. Enter your GitHub token into gradle.properties
2. Run `./gradlew run` or hit green triangle in IDEA in Main.kt

## Examples

Gerrit and GitHub examples inside `Main.kt`

## Notes

Run task generates a properties file in build, then GithubLoader can use it
via resources as default token for downloading.