### ToDo
#### Add
- Add support for a changing "invalid session" message when offline player connects with online nickname
- Gamemode switching on login
- Op status switching on login
- Optional session by player

#### Fix
- Check how offline players work if they try to connect with a taken online account

---
### 3.4.3
##### Minecraft 1.19.4, 1.20[.X], 1.21[.X]
#### Add
- IP limit controls [#200](https://github.com/NikitaCartes/EasyAuth/issues/200), thanks to @virgil698 [#252](https://github.com/NikitaCartes/EasyAuth/pull/252)
- New config options for IP limit in `extended.conf`:
  - `ipLimit.enabled`
  - `ipLimit.maxAccountsPerIp`
  - `ipLimit.blockExcessRegistration`
  - `ipLimit.notifyAdmins`
  - `ipLimit.exemptIps`
  - `ipLimit.cacheExpirySeconds`
  - `ipLimit.maxConcurrentSessionsPerIp`
  - `ipLimit.exemptOnlinePlayers`
- UUID management commands, thanks to @virgil698 [#249](https://github.com/NikitaCartes/EasyAuth/issues/249)
  - `/auth setUuid`
  - `/auth clearUuid`
  - `/auth getUuid`
- PostgreSQL support, thanks to @DanielTrejoBorjas [#226](https://github.com/NikitaCartes/EasyAuth/pull/226), [#258](https://github.com/NikitaCartes/EasyAuth/pull/258)
- New options in `extended.conf` for preventing OPs and registered players from joining without logging with `skipAllAuthChecks` enabled, thanks to @Fenix5fire [#184](https://github.com/NikitaCartes/EasyAuth/pull/184)
  - `skipAllAuthChecksNotForOperators`
  - `skipAllAuthChecksNotForRegisteredPlayers`
- Option `allowCustomPacketsForNonOp` to allow handling custom packets for non-OP players
- Option `allowedCustomPackets` to specify list of custom packets that always allowed

#### Fix
- `/auth update` to also refresh cached online player data [#244](https://github.com/NikitaCartes/EasyAuth/issues/244)
- Problem with loading configurations files [#225](https://github.com/NikitaCartes/EasyAuth/issues/225)
- Fail to freeze player before login [#261](https://github.com/NikitaCartes/EasyAuth/issues/261)

---
### 3.4.2
##### Minecraft 1.19.4, 1.20[.X], 1.21[.X]
#### Fix
- It wasn't possible to join the server using an online account if `forced-offline-uuid` was enabled
- `hide-player-coords` not working for players with `ONLINE` status on `online-mode` servers [#251](https://github.com/NikitaCartes/EasyAuth/issues/251)
- Packets are being handled for not authenticated players [#230](https://github.com/NikitaCartes/EasyAuth/issues/230)

#### Add
- Option `allowCustomPackets` to allow handling custom packets (used by other mods) for not authenticated players
- Option `allowAllPackets` to allow handling all packets for not authenticated players (including vanilla packets)

---
### 3.4.1
##### Minecraft 1.19.4, 1.20[.X], 1.21[.X]
#### Fix
- Incompatibility with QuickBackupMulti mod [#232](https://github.com/NikitaCartes/EasyAuth/issues/232)
- MongoDB connection [#247](https://github.com/NikitaCartes/EasyAuth/issues/247), thanks to @MemoKing34
- Server crash during config reload [#248](https://github.com/NikitaCartes/EasyAuth/issues/248)
- Incorrect messages when deleting/updating non-existing account
- Sometimes players are not deleted properly from the database
- Config not migrating after adding new options [#239](https://github.com/NikitaCartes/EasyAuth/issues/239)

#### Changes
- Update Chinese translation [#233](https://github.com/NikitaCartes/EasyAuth/issues/233), thanks to @ShadyLeaf
- Update Spanish translation [#250](https://github.com/NikitaCartes/EasyAuth/pull/250), thanks to @danielospina-b

---
### 3.4.0
##### Minecraft 1.21.9-1.21.10
#### Fix
- Not sending register prompt on login if coordinates hiding is disabled [#237](https://github.com/NikitaCartes/EasyAuth/issues/237)
- False-positive UUID mismatch
- Not preventing "Kick from another location" disconnection

---
### 3.3.6
##### Minecraft 1.19.4, 1.20[.X], 1.21[.X]
#### Add
- Add `prevent-offline-players-with-online-usernames` option to prevent offline players from joining with online usernames
- Add `check-offline-players-with-online-usernames` option to check offline players with online usernames every time they join the server for an online account

#### Fix
- Command reloading
- Detection of offline players that join using an online account

#### Changes
- Update Turkish translation, thanks to @MemoKing34

---
### 3.3.5
##### Minecraft 1.19.4, 1.20[.X], 1.21[.X]
#### Changes
- Build EasyAuth against Java 17 [#199](https://github.com/NikitaCartes/EasyAuth/issues/199)
- Improve readability of es_mx language

----
### 3.3.4
##### Minecraft 1.20[.X], 1.21[.X]
#### Add
- Disable register command if `enable-global-password` enabled with `single-use-global-password` disabled

#### Fix
- Re-register `register` command if `single-use-global-password` was changed
- Wrong log-in message with enabled `single-use-global-password` and disabled `enable-global-password`
- Double login required message on login

----
### 3.3.3
##### Minecraft 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4, 1.21.5, 1.21.6
#### Add
- Vietnamese translation, thanks to @Toibithieunang123

#### Fix
- Player joins vanished with enabled `vanish-until-auth` and valid session

----
### 3.3.2
##### Minecraft 1.21.2-1.21.4, 1.21.5, 1.21.6
#### Add
- `auth setSpawn` command only with coordinates, without yaw and pitch

#### Fix
- Not being invulnerable and invisible upon logging in
- Auth markAsOnline doesn't work if a user is on server [#216](https://github.com/NikitaCartes/EasyAuth/issues/216)
- Check for account existence before marking player as online
- Fix a problem with loading new language config

#### Changes
- Update english and russian translations

----
### 3.3.0
##### Minecraft 1.21.5
#### Add
- Option `vanish-until-auth` to vanish players until they authenticate using [Vanish mod](https://github.com/DrexHD/Vanish)
- Option `default-language` to set default language with disabled server-side translation

#### Remove
- Remove `hide-players-from-player-list` option

#### Fix
- Fix unexpected error with spawn position outside of world height limit
- Fix EasyAuth not sending minPasswordLength and maxPasswordLength to players [#205](https://github.com/NikitaCartes/EasyAuth/issues/205)
- Fix detecting fake players [#207](https://github.com/NikitaCartes/EasyAuth/issues/207)
- Relocate libs to be compatible with other mods

#### Changes
- Changed how Invulnerability and Invisibility are handled during login
- EasyAuth doesn't include `fabric-permissions-api-v0` anymore
- Simplify config system's backend

----
### 3.2.1
##### Minecraft 1.20[.1], 1.20.2, 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4, 1.21.5
#### Fixes
- Disconnect text sent as text key

----
### 3.2.0
##### Minecraft 1.20[.1], 1.21.5
#### Add
- Integration with LuckPerms context:
    - `easyauth:authenticated`: true if player is authenticated
    - `easyauth:online_account`: true if player is using Mojang account
- Add option to log login and registration as info messages:
    - `log-player-registration` and `log-player-login` in `extended.conf`
- Add command `auth getOnlinePlayers` to get info about online players

#### Fixes
- Build EasyAuth against Java 17 [#199](https://github.com/NikitaCartes/EasyAuth/issues/199)
- Fix incompatibilities with mods by getting rid of @Redirect
- Fix duplicate config files [#196](https://github.com/NikitaCartes/EasyAuth/issues/196)

#### Changes
- Update brazilian Portuguese translation, thanks to @guigiffoni

----
### 3.1.11
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2, 1.19, 1.19.1-1.19.2, 1.19.3, 1.19.4, 1.20[.1], 1.20.2, 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4, 1.21.5
#### Fixes
- Fix server-side translation, fix [#163](https://github.com/NikitaCartes/EasyAuth/issues/163)
- Fix vehicle disappear if player log out in different dimension with hide-player-coords, fix [#141](https://github.com/NikitaCartes/EasyAuth/issues/141)

----
### 3.1.10
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2, 1.19, 1.19.1-1.19.2, 1.19.3, 1.19.4, 1.20[.1], 1.20.2, 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4, 1.21.5
#### Fixes
- Fix potential crash while migrating passwords

----
### 3.1.9
##### Minecraft 1.21.2-1.21.4
#### Fixes
- Fix bug with using wrong hash algorithm for passwords

----
### 3.1.8
##### Minecraft 1.19, 1.19.1-1.19.2, 1.19.3, 1.19.4, 1.20[.1], 1.20.2, 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4
#### Fixes
- Minor code clean up

----
### 3.1.7
##### Minecraft 1.20.3-1.20.4, 1.20.5-1.20.6, 1.21-1.21.1, 1.21.2-1.21.4
#### Fixes
- Fix configuration not loading
- Fix password migration

----
### 3.1.6
##### Minecraft 1.21-1.21.1, 1.21.2-1.21.4
#### Added
- Automatically migrate passwords to BCrypt if they are in Argon2
- Backup config files before rewriting them

#### Changes
- Switch to BCrypt as Hash algorithm for passwords

#### Deleted
- Options `confirmed-online-players`, `forced-offline-players` and `check-unmigrated-argon2`

----
### 3.1.5
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Fixes
- Fix bug with missing translation key
- `'` isn't wrongly escaped in config anymore
- Fix global password not working correctly

#### Changes
- Update Turkish translation, thanks to @MemoKing34

----
### 3.1.4
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Fixes
- Fix [#188](https://github.com/NikitaCartes/EasyAuth/issues/188), bug with Carpet fake players

----
### 3.1.3
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Fixes
- Fix MySQL migration bug

----
### 3.1.2
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Fixes
- Fix bug with missing translation key
- Fix [#186](https://github.com/NikitaCartes/EasyAuth/issues/186), bug with linked Floodgate players

----
### 3.1.1
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Fixes
- Fix [#178](https://github.com/NikitaCartes/EasyAuth/issues/178), when min-password-chars and max-password-chars weren't sent to players
- Fix a few bugs with MySQL connection

----
### 3.1.0
##### Minecraft 1.21 - 1.21.1, 1.21.2 - 1.21.4
#### Added
- Option `single-use-global-password` when enabled, player can register with global password but not log in with it
- Online/offline player separation:
   - Option `offline-by-default` (default `false`) to mark all players as offline by default
   - Command `account online` that mark a player as online
   - Commands `auth markAsOffline <username>` and `auth markAsOnline <username>`
- Option `hide-inventory` (default `true`) in `extended.conf` to hide inventory of unauthenticated players
- Command `auth getPlayerInfo`
- Option `allow-case-insensitive-username` in `extended.conf` to allow players with same nickname but different case to join (default `false`)
- Option `authentication-prompt-interval` in `extended.conf` to set interval between authentication prompts (default 10 seconds)
- Option `mojang-api-settings` in `extended.conf` for custom Mojang API settings

#### Fixes
- Fix bug with respawn while leaving server being dead

#### Changes
- Allow players to log in even if a player with the same nickname is already online if they join from the same IP
- Database overhaul:
   - Now database key is username instead of uuid
   - SQLite is now default database instead of LevelDB
   - Drop support for LevelDB (data from LevelDB will be migrated to SQLite automatically)
   - Config version is now 2
   - All players from `usercache.json` will be migrated automatically
- Change default hash algorithm to Argon2 from BCrypt:
   - If you previously used BCrypt, typed password will be checked both against BCrypt and Argon2 (option `check-unmigrated-argon2` in `extended.conf`)
- Increased default `teleportation-timeout-ms` from 5 to 20 ms
- Turkish translation update, thanks to @MemoKing34

#### Deleted
- Command `auth addToForcedOffline <username>`
- Options `confirmed-online-players` and `forced-offline-players` are now stored in the database for each player separately instead

----
### 3.0.28
##### Minecraft 1.21.2 - 1.21.4

1) Fix [#164](https://github.com/NikitaCartes/EasyAuth/issues/164), incompatibility with C2ME and hide player coords

----
### 3.0.27
##### Minecraft 1.21.2 - 1.21.4

1) Fix session issue for real this time

----
### 3.0.26
##### Minecraft 1.21.2, 1.21.3

1) Update Simplified Chinese translation, thanks to @CMJNB and @MyBlueHorizon
2) Update French translation, thanks to @Sky-NiniKo
3) Update Ukrainian translation, thanks to @Y0shioSato
4) Fix uuid wasn't clickable in chat in some languages
5) Fix connection bug

----
### 3.0.25
##### Minecraft 1.21-1.21.3

1) Fix toLowerCase() using on PC with Turkish locale

----
### 3.0.25
##### Minecraft 1.20.5-1.20.6, 1.21

1) Fix premium player being invulnerable after using `/logout` command

----
### 3.0.24
##### Minecraft 1.20.5-1.20.6

1) Fix spawning in wrong dimension with `hide-player-coords` enabled

----
### 3.0.23
##### Minecraft 1.20.5-1.20.6

1) Fix `auth list`.

----
### 3.0.22-SNAPSHOT
##### Minecraft 1.20.3-1.20.4

1) Fix mod not working not in dev environment

----
### 3.0.21-SNAPSHOT
##### Minecraft 1.20.3-1.20.4

1) Change config system entirely.
2) Fix `hidePlayersFromPlayerList`
3) Add `/reg` alias for `/register`
4) Add Italian translation, thanks to @Rizzo1812
5) Add Taiwan Chinese translation, thanks to @Sam5440
6) Add message when player is log in with valid session and online account:
   - `text.easyauth.validSession`
   - `text.easyauth.onlinePlayerLogin`
7) If player leave server while at respawn screen, they will be killed after log in to prevent coordinates leaking.
8) Fix infinite "Loading Terrain" problem with terrain not loading 

----
### 3.0.20
##### Minecraft 1.20.3-1.20.4

Update dependencies

----
### 3.0.19
##### Minecraft 1.20[.1], 1.20.2

1) Add option to skip all authentication if it's already done by another thing (mod/proxy/etc.):
   - `skipAllAuthChecks`
2) Player will be re-mounted on entity if they were dismounted on login with `spawnOnJoin` enabled
3) Changed behavior of `premiumAutologin`. Now it only allows online players not to authorize when logging in
4) Fix incompatibility with LuckPerms with enabled forcedOfflineUuids

----
### 3.0.18
##### Minecraft 1.20

1) Add options to allow certain commands to be used before login:
   - `allowCommands` - allow all commands
   - `allowedCommands` - allow only listed commands

----
### 3.0.0-17
##### Minecraft 1.19.4, 1.20

1) Increase priority `forcedOfflinePlayers` over `verifiedOnlinePlayer`
2) Improve debug log

----
### 3.0.0-16
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2, 1.19, 1.19.1-1.19.2, 1.19.3, 1.19.4, 1.20

1) Add Polish translation, thanks to @LimakXRobczuk
2) Fix [#51](https://github.com/NikitaCartes/EasyAuth/issues/51) that sometimes online player treated as offline 
3) Fix blank uuid in console log
4) Improve logging

