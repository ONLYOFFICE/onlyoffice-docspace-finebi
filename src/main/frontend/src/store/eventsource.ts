import { create } from "zustand";
import { HostEventSource } from "@api/eventsource";
import type { IEventSource, IEventSourceConfig, EventSourceHandler } from "@api/eventsource";

interface EventSourceState extends IEventSource {
  init(url: string, config: IEventSourceConfig): void;
}

export const useEventSourceStore = create<EventSourceState>()((set) => {
  let instance: IEventSource | null = null;

  return {
    init: (url, config) => {
      instance = new HostEventSource(url, config);
      set({});
    },
    start: () => instance?.start(),
    onEvent: (handler: EventSourceHandler) => {
      if (!instance) return () => {};
      return instance.onEvent(handler);
    },
  };
});
