# MR-loader

At the moment tool is shared "as it is" and will be reworked as CLI tool. 

## How to use

1. Enter your GitHub token into gradle.properties
2. Run `./gradlew run`

## Examples

Gerrit and GitHub examples inside `Main.kt`

## Notes

Run task generates a properties file in build, then GithubLoader can use it
via resources as default token for downloading.