----
### 3.0.0-15
##### Minecraft 1.19.1-1.19.2, 1.19.3

1) Fix few NPEs

----
### 3.0.0-14
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2, 1.19, 1.19.1-1.19.2, 1.19.3

1) Improve kicking if number of login attempts exceeded
2) Save EasyAuth's database on `save-all` command
3) Reconnect to DB with `auth reload`
4) Fix `hideUnauthenticatedPLayersFromPlayerList`, thanks to @Wereii
5) Update Simplified Chinese Localization, thanks to @GodGun968
6) Add `floodgateBypassUsernameRegex` option that allow players that join via Floodgate even if their username isn't validated by the Regex matcher, thanks to @Biel675
7) Add Ukrainian translation, thanks to @txlbr
8) Fix fall through half-blocks on login
9) Add AutoSave for DB
10) Fix [#83](https://github.com/NikitaCartes/EasyAuth/issues/83) for new Mojang's API, thanks to @martinszelcel

----
### 3.0.0-8
##### Minecraft 1.19.1

1) Quick fix for MongoDB connection string
2) Update Czech translation

----
### 3.0.0-7
##### Minecraft 1.19

1) Probably fix missing server side translation [#36](https://github.com/NikitaCartes/EasyAuth/issues/36) with updating server translation
2) Fix using command before login [#52](https://github.com/NikitaCartes/EasyAuth/issues/52), [#56](https://github.com/NikitaCartes/EasyAuth/issues/56), thanks to @were491

----
### 3.0.0-6
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2, 1.19

1) Fix MySQL support
2) Add placeholder api support, thanks to @Nikijaz
3) Fix crash if Fake Players join
4) Fix softlock while log in inside a portal
5) Add support for Floodgate players, thanks to @dmunozv04

