### 3.1.0
##### Minecraft TBA

1) Added single use global password:
   - When `single-use-global-password` is enabled, player can register with global password but not log in with it
   - Default `false`
2) Improve online/offline player separation:
   - Add `account online` command that mark player as online
   - Add `auth markAsOffline <player>` and `auth markAsOnline <player>` commands
   - Add option `offline-by-default` (default `false`) to mark all players as offline by default
   - Delete `auth addToForcedOffline <player>` command
3) Add `hide-inventory` option in `extended.conf` to hide inventory of unauthenticated players. Default `true`
4) Add ability to log in with OTP:
   - `enable-otp` in `main.conf` to enable OTP. Default `true`
   - `account otp generate` to generate OTP secret
   - `account otp enable <otp code>` to enable OTP
   - `account otp disable <otp code>` to disable OTP
   - `account enable2Fa <password> <otp code>` to enable 2FA
   - `account disable2Fa <password> <otp code>` to disable 2FA
   - `auth resetOtp <player>` admin command to reset OTP for player
5) Player allowed to log in even if player with same nickname is already online if they join from same IP
6) Database overhaul:
   - Now database key is username instead of uuid
   - SQLite is now default database instead of LevelDB
   - Drop support for LevelDB (data from LevelDB will be migrated to SQLite automatically)
   - Config version is now 2
   - All players from `usercache.json` will be migrated automatically
7) Change default hash algorithm to Argon2 from BCrypt
   - If you previously used BCrypt, typed password will be checked both against BCrypt and Argon2 (option `check-unmigrated-argon2` in `extended.conf`)
8) Increased default `teleportation-timeout-ms` from 5 to 20 ms
9) `confirmed-online-players` and `forced-offline-players` is not used anymore. Now they are stored in database for each player separately
10) Added new option `allow-case-insensitive-username` in `extended.conf` to allow players with same nickname but different case to join (default `false`)
11) Fix bug with respawn while leaving server being dead
12) Add setting `authentication-prompt-interval` in `extended.conf` to set interval between authentication prompts
13) Add `auth getPlayerInfo` command
14) Add `mojang-api-settings` in `extended.conf` for custom Mojang API settings
 
### 3.0.28
##### Minecraft 1.21.2 - 1.21.4

1) Fix [#164](https://github.com/NikitaCartes/EasyAuth/issues/164), incompatibility with C2ME and hide player coords

### 3.0.27
##### Minecraft 1.21.2 - 1.21.4

1) Fix session issue for real this time

### 3.0.26
##### Minecraft 1.21.2, 1.21.3

1) Update Simplified Chinese translation, thanks to @CMJNB and @MyBlueHorizon
2) Update French translation, thanks to @Sky-NiniKo
3) Update Ukrainian translation, thanks to @Y0shioSato
4) Fix uuid wasn't clickable in chat in some languages
5) Fix connection bug

### 3.0.25
##### Minecraft 1.21-1.21.3

1) Fix toLowerCase() using on PC with Turkish locale

### 3.0.25
##### Minecraft 1.20.5-1.20.6, 1.21

1) Fix premium player being invulnerable after using `/logout` command

### 3.0.24
##### Minecraft 1.20.5-1.20.6

1) Fix spawning in wrong dimension with `hide-player-coords` enabled

### 3.0.23
##### Minecraft 1.20.5-1.20.6

1) Fix `auth list`.

### 3.0.22-SNAPSHOT
##### Minecraft 1.20.3-1.20.4

1) Fix mod not working not in dev environment

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
