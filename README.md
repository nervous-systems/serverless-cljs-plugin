# serverless-cljs-plugin

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

## License

serverless-cljs-plugin is free and unencumbered public domain software. For more
information, see http://unlicense.org/ or the accompanying LICENSE
file.
