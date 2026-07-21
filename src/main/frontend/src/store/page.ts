import { create } from "zustand";
import { PluginCoreServer } from "@api/plugin";
import type { PluginCoreServerConfiguration } from "@api/plugin";

interface PageState {
  config: PluginCoreServerConfiguration | null;
  /**
   * Fetch the current /session config and store it. Used for the initial
   * bootstrap and for every in-app transition (login, logout, reset), so the
   * app re-renders via the router instead of hard-reloading the page.
   */
  load(): Promise<PluginCoreServerConfiguration>;
}

export const usePageStore = create<PageState>()((set) => {
  const server = new PluginCoreServer();
  return {
    config: null,
    async load() {
      const config = await server.load();
      set({ config });
      return config;
    },
  };
});
