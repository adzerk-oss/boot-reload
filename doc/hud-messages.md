# HUD messages

Shared data format for exception data or warning.

```clj
{:message "Message"
 :file "src/css/main.less.less"
 :line 10
 :column 1}
```

## Exceptions

- *Legacy* `ex-data` contains `:from` property with value `:boot-cljs`
- `ex-data` contains property `:adzerk.boot-reload/exception` with truthy value

## Warnings

- *Legacy* `:adzerk.boot-cljs/warnings` from `.cljs.edn` file fileset metadata
- `:adzerk.boot-reload/warnings` from fileset metadata of any file

## TODO

- Support error/warning context, should probably contain few lines of code near the
error, and information to display proper line numbers for those lines (e.g. start line)
- Should support showing multiple errors, e.g. boot-less can return multiple errors
