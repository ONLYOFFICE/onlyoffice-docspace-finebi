import preact from "@preact/preset-vite";
import { defineConfig, type Plugin } from "vite";
import { fileURLToPath, URL } from "node:url";
import svgr from "vite-plugin-svgr";

import manifest from "../../../manifest.json"

function assetPaths(assetPath: string) {
  const slash = assetPath.lastIndexOf("/");
  return {
    dir: assetPath.substring(0, slash),
    file: assetPath.substring(slash + 1),
    stem: assetPath.substring(slash + 1).replace(/\.[^.]+$/, ""),
  };
}

const nav = assetPaths(manifest.assets.navigation.script);
const navStyle = assetPaths(manifest.assets.navigation.style);

const outDir = fileURLToPath(new URL(
  `../../../target/generated-resources/frontend/${nav.dir}`,
  import.meta.url,
));

function inlineScriptSafe(): Plugin {
  return {
    name: "inline-script-safe",
    generateBundle(_options, bundle) {
      for (const output of Object.values(bundle)) {
        if (output.type === "chunk") {
          output.code = output.code.replace(/<\/script/gi, "<\\/script");
        }
      }
    },
  };
}

export default defineConfig({
  base: "./",
  publicDir: false,
  build: {
    outDir,
    emptyOutDir: true,
    target: "es2017",
    lib: {
      entry: fileURLToPath(new URL("./src/index.tsx", import.meta.url)),
      formats: ["iife"],
      name: "DocSpaceFineBIAuth",
      fileName: () => nav.file,
      cssFileName: navStyle.stem,
    },
  },
  resolve: {
    alias: {
      "@manifest": fileURLToPath(new URL("../../../manifest.json", import.meta.url)),
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@api": fileURLToPath(new URL("./src/api", import.meta.url)),
      "@components": fileURLToPath(new URL("./src/components", import.meta.url)),
      "@features": fileURLToPath(new URL("./src/features", import.meta.url)),
      "@hooks": fileURLToPath(new URL("./src/hooks", import.meta.url)),
      "@pages": fileURLToPath(new URL("./src/pages", import.meta.url)),
      "@utils": fileURLToPath(new URL("./src/utils", import.meta.url)),
      "@store": fileURLToPath(new URL("./src/store", import.meta.url)),
      "@i18n": fileURLToPath(new URL("./src/i18n", import.meta.url)),
      "@resources": fileURLToPath(new URL("./resources", import.meta.url)),
      "@config": fileURLToPath(new URL("./config", import.meta.url)),
    },
  },
  plugins: [
    preact(),
    svgr({ include: "**/*.svg" }),
    inlineScriptSafe(),
  ],
});
