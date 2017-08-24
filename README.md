# serverless-cljs-plugin

[![npm version](https://badge.fury.io/js/serverless-cljs-plugin.svg)](https://badge.fury.io/js/serverless-cljs-plugin)

A [Serverless](https://github.com/serverless/serverless) plugin which
uses [cljs-lambda](https://github.com/nervous-systems/cljs-lambda) to package
services written in [Clojurescript](https://clojurescript.org/).  At deployment
time, it uses [Leiningen](https://leiningen.org) to start a JVM in which your
functions are compiled to Javascript.

``` shell
$ lein new serverless-cljs example
```

Will generate an `example` directory containing a minimal `serverless.yml` and
`project.clj` demonstrating this plugin's functionality.

### [Guide to using the plugin.](https://nervous.io/clojurescript/lambda/2017/02/06/serverless-cljs/)

## Usage

```yaml
functions:
  echo:
    cljs: example.core/echo

plugins:
 - serverless-cljs-plugin
```

With the above `serverless.yml`, `serverless deploy` will create a zip file
containing your functions.  Doing this is similar to setting the Serverless
`packaging.artifact` option - `cljs-lambda` is responsible for the zip contents,
and Serverless includes/excludes will be skipped (`cljs-lambda` offers
equivalent functionality).

In the example above, there needn't be a corresponding entry for `echo` in
`project.clj`.

#### Lumo compiler

Alternatively you can use the [Lumo](https://github.com/anmonteiro/lumo)
[compiler](https://anmonteiro.com/2017/02/compiling-clojurescript-projects-without-the-jvm/).

In order to enable it either pass the `--lumo` switch to either `deploy` or `package`:

```shell
serverless deploy --lumo
```

Or add the following to your `serverless.yml`:

```yaml
custom:
  cljsCompiler: lumo
```

The source paths and compiler options will be read from `serverless-lumo.edn`:

```clojure
{:source-paths ["src"]
 :compiler-options {:output-to     "out/my/artifact.js" ;; defaults to out/lambda.js
                    :output-dir    "out/my"             ;; defaults to out
                    :optimizations :simple              ;; defaults to :none
                    :source-map    false                ;; defaults to false because of lumo bug #132
                    ...other options...}}
```

## License

serverless-cljs-plugin is free and unencumbered public domain software. For more
information, see http://unlicense.org/ or the accompanying LICENSE
file.
