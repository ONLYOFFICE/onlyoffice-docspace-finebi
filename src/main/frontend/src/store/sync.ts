import { create } from "zustand";

import {
  HostEventSource,
  HostSocketEmitterListener,
  HostWidgetRefresher,
  Synchronizer,
} from "@api/sync";
import type {
  IEventSourceConfig,
  INotification,
  ISocketChannelConfig,
} from "@api/sync";

interface SyncState {
  start(options: {
    eventUrl: string;
    eventConfig: IEventSourceConfig;
    socketConfig: ISocketChannelConfig;
    notification: INotification;
  }): void;
}

export const useSyncStore = create<SyncState>()(() => {
  const refresher = new HostWidgetRefresher();

  return {
    start({ eventUrl, eventConfig, socketConfig, notification }) {
      const eventSource = new HostEventSource(eventUrl, eventConfig);
      const socket = new HostSocketEmitterListener(socketConfig);
      const synchronizer = new Synchronizer(eventSource, socket, refresher, notification);
      synchronizer.start();
    },
  };
});
