import { create } from "zustand";

import { PluginClient, PluginCoreServer } from "@api/plugin";
import type { PluginCorePageMode, PluginCoreServerConfiguration } from "@api/plugin";

function loggedInFor(mode: PluginCorePageMode): boolean {
  return mode === "docspace" || mode === "settings";
}

interface PluginState {
  config: PluginCoreServerConfiguration | null;
  mode: PluginCorePageMode | null;
  loggedIn: boolean;
  load(): Promise<PluginCoreServerConfiguration>;
  setMode(mode: PluginCorePageMode): void;
  login: PluginClient["login"];
  clearTenant: PluginClient["clearTenant"];
  logout: PluginClient["logout"];
  registerWebhook: PluginClient["registerWebhook"];
  importFile: PluginClient["importFile"];
  getFolders: PluginClient["getFolders"];
}

export const usePluginStore = create<PluginState>()((set) => {
  const server = new PluginCoreServer();
  const client = new PluginClient();
  return {
    config: null,
    mode: null,
    loggedIn: false,
    async load() {
      const config = await server.load();
      set({ config, mode: config.mode, loggedIn: loggedInFor(config.mode) });
      return config;
    },
    setMode: (mode) => set({ mode, loggedIn: loggedInFor(mode) }),
    login: (action, fields) => client.login(action, fields),
    clearTenant: (action) => client.clearTenant(action),
    logout: (action) => client.logout(action),
    registerWebhook: (url) => client.registerWebhook(url),
    importFile: (importUrl, params) => client.importFile(importUrl, params),
    getFolders: (foldersUrl) => client.getFolders(foldersUrl),
  };
});
