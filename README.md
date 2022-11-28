# MR-loader

Tool for loading merge requests from Gerrit. Github functionality will be added later.

## How to use

### CLI

At the moment, CLI can be used for loading from Gerrit.

1. Run `./gradlew :cli:shadowJar`
2. Now you can use shell script to use cli `./run.sh`

The script should be executed as:
```shell script
sh ./run.sh GerritLoad options arguments
```

To get more info about options:
```shell script
sh ./run.sh GerritLoad -h
```