----
### 2.2.2
##### Minecraft 1.17, 1.17.1, 1.18[.1], 1.18.2

1) Fix many real and potential issues with login attempts, thanks to @were491
2) New option `resetLoginAttemptsTime`
   - How long it takes (seconds) after a player gets kicked for too many logins for the player to be allowed back in
3) MySQL support, thanks to @Nikijaz

----
### 2.1.0
##### Minecraft 1.18.x

1) Fix Turkish translation
2) Mod was trying to create two different folder for DB (`levelDBStore` and `leveldbStore`) [#29](https://github.com/NikitaCartes/EasyAuth/issues/29). Sometimes this resulted in an error [#6](https://github.com/NikitaCartes/EasyAuth/issues/6).
3) Remove lag spike on first connection of player [#31](https://github.com/NikitaCartes/EasyAuth/issues/31)
4) Temporarily disabled `hideUnauthenticatedPLayersFromPlayerList` feature

----
### 2.0.6
##### Minecraft 1.18.x

1) Add `auth addToForcedOffline <player>` command to add player in `forcedOfflinePlayers` list
2) Change default op-level for `auth *` from 4 to level 3 (except for `setGlobalPassword`)
3) Fix [#23](https://github.com/NikitaCartes/EasyAuth/issues/23) when players sometimes stays invulnerable after login
4) Turkish translation, thanks to @egeesin
5) New option `enableServerSideTranslation` to disable server-side translation

