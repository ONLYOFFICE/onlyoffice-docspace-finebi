import { create } from "zustand";

import { openShellOverlay } from "@features/import/components/ShellOverlay";
import { ImportUrlUtils } from "@features/import/utils/url";
import { useExportStore } from "@features/export";
import { useNotificationStore } from "@store/notification";

interface ImportState {
  pick(): Promise<void>;
}

async function ensureReady(): Promise<boolean> {
  const { notify } = useNotificationStore.getState();
  try {
    const { configured, loggedIn } = await useExportStore.getState().fetch();
    if (!configured) {
      notify("A FineBI administrator must configure the DocSpace tenant first.");
      return false;
    }

    if (!loggedIn) {
      notify("Sign in to DocSpace first.");
      return false;
    }

    return true;
  } catch {
    notify("A FineBI administrator must configure the DocSpace tenant first.");
    return false;
  }
}

export const useImportStore = create<ImportState>()(() => ({
  async pick() {
    if (!(await ensureReady())) return;
    openShellOverlay(ImportUrlUtils.picker());
  },
}));
