import { useCallback, useEffect } from "preact/hooks";

import { useEventListener } from "@hooks/useEventListener";

import type { PluginCorePageMode } from "@api/plugin";

import { usePluginStore } from "@store/plugin";

import { DocSpaceStateEvents } from "@/types/events";

export function useSessionMode(): PluginCorePageMode | null {
  useEffect(() => {
    void usePluginStore.getState().load().catch(() => {});
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
    useCallback(() => {
      void usePluginStore.getState().load().catch(() => {});
    }, []),
  );

  return usePluginStore((s) => s.mode);
}

export function useLoggedIn(): boolean {
  useSessionMode();
  return usePluginStore((s) => s.loggedIn);
}
