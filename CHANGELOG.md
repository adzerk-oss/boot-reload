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
