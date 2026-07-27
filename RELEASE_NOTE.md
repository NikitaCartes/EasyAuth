### Release notes:
#### Fix
- `playerInvulnerable` not blocking damage before authentication (players took hits from other players, explosions and potions)
- NullPointerException in restoreTrueLocation when client disconnects mid-join [#276](https://github.com/NikitaCartes/EasyAuth/issues/276)
- Endless vanish on login with `vanish-until-auth`: a pre-auth vanish saved on disconnect reloaded as a permanent vanish [#278](https://github.com/NikitaCartes/EasyAuth/issues/278)

---

### Full Changelog:
https://github.com/NikitaCartes/EasyAuth/tree/HEAD/CHANGELOG.md