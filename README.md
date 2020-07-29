# Simple Authentication Mod

[![License](https://img.shields.io/github/license/samolego/simpleauth.svg)](https://github.com/samolego/SimpleAuth/blob/master/LICENSE)
[![Gradle Build](https://github.com/samolego/SimpleAuth/workflows/Gradle%20Build/badge.svg)](https://samolego.github.io/projects/ci/SimpleAuth/latest)
[![Version](https://img.shields.io/github/v/tag/samolego/SimpleAuth.svg?label=version)](https://github.com/samolego/SimpleAuth/releases/latest)
[![Closed Issues](https://img.shields.io/github/issues-closed/samolego/simpleauth.svg)](https://github.com/samolego/SimpleAuth/issues?q=is%3Aissue+is%3Aclosed)
[![Curseforge downloads](http://cf.way2muchnoise.eu/full_simpleauth_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/simpleauth)

[<img src="https://i.imgur.com/Ol1Tcf8.png" alt="Requires Fabric API." width="200px" href="https://www.curseforge.com/minecraft/mc-mods/fabric-api">](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

## License
Libraries that the project is using:
- `Argon2 (LGPLv3)` https://github.com/phxql/argon2-jvm
- `leveldb (BSD-3-Clause)` https://github.com/google/leveldb
- `JNA (Apache 2 || LGPLv3)` https://github.com/java-native-access/jna

This project is licensed under the `MIT` license.

# For mod developers

## Changing code

1. Clone the repository. Then run `./gradlew genSources`
2. Edit the code you want.
3. To build run the following command:

```
./gradlew build
```
## Adding the mod to your buildscript (to ensure compatibility)

Add following text to your `build.gradle`

```gradle
repositories {
	maven {
		url 'https://jitpack.io'
	}
}

dependencies {
  // By version tag
  modImplementation 'com.github.samolego:SimpleAuth:${project.simpleauth_version}'
  
  // Or by branch
  modImplementation 'com.github.samolego:SimpleAuth:${project.simpleauth_branch}-SNAPSHOT'
}
```

And this to your `gradle.properties`
```properties
# By tag (version)
# SimpleAuth version (this might not be the latest version)
simpleauth_version = 1.4.8

# Or this (by branch)
# SimpleAuth branches

# master branch (the latest version for stable minecraft release)
simpleauth_branch = master

# snapshot branch (branch for minecraft snapshots)
simpleauth_branch = latest-snapshot
```
