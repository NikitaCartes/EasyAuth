## Easy Authentication Mod

Since samo_lego deprecated his mod, I consider updating it to the latest version.

For Minecraft 1.16 and below you can check [original repository](https://github.com/samolego/SimpleAuth).

See [wiki](https://github.com/NikitaCartes/EasyAuth/wiki) for more information.

## License
Libraries that the project is using:
- `Argon2 (LGPLv3)` https://github.com/phxql/argon2-jvm
- `BCrypt (Apache 2)` https://github.com/patrickfav/bcrypt
- `Bytes (Apache 2)` https://github.com/patrickfav/bytes-java
- `leveldb (BSD-3-Clause)` https://github.com/google/leveldb
- `JNA (Apache 2 || LGPLv3)` https://github.com/java-native-access/jna

This project licensed under the `MIT` license.

# For mod developers

## Changing code

1. Clone the repository. Then run `./gradlew genSources`
2. Edit the code you want.
3. To build run the following command:

```
./gradlew clean build
```