----
### 2.0.5
##### Minecraft 1.17, 1.17.1, 1.18

1) `auth uuid <player>` that would give correct offline uuid fot that player nickname in lower case
2) Add [permission](https://github.com/NikitaCartes/EasyAuth/wiki/Permissions) support
3) Add `auth list` command to print all registered players
4) Fix `auth update` command
5) Temporally disable `hideUnauthenticatedPLayersFromPlayerList` by default
6) Czech translation, thanks to @DavidCZ2051

----
### 2.0.4
##### Minecraft 1.17.1, 1.18-pre1

1) With enabled [global password](https://github.com/NikitaCartes/EasyAuth/wiki/Global-password) player can log in with global password or password set by `auth register`

----
### 2.0.3
##### Minecraft 1.17.1, 21w37a+

1) Fix problem with registration ([#14](https://github.com/NikitaCartes/EasyAuth/issues/14))
   - argon2 library split to two libs, and I didn't include one of it
   - Update libraries
2) Improve hiding in TabList
   - Now premium players shown in it
   - As well as carpet's fake-player

----
### 2.0.2
##### Minecraft 1.17.1

1) Add setting which hide unauthenticated players from tab list
   - `hideUnauthenticatedPLayersFromPlayerList` in `config.json`
   - `true` by default

----
### 2.0.1
##### Minecraft 1.17.1

