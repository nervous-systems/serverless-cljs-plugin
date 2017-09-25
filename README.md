# serverless-cljs-plugin

[![npm version](https://badge.fury.io/js/serverless-cljs-plugin.svg)](https://badge.fury.io/js/serverless-cljs-plugin)

A [Serverless](https://github.com/serverless/serverless) plugin which
uses lein/[cljs-lambda](https://github.com/nervous-systems/cljs-lambda) (or,
optionally [Lumo](https://github.com/anmonteiro/lumo)) to package services
written in [Clojurescript](https://clojurescript.org/).

## JVM Template

``` shell
$ lein new serverless-cljs example
```

Will generate an `example` directory containing a minimal `serverless.yml` and
`project.clj` demonstrating this plugin's functionality.

### [Guide to using the plugin on the JVM.](https://nervous.io/clojurescript/lambda/2017/02/06/serverless-cljs/)

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

## Lumo

Alternatively you can use the [Lumo](https://github.com/anmonteiro/lumo)
[compiler](https://anmonteiro.com/2017/02/compiling-clojurescript-projects-without-the-jvm/).

In order to enable it, pass the `--lumo` switch to either `deploy` or `package`:

```shell
$ serverless deploy --lumo
```

Or add the following to your `serverless.yml`:

```yaml
custom:
  cljsCompiler: lumo
```

The source paths and compiler options will be read from the optional file
`serverless-lumo.edn`.  Below are the defaults:

```clojure
{:source-paths ["src"]
 :compiler     {:output-to     "out/lambda.js"
                :output-dir    "out"
                :source-map    false ;; because of a bug in lumo <= 1.8.0
                :target        :nodejs
                :optimizations :none}}
```

If `package` is called with `--index` or the following is added to
`serverless.yml`:

```yaml
custom:
  cljsIndex: true
```

A custom `index.js` will be materialized in `:output-dir`'s parent folder. This
file should be thought as managed by `serverless-cljs-plugin` and can be useful
for some plugins (e.g.: [`serverless-offline`](https://github.com/dherault/serverless-offline)).

_Note_: with the defaults above `index.js` will be saved, overwriting without
warning, in the project root (the parent dir of `out`).

### Configuration

As an alternative to `cljsCompiler: lumo`, `cljsCompiler.lumo` may be specified
as a map of options.  Currently supported:

```yml
custom:
  cljsCompiler:
    lumo:
      dependencies:
        - andare:0.7.0
      classpath:
        - /tmp/
      localRepo: /xyz
```

## License

serverless-cljs-plugin is free and unencumbered public domain software. For more
information, see http://unlicense.org/ or the accompanying LICENSE
file.
