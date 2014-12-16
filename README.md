# boot-reload

[![Clojars Project][2]][3]

[Boot][1] task to automatically reload resources in the browser when files in
the project change. Communication with the client is via websockets.

* Provides the `reload` task

## Usage

Add `boot-reload` to your `build.boot` dependencies and `require` the namespace:

```clj
(set-env! :dependencies '[[adzerk/boot-reload "X.Y.Z" :scope "test"]])
(require '[adzerk.boot-reload :refer :all])
```

You can see the options available on the command line:

```bash
boot reload -h
```

or in the REPL:

```clj
boot.user=> (doc reload)
```

## Examples

FIXME.

## License

Copyright © 2014 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]:                https://github.com/tailrecursion/boot
[2]:                http://clojars.org/adzerk/boot-reload/latest-version.svg?cache=5
[3]:                http://clojars.org/adzerk/boot-reload
