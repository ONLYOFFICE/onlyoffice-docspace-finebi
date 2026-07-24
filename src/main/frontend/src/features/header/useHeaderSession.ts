import { useCallback, useEffect, useRef, useState } from "preact/hooks";
import { PluginCoreServer } from "@api/plugin";
import type { PluginCorePageMode, PluginCoreServerConfiguration } from "@api/plugin";
import { useDocSpaceStore } from "@store/docspace";
import { usePluginStore } from "@store/plugin";
import { useNotificationStore } from "@store/notification";
import { useEventListener } from "@hooks/useEventListener";
import { useEventPublisher } from "@hooks/useEventPublisher";
import { UrlUtils } from "@utils/url";
import { FuncUtils } from "@utils/func";
import { DocSpaceStateEvents } from "@/types/events";
import manifest from "@manifest";

import { useViewOpen } from "./useViewOpen";

export interface HeaderSession {
  visible: boolean;
  isLoggingOut: boolean;
  logout(): Promise<void>;
}

export function useHeaderSession(): HeaderSession {
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [mode, setMode] = useState<PluginCorePageMode | null>(null);
  const configRef = useRef<PluginCoreServerConfiguration | null>(null);
  
  const viewOpen = useViewOpen();
  const { publish } = useEventPublisher();

  useEffect(() => {
    let active = true;
    new PluginCoreServer()
      .load()
      .then((config) => {
        if (!active) return;
        configRef.current = config;
        setMode((current) => current ?? config.mode);
      })
      .catch(() => { /* ignored */ });
    return () => {
      active = false;
    };
  }, []);

  useEventListener(
    DocSpaceStateEvents.session,
    useCallback((data: Record<string, unknown>) => {
      if (typeof data.mode !== "string") return;
      const next = data.mode as PluginCorePageMode;

      setMode(next);

      if (configRef.current) configRef.current = { ...configRef.current, mode: next };

      setIsLoggingOut(false);
    }, []),
  );

  const logout = useCallback(async () => {
    if (isLoggingOut || mode !== "docspace")
      return;

    setIsLoggingOut(true);

    const config = configRef.current;
    const tenantUrl = UrlUtils.normalize(config?.tenant.docSpaceUrl);
    const logoutUrl = config?.actions.logout || UrlUtils.pluginUrl(manifest.endpoints.logout);
    try {
      if (tenantUrl)
        await useDocSpaceStore.getState().logout(tenantUrl);
      else
        useDocSpaceStore.getState().reset();

      await usePluginStore.getState().logout(logoutUrl);

      setMode("user");
      publish(DocSpaceStateEvents.reset, { teardown: true });
    } catch (err) {
      useNotificationStore.getState().notify(FuncUtils.errorMessage(err), "error");
    } finally {
      setIsLoggingOut(false);
    }
  }, [isLoggingOut, mode, publish]);

  return { visible: mode === "docspace" && viewOpen, isLoggingOut, logout };
}
