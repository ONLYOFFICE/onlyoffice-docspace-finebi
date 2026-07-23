import { useCallback, useEffect } from "preact/hooks";

import { useEventListener } from "@hooks/useEventListener";

import { PluginCoreServer } from "@api/plugin";
import type { PluginCorePageMode } from "@api/plugin";

import { usePluginStore } from "@store/plugin";

import { DocSpaceStateEvents } from "@/types/events";

export function useSessionMode(): PluginCorePageMode | null {
  useEffect(() => {
    let active = true;
    new PluginCoreServer()
      .load()
      .then((config) => {
        if (active) usePluginStore.getState().init(config.mode);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  useEventListener(
    DocSpaceStateEvents.session,
    useCallback((data: Record<string, unknown>) => {
      if (typeof data.mode === "string") {
        usePluginStore.getState().setMode(data.mode as PluginCorePageMode);
      }
    }, []),
  );

  useEventListener(
    DocSpaceStateEvents.reset,
    useCallback(() => usePluginStore.getState().setMode("user"), []),
  );

  return usePluginStore((s) => s.mode);
}

export function useLoggedIn(): boolean {
  useSessionMode();
  return usePluginStore((s) => s.loggedIn);
}