1) Fix problem with MongoDB ([#15](https://github.com/NikitaCartes/EasyAuth/issues/15))
2) Change `config.json`:
   - Delete `mongoDBCredentials` section
   - Add `MongoDBConnectionString` and `MongoDBDatabase` in main section

----
### 1.9.7
##### Minecraft 1.17, 1.17.1

1) Fix crash on account unregistering
2) Add alias `\l` for `\login` and setting for disabling it
3) Allow special characters like `@,#!` in password (you will need to enclose password in quotes if you use them)

----
### 1.9.6
##### Minecraft 1.17, 1.17.1

1) Fix [#11](https://github.com/NikitaCartes/EasyAuth/issues/11)
   - Fix `account unregister <password>` not unregistering account
   - Fix `auth remove <uuid>` crashing server on it's stopping

----
### 1.9.5
##### Minecraft 1.17.1

1) Fix [#8](https://github.com/NikitaCartes/EasyAuth/issues/8)
   - Add [`teleportationTimeoutInMs`](https://github.com/NikitaCartes/EasyAuth/wiki/Config#experimental-part) setting
   - Limit number of packets server will send to unauthorized players
   - Note: this setting is server-wide so maximum rate would be `(1000/teleportationTimeoutInMs)` per seconds for all unauthorised players
   - Value 0 would effectively disable this setting so players will be teleported after each packet, but you can expect a lot of incoming and outgoing packets (up to 3000 and more).

----
### 1.9.3
##### Minecraft 1.17, 1.17.1

1) Server-side translation
2) Changed implementation of supporting SimpleAuth database
   - Now there is a [`useSimpleAuthDatabase`](https://github.com/NikitaCartes/EasyAuth/wiki/Config#experimental-part)
   setting in config

----
### 1.9.1
##### Minecraft 1.17.1

1) Rename mod to EasyAuth
2) Add support fot SimpleAuth database

----
### 1.9.0
##### Minecraft 1.17.1

1) Migrate from Architectury
2) Fix GitHub actions

----
### 1.8.2
##### Minecraft 1.17

1) Fix forceOfflineUuid

----
### 1.8.1
##### Minecraft 1.17

1) Fix some bugs

----
### 1.8.0
##### Minecraft 1.17

1) First 1.17 update
