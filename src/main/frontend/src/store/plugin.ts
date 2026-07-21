import { create } from "zustand";
import { PluginClient } from "@api/plugin";

interface PluginState {
  login: PluginClient["login"];
  clearTenant: PluginClient["clearTenant"];
  logout: PluginClient["logout"];
  registerWebhook: PluginClient["registerWebhook"];
  importFile: PluginClient["importFile"];
  getFolders: PluginClient["getFolders"];
}

export const usePluginStore = create<PluginState>()(() => {
  const client = new PluginClient();
  return {
    login: (action, fields) => client.login(action, fields),
    clearTenant: (action) => client.clearTenant(action),
    logout: (action) => client.logout(action),
    registerWebhook: (url) => client.registerWebhook(url),
    importFile: (importUrl, params) => client.importFile(importUrl, params),
    getFolders: (foldersUrl) => client.getFolders(foldersUrl),
  };
});
