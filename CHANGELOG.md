## 0.3.3 (x.x.2015)

- Added HUD
    - Reads warning and exception information set by Boot-cljs and displays
    the warnings and exceptions in the browser.
    - Added `open-file` option which can be set to run a command when a warning
    or exception is clicked in browser. This should support e.g. Vim and Emacs.

## 0.3.2 (28.8.2015)

- Add `ws-host` option. Can be used to set the address to which the client connects to.
- Add `ids` option. Can be used to enable reload for only specific Cljs builds.
- Fix #23

## 0.3.1 (14.6.2015)

- Added `:asset-path` option
