import { create } from "zustand";
import { HostNotification } from "@api/notification";
import type { NotificationLevel } from "@api/notification";
import type { IHostGlobal } from "@/types/host";

interface NotificationState {
  notify(message: string, level?: NotificationLevel): void;
}

function resolveBI(): Record<string | symbol, unknown> | undefined {
  const own = (window as { BI?: Record<string | symbol, unknown> }).BI;
  if (own) return own;

  try {
    if (window.parent && window.parent !== window)
      return (window.parent as unknown as { BI?: Record<string | symbol, unknown> }).BI;
  } catch {}

  return undefined;
}

export const useNotificationStore = create<NotificationState>()(() => {
  const notification = new HostNotification(
    new Proxy({} as IHostGlobal, {
      get: (_, prop: string | symbol) => resolveBI()?.[prop],
    }),
  );

  return {
    notify: (message, level) => notification.notify(message, level),
  };
});
