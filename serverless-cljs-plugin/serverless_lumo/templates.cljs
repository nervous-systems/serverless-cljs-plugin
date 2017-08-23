(ns serverless-lumo.templates)

(def template-none
  "require(\"./{{{output-dir}}}/goog/bootstrap/nodejs.js\");

goog.global.CLOSURE_UNCOMPILED_DEFINES = {\"cljs.core._STAR_target_STAR_\":\"nodejs\"};

require(\"./{{{output-to}}}\");

{{#module}}
goog.require(\"{{name}}\");

{{#function}}
exports.{{export}} = {{js-name}};
{{/function}}
{{/module}} ")

(def template-simple
  "var __CLJS_LAMBDA_NS_ROOT = require(\"./{{{output-to}}}\");

{{#module}}
{{#function}}
exports.{{export}} = __CLJS_LAMBDA_NS_ROOT.{{js-name}};
{{/function}}
{{/module}}")

(def template-advanced
  "require(\"./{{{output-to}}}\");

{{#module}}
{{#function}}
exports.{{export}} = {{js-name}};
{{/function}}
{{/module}}")

(def templates
  {:none     template-none
   :simple   template-simple
   :advanced template-advanced})
