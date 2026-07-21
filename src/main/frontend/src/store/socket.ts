import { create } from "zustand";
import { HostSocketEmitterListener } from "@api/socket";
import type { ISocketEmitterListener, ISocketChannelConfig, EmitterHandler } from "@api/socket";

export interface ISocketStore extends ISocketEmitterListener {
  isAvailable(): boolean;
  init(config: ISocketChannelConfig): void;
}

export const useSocketStore = create<ISocketStore>()((set, get) => {
  let instance: (ISocketEmitterListener & { isAvailable(): boolean }) | null = null;

  return {
    init: (config) => {
      instance = new HostSocketEmitterListener(config);
      set({});
    },
    isAvailable: () => instance?.isAvailable() ?? false,
    subscribe: (topic: string) => instance?.subscribe(topic),
    unsubscribe: (topic: string) => instance?.unsubscribe(topic),
    onEmit: (handler: EmitterHandler) => {
      if (!instance) return () => {};
      return instance.onEmit(handler);
    },
  };
});
