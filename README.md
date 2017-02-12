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

## License

serverless-cljs-plugin is free and unencumbered public domain software. For more
information, see http://unlicense.org/ or the accompanying LICENSE
file.
