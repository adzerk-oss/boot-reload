## 0.4.1 (11.10.2015)

- Cap maximum HUD height
- Guard against cases where HUD container is removed from DOM

## 0.4.0 (4.10.2015)

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
