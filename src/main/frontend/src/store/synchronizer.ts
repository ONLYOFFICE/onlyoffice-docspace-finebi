import { create } from "zustand";
import { Synchronizer } from "@api/synchronizer";
import type { IEventSource } from "@api/eventsource";
import type { ISocketEmitterListener } from "@api/socket";
import type { IWidgetRefresher } from "@api/refresher";
import type { INotification } from "@api/synchronizer";

interface SynchronizerState {
  init(
    eventSource: IEventSource,
    tableUpdateListener: ISocketEmitterListener & { isAvailable(): boolean },
    widgetRefresh: IWidgetRefresher,
    notification: INotification,
  ): void;
  start(): void;
}

export const useSynchronizerStore = create<SynchronizerState>()((set) => {
  let instance: Synchronizer | null = null;

  return {
    init: (eventSource, tableUpdateListener, widgetRefresh, notification) => {
      instance = new Synchronizer(eventSource, tableUpdateListener, widgetRefresh, notification);
      set({});
    },
    start: () => instance?.start(),
  };
});
