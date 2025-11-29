// esbuild production build for npm dependencies
// Bundles vega + vega-embed for shadow-cljs external provider

import * as esbuild from "esbuild";

await esbuild.build({
  entryPoints: ["js-src/index.js"],
  bundle: true,
  format: "iife",
  outfile: "public/js/libs.js",
  minify: true,
  sourcemap: true,
  target: ["es2020"],
  define: {
    "process.env.NODE_ENV": '"production"',
  },
});

console.log("esbuild: Built public/js/libs.js");
