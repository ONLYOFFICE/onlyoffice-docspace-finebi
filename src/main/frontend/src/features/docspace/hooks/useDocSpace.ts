import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import type { PluginCoreServerConfiguration } from "@api/plugin";
import { useDocSpaceStore } from "@store/docspace";

export type DocSpaceStatus = "loading" | "ready" | "error";

export function useDocSpace(config: PluginCoreServerConfiguration) {
  const [status, setStatus] = useState<DocSpaceStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const { setFrameVisible, connect, launchManager, destroyManager } =
      useDocSpaceStore.getState();
    setFrameVisible(true);
    void usePluginStore.getState().registerWebhook(config.locations.webhookRegistrationUrl);

    connect(config)
      .then((url) => {
        if (cancelled) return;
        return launchManager(url);
      })
      .then(() => {
        if (cancelled) {
          destroyManager();
          return;
        }
        setStatus("ready");
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setFrameVisible(false);
        setError(e instanceof Error && e.message ? e.message : String(e));
        setStatus("error");
      });

    return () => {
      cancelled = true;
      destroyManager();
    };
  }, []);

  return { status, error };
}
