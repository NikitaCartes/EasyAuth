## EasyAuth
### Authentication mod for your Offline/Online server.

EasyAuth disallows players who aren't authenticated to do actions like placing blocks, moving, typing commands or use the inventory.

### Feature:
- Auto login players that have purchased Minecraft
- Online UUIDs for online players
- Sessions for auto login if they connect from the same IP
- Coordinate protection
- Prevents "Logged in from another location"
- Server-side translation
- Support for special characters in password
- Global password
- Support Luckperms API and Luckperms Context
- Support Floodgate and Carpet players
- Support Vanish mod to hide unauthenticated players

See [wiki](https://github.com/NikitaCartes/EasyAuth/wiki) for more information.

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/easyauth), [Modrinth](https://modrinth.com/mod/easyauth)

[Discord](https://discord.gg/UY4nhvUzaK)

[My Whitelist mod](https://github.com/NikitaCartes/EasyWhitelist) that changes whitelist behaviour from uuid-based to name-based, allowing it to be used on offline servers.

### Dependencies
This mod requires:
- `Fabric API` [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api), [Modrinth](https://modrinth.com/mod/fabric-api)

### Build
Build all supported versions at once:
```bash
./gradlew build
```

If you want to add new feature, you can make pull request against `fabric-1.21.6` branch as it contains the latest changes from `stonecutter` branch:
```bash
git checkout fabric-1.21.6
./gradlew build
```
### Languages
This mod supports multiple languages.

| Language                                                                | Missing Strings |                        Contributors                         |
|-------------------------------------------------------------------------|:---------------:|:-----------------------------------------------------------:|
| Czech <br/>(cs_cz)                                                      |       11        |                  @DavidCZ2051, @Thewest123                  |
| German <br/>(de_de)                                                     |       14        |                           @X00LA                            |
| English <br/>(en_gb, en_us)                                             |        0        |                  @samolego, @NikitaCartes                   |
| Spanish <br/>(es_ar, es_cl, es_ec, <br/>es_es, es_mx, es_uy,<br/>es_ve) |       14        |                @Zailer43, @DanielTrejoBorjas                |
| French <br/>(fr_fr)                                                     |        9        |                    @Uxzylon, @Sky-NiniKo                    |
| Hungarian <br/>(hu_hu)                                                  |       14        |                       @Bendimester23                        |
| Italian <br/>(it_it)                                                    |       11        |                         @Rizzo1812                          |
| Polish <br/>(pl_pl)                                                     |       11        |                       @LimakXRobczuk                        |
| Brazilian Portuguese <br/>(pt_br)                                       |        2        |                   @luizffgv, @guigiffoni                    |
| Russian <br/>(ru_ru)                                                    |        0        |               @alphatoasterous, @NikitaCartes               |
| Slovenian <br/>(sl_si)                                                  |       14        |                          @Kljunas2                          |
| Turkish <br/>(tr_tr)                                                    |        0        |                    @egeesin, @MemoKing34                    |
| Ukrainian <br/>(uk_ua)                                                  |        9        |                     @txlbr, @Y0shioSato                     |
| Vietnamese <br/>(vi_vn)                                                 |        0        |                     @Toibithieunang123                      |
| Chinese <br/>(zh_cn, zh_tw)                                             |        9        | @Neubulae, @GodGun968, @Sam5440,<br/>@CMJNB, @MyBlueHorizon |

A Total of 43 strings are used in the mod

### Contributors

* Many thanks for @samolego, author of SimpleAuth, for his mod, and his permission for creating this fork
  - For Minecraft 1.16 and below you can check [original repository](https://github.com/samolego/SimpleAuth).
* Thanks to @were491 for improvement in a login system
* Thanks to @Nikijaz for implementing MySQL database support
* Thanks to @dmunozv04 for Floodgate support
* Thanks to @Wereii for 1.19.3 support
* Thanks to @martinszelcel for updating for new Mojang's API
* Thanks to @Gamecraft007 for 1.21 support
