'use strict';

const bluebird = require('bluebird');
const _        = require('lodash');

const munge    = require('./serverless-cljs-plugin/munge');
const mkdirp   = bluebird.promisify(require('mkdirp'));
const exec     = bluebird.promisify(require('child_process').exec,
                                    {multiArgs: true});

function handler(fn) {
  return `index.${munge.munge(fn.cljs)}`;
}

function destroyFnMap(service) {
  return _.mapValues(
    service.functions,
    fn => {
      if(fn.cljs) {
        fn.handler = `index.${munge.munge(fn.cljs)}`;
        _.set(fn, 'package.artifact', service.__cljsArtifact);
      }
      return fn;
    });
}

function edn(v) {
  if (_.isArray(v)) {
    return '[' + v.map(edn).join(' ') + ']';
  }
  if (_.isPlainObject(v)) {
    return '{' + _.map(v, (v, k) => ':' + k + ' ' + edn(v)).join(' ') + '}'
  }
  return v;
}

function slsToCljsLambda(functions, opts) {
  return _(functions)
    .pickBy((v, k) => v.cljs && (opts.function ? k === opts.function : true))
    .values()
    .map   (m => {
      return {name: `"${m.name}"`, invoke: m.cljs};
    })
    .thru  (edn)
    .value ();
}

function basepath(config, service, opts) {
  return `${config.servicePath}/.serverless/${opts.function || service.service}`;
}

function cljsLambdaBuild(serverless, opts) {
  const fns = slsToCljsLambda(serverless.service.functions, opts);
  const cmd = (`lein update-in :cljs-lambda assoc :functions '${fns}' ` +
               `-- cljs-lambda build :output ${serverless.service.__cljsArtifact} ` +
               `:quiet`);

  serverless.cli.log(`Executing "${cmd}"`);
  return exec(cmd);
};

const after_createDeploymentArtifacts = bluebird.coroutine(
  function*(serverless, opts) {
    yield mkdirp(`${serverless.config.servicePath}/.serverless`);

    yield cljsLambdaBuild(serverless, opts);

    serverless.cli.log(`Returning artifact path ${serverless.service.__cljsArtifact}`);
    return service.__cljsArtifact;
  });

class ServerlessPlugin {
  constructor(serverless, opts) {
    opts.function = opts.f || opts.function;

    serverless.service.__cljsBasePath = (
      `${basepath(serverless.config, serverless.service, opts)}`);
    serverless.service.__cljsArtifact= `${serverless.service.__cljsBasePath}.zip`;

    serverless.cli.log(`Targeting ${serverless.service.__cljsArtifact}`);

    destroyFnMap(serverless.service);

    const buildAndMerge = after_createDeploymentArtifacts.bind(
      null, serverless, opts);

    this.hooks = {
      'after:deploy:createDeploymentArtifacts': buildAndMerge,
      'after:deploy:function:packageFunction': buildAndMerge
    };
  }
}

module.exports = ServerlessPlugin;
