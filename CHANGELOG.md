## 0.5.1 (5.2.2017)

- Munge reserved JS names in boot-reload client namespace (Fixes [#113](https://github.com/adzerk-oss/boot-reload/issues/113))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.5.0...0.5.1)**

## 0.5.0 (3.1.2017)

- Close HttpKit server used for Reload WebSocket connection when task is closed ([#107](https://github.com/adzerk-oss/boot-reload/issues/107))
- Uses `localhost` as fallback WebSocket host, but also shows the warning if the
host can't be properly detected (.e.g when serving from `file:` URI). ([#92](https://github.com/adzerk-oss/boot-reload/issues/92), [#98](https://github.com/adzerk-oss/boot-reload/issues/98))
- Checks if `window` methods exists before trying to access them, to prevent problems with React-native and other
environments where they don't exist ([#110](https://github.com/adzerk-oss/boot-reload/pull/110))
- Changed mechanism used by Boot-reload to inject Cljs code to the build.
    - This probably doesn't change anything, but there is small change this
    could fix some rare, hard to reproduce errors about Boot-reload Cljs
    file not being found.
- Use `.cljs.edn` path for Boot-reload client namespace name
- Read client options from `:boot-reload` property in `.cljs.edn` to allow different values for different builds

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.13...0.5.0)**

## 0.4.13 (18.10.2016)

- Generic way tasks to send messages to HUD: https://github.com/adzerk-oss/boot-reload/blob/master/doc/hud-messages.md ([#72](https://github.com/adzerk-oss/boot-reload/issues/72))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.12...0.4.13)**

## 0.4.12 (20.7.2016)

- Fix CLJS logo issues with CLJS 1.9.76 and below ([#87](https://github.com/adzerk-oss/boot-reload/issues/87))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.11...0.4.12)**

## 0.4.11 (27.6.2016)

- Fix CLJS logo reloading issues with CLJS 1.9.76 and up ([#84](https://github.com/adzerk-oss/boot-reload/pull/84))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.10...0.4.11)**

## 0.4.10 (25.6.2016)

- Use absolute paths when reloading files ([#83](https://github.com/adzerk-oss/boot-reload/pull/83))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.9...0.4.10)**

## 0.4.9 (22.6.2016)

- Added `:only-by-re` option to control which files should trigger reload ([#70](https://github.com/adzerk-oss/boot-reload/pull/70))
- Fix reload-css/img through changed-href? ([#80](https://github.com/adzerk-oss/boot-reload/pull/80))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.8...0.4.9)**

## 0.4.8 (25.5.2016)

- Fixed Boot 2.6 compatibility
- Added `ws-port` option ([#73](https://github.com/adzerk-oss/boot-reload/pull/73)) to set WS port for the client

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.7...0.4.8)**

## 0.4.7 (3.4.2016)

- Fixed URIs in Windows ([#71](https://github.com/adzerk-oss/boot-reload/pull/71))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.6...0.4.7)**

## 0.4.6 (24.3.2016)

- Default to current host if `ws-host` is not set ([#62](https://github.com/adzerk-oss/boot-reload/pull/62))
- Add new `cljs-asset-path` option to prepend a path before URL ([#65](https://github.com/adzerk-oss/boot-reload/pull/65))

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.5...0.4.6)**

## 0.4.5 (31.1.2016)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.4...0.4.5)**

- Fixed use with React Native ([#58](https://github.com/adzerk-oss/boot-reload/pull/58))
- Added `disable-hud` option ([#55](https://github.com/adzerk-oss/boot-reload/pull/55))
- Load changed JS files in strict order ([#53](https://github.com/adzerk-oss/boot-reload/pull/53))
    - This might make reload a bit slower, but will guarantee that files are evaluated in dependency order

## 0.4.4 (11.1.2015)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.3...0.4.4)**

- Fixed build

## 0.4.3 (10.1.2015)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.2...0.4.3)**

- **Broken release**
- Added `asset-host` option. It can be used to manually set where the changed files
are reloaded. ([#51](https://github.com/adzerk-oss/boot-reload/issues/51),
[#54](https://github.com/adzerk-oss/boot-reload/issues/54))
- Increased HUD `z-index`.

## 0.4.2 (8.11.2015)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.1...0.4.2)**

- Do not try reloading the main shim file [#46](https://github.com/adzerk-oss/boot-reload/issues/46)
- Fixed asset-path for non-root uses [#43](https://github.com/adzerk-oss/boot-reload/issues/43)

## 0.4.1 (11.10.2015)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.4.0...0.4.1)**

- Cap maximum HUD height
- Guard against cases where HUD container is removed from DOM

## 0.4.0 (4.10.2015)

**[compare](https://github.com/adzerk-oss/boot-reload/compare/0.3.2...0.4.0)**

- Added HUD
    - **Requires**: `[adzerk/boot-cljs "1.7.48.5"]`
    - Reads warning and exception information set by Boot-cljs and displays
    the warnings and exceptions in the browser.
    - Added `open-file` option which can be set to run a command when a warning
    or exception is clicked in browser. This should support e.g. Vim and Emacs.
- Added `secure` option which can be used to tell client to connect using
`wss` protocol.

## 0.3.2 (28.8.2015)

- Add `ws-host` option. Can be used to set the address to which the client connects to.
- Add `ids` option. Can be used to enable reload for only specific Cljs builds.
- Fix #23

## 0.3.1 (14.6.2015)

- Added `:asset-path` option
