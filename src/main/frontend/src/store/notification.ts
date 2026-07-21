import { create } from "zustand";
import { HostNotification } from "@api/notification";
import type { IHostGlobal } from "@/types/host";

interface NotificationState {
  notify(message: string): void;
}

export const useNotificationStore = create<NotificationState>()(() => {
  const notification = new HostNotification(
    new Proxy({} as IHostGlobal, {
      get: (_, prop: string | symbol) =>
        (window as { BI?: Record<string | symbol, unknown> }).BI?.[prop as string],
    }),
  );

  return {
    notify: (message) => notification.notify(message),
  };
});
