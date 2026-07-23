import { create } from "zustand";
import type { PluginCorePageMode } from "@api/plugin";

interface SessionState {
  mode: PluginCorePageMode | null;
  loggedIn: boolean;
  init(mode: PluginCorePageMode): void;
  setMode(mode: PluginCorePageMode): void;
}

export const useSessionStore = create<SessionState>()((set, get) => ({
  mode: null,
  loggedIn: false,
  init: (mode) => {
    if (get().mode === null) set({ mode, loggedIn: mode === "docspace" });
  },
  setMode: (mode) => set({ mode, loggedIn: mode === "docspace" }),
}));
