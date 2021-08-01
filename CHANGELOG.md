### 1.9.5, Minecraft 1.17.1

1) Fix [#8](https://github.com/NikitaCartes/EasyAuth/issues/8)
    - Add [`teleportationTimeoutInMs`](https://github.com/NikitaCartes/EasyAuth/wiki/Config#experimental-part) setting
    - Limit number of packets server will send to unauthorized players
    - Note: this setting is server-wide so maximum rate would be `(1000/teleportationTimeoutInMs)` per seconds for all unauthorised players
    - Value 0 would effectively disable this setting so players will be teleported after each packet, but you can expect a lot of incoming and outgoing packets (up to 3000 and more).

### 1.9.3, Minecraft 1.17.1, 1.17.0

1) Server-side translation
2) Changed implementation of supporting SimpleAuth database
    - Now there is a [`useSimpleAuthDatabase`](https://github.com/NikitaCartes/EasyAuth/wiki/Config#experimental-part) setting in config

### 1.9.1, Minecraft 1.17.1

1) Rename mod to EasyAuth
2) Add support fot SimpleAuth database

### 1.9.0, Minecraft 1.17.1

1) Update to Minecraft 1.17.1
2) Migrate from Architectury
3) Fix GitHub actions

### 1.8.2, Minecraft 1.17.0

Fix forceOfflineUuid

### 1.8.1, Minecraft 1.17.0

Fix some bugs

### 1.8.0, Minecraft 1.17.0

First 1.17.0 update