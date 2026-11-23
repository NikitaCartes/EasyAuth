### Release notes:
#### Fix
- No death screen for a player who left the server dead and joined again before login [#292](https://github.com/NikitaCartes/EasyAuth/issues/292)
- NullPointerException in `ServerLoginPacketListenerImpl.tick` ("Internal server error") when an offline player joins: the login state was set before the player profile

---

### Full Changelog:
https://github.com/NikitaCartes/EasyAuth/tree/HEAD/CHANGELOG.md