// esbuild watch mode for npm dependencies
// Rebuilds vega + vega-embed bundle on changes

import * as esbuild from "esbuild";

const ctx = await esbuild.context({
  entryPoints: ["js-src/index.js"],
  bundle: true,
  format: "iife",
  outfile: "public/js/libs.js",
  minify: false,
  sourcemap: true,
  target: ["es2020"],
  define: {
    "process.env.NODE_ENV": '"development"',
  },
});

await ctx.watch();
console.log("esbuild: Watching for changes...");
