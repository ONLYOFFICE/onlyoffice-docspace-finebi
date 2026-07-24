import { create } from "zustand";
import { DocSpaceClient } from "@features/docspace/api/client";
import type { DocSpaceFrame, FileSelectorOptions } from "@features/docspace/types";

import type { PluginCoreServerConfiguration } from "@api/plugin";

interface DocSpaceState {
  /** Whether the manager frame host should fill the page. */
  frameVisible: boolean;
  setFrameVisible(visible: boolean): void;
  frameId(): string;
  systemFrameId(): string;
  pickerFrameId(): string;
  /** Open the hidden system frame (cached per URL). */
  ensureFrame(docSpaceUrl: string): Promise<DocSpaceFrame>;
  /** Open the system frame and restore the stored session; returns the normalised URL. */
  connect(config: PluginCoreServerConfiguration): Promise<string>;
  /** Best-effort, time-bounded SDK logout. */
  logout(docSpaceUrl: string): Promise<void>;
  /** Drop the cached system frame (e.g. after tenant reset). */
  reset(): void;
  /** Destroy the manager iframe; keeps the system auth frame. */
  destroyManager(): void;
  /** Destroy the file-selector iframe. */
  destroyPicker(): void;
  /** Open the DocSpace manager UI in the frame. */
  launchManager(docSpaceUrl: string): Promise<void>;
  /** Open the DocSpace file selector (import picker). */
  launchFileSelector(
    docSpaceUrl: string,
    events: NonNullable<FileSelectorOptions["events"]>,
    isCancelled?: () => boolean,
  ): Promise<void>;
}

export const useDocSpaceStore = create<DocSpaceState>()((set) => {
  const client = new DocSpaceClient();
  return {
    frameVisible: false,
    setFrameVisible: (visible) => set({ frameVisible: visible }),
    frameId: () => client.frameId(),
    systemFrameId: () => client.systemFrameId(),
    pickerFrameId: () => client.pickerFrameId(),
    ensureFrame: (url) => client.ensureFrame(url),
    connect: (config) => client.connect(config),
    logout: async (url) => {
      await client.logout(url);
      set({ frameVisible: false });
    },
    reset: () => {
      client.reset();
      set({ frameVisible: false });
    },
    destroyManager: () => {
      client.destroyManager();
      set({ frameVisible: false });
    },
    destroyPicker: () => client.destroyPicker(),
    launchManager: (url) => client.launchManager(url),
    launchFileSelector: (url, events, isCancelled) =>
      client.launchFileSelector(url, events, isCancelled),
  };
});
