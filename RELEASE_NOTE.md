### Release notes:
#### Add
- Disable register command if `enable-global-password` enabled with `single-use-global-password` disabled

#### Fix
- Re-register `register` command if `single-use-global-password` was changed
- Wrong log-in message with enabled `single-use-global-password` and disabled `enable-global-password`
- Double login required message on login

#### Changes
- Build EasyAuth against Java 17 [#199](https://github.com/NikitaCartes/EasyAuth/issues/199)
- Improve readability of es_mx language

---

### Full Changelog:
https://github.com/NikitaCartes/EasyAuth/tree/HEAD/CHANGELOG.md