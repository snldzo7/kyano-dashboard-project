// JS entry point for esbuild
// Bundles npm deps and creates shadow$bridge for shadow-cljs external provider

import * as vega from 'vega';
import vegaEmbed from 'vega-embed';

// shadow-cljs external provider bridge
const ALL = {};
globalThis.shadow$bridge = function(name) {
  const ret = ALL[name];
  if (ret === undefined) {
    throw new Error("Dependency: " + name + " not provided by external JS!");
  }
  return ret;
};

ALL["vega"] = vega;
ALL["vega-embed"] = { default: vegaEmbed };
