# MR-loader

Tool for loading merge requests from Gerrit and Github.

## How to use

### CLI

At the moment, CLI can be used for loading from Gerrit. Github functionality will be added later.

1. Enter your GitHub token into gradle.properties
2. Run `./gradlew :cli:shadowJar`
3. Now you can use shell script to use cli `./run.sh`

The script should be executed as:
```shell script
./run.sh commandName options arguments
```

To get more info about options:
```shell script
./run.sh commandName -h
```

### Kotlin app

Gerrit and GitHub examples inside `Main.kt`

1. Enter your GitHub token into gradle.properties
2. Make adjustments in `Main.kt`
3. Run `./gradlew run`

## Notes

Run task generates a properties file in build, then GithubLoader can use it
via resources as default token for downloading.