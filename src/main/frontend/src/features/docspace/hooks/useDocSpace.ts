import { useEffect, useState } from "preact/hooks";

import type { PluginCoreServerConfiguration } from "@api/plugin";

import { usePluginStore } from "@store/plugin";
import { useDocSpaceStore } from "@store/docspace";

export type DocSpaceStatus = "loading" | "ready" | "error";

export function useDocSpace(config: PluginCoreServerConfiguration) {
  const [status, setStatus] = useState<DocSpaceStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const { setFrameVisible, connect, launchManager, reset } =
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
          reset();
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
      reset();
      setFrameVisible(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { status, error };
}
