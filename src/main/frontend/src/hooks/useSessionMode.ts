import { useCallback, useEffect } from "preact/hooks";

import { useEventListener } from "@hooks/useEventListener";

import { PluginCoreServer } from "@api/plugin";
import type { PluginCorePageMode } from "@api/plugin";

import { useSessionStore } from "@store/session";

import { DocSpaceStateEvents } from "@/types/events";

export function useSessionMode(): PluginCorePageMode | null {
  useEffect(() => {
    let active = true;
    new PluginCoreServer()
      .load()
      .then((config) => {
        if (active) useSessionStore.getState().init(config.mode);
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
        useSessionStore.getState().setMode(data.mode as PluginCorePageMode);
      }
    }, []),
  );

  useEventListener(
    DocSpaceStateEvents.reset,
    useCallback(() => useSessionStore.getState().setMode("user"), []),
  );

  return useSessionStore((s) => s.mode);
}

export function useLoggedIn(): boolean {
  useSessionMode();
  return useSessionStore((s) => s.loggedIn);
}
