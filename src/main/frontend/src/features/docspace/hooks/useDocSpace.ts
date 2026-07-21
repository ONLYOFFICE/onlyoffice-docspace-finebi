import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import type { PluginCoreServerConfiguration } from "@api/plugin";
import { useDocSpaceStore } from "@store/docspace";

export type DocSpaceStatus = "loading" | "ready" | "error";

export function useDocSpace(config: PluginCoreServerConfiguration) {
  const [status, setStatus] = useState<DocSpaceStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const { setFrameVisible, connect, launchManager } = useDocSpaceStore.getState();
    setFrameVisible(true);
    void usePluginStore.getState().registerWebhook(config.locations.webhookRegistrationUrl);

    connect(config)
      .then((url) => launchManager(url))
      .then(() => setStatus("ready"))
      .catch((e: unknown) => {
        setFrameVisible(false);
        setError(e instanceof Error && e.message ? e.message : String(e));
        setStatus("error");
      });

    return () => {
      setFrameVisible(false);
    };
  }, []);

  return { status, error };
}
