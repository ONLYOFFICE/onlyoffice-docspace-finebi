import { create } from "zustand";

import { ExportClient } from "@features/export/api/client";
import { fetchExportConfig } from "@features/export/api/config";
import type { ExportConfig } from "@features/export/types/config";

interface ExportState {
  config: ExportConfig | null;
  busy: boolean;
  enabled: boolean;
  fetch(): Promise<ExportConfig>;
  run(): Promise<void>;
}

export const useExportStore = create<ExportState>()((set, get) => {
  const client = new ExportClient();

  return {
    config: null,
    busy: false,
    enabled: false,

    async fetch() {
      const cached = get().config;
      if (cached) return cached;
      const config = await fetchExportConfig();
      set({
        config,
        enabled: config.configured && config.loggedIn,
      });
      return config;
    },

    async run() {
      if (get().busy) return;
      set({ busy: true });
      try {
        const message = await client.run(get().config);
        client.notify(message);
      } catch (err) {
        client.notifyError(err);
      } finally {
        set({ busy: false });
      }
    },
  };
});
