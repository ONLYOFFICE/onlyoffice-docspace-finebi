import type { IEventSource } from "@api/eventsource";
import type { ISocketEmitterListener } from "@api/socket";
import type { IWidgetRefresher } from "@api/refresher";

// TODO: Make it configurable
const FALLBACK_TIMEOUT_MS = 30_000;

export interface INotification {
  showSuccess(): void;
  showFallback(): void;
}

export class Synchronizer {
  constructor(
    private readonly eventSource: IEventSource,
    private readonly listener: ISocketEmitterListener & {
      isAvailable(): boolean;
    },
    private readonly refresher: IWidgetRefresher,
    private readonly notification: INotification,
  ) {}

  private refresh(): void {
    const refreshed = this.refresher.refreshAll();
    if (refreshed) {
      this.notification.showSuccess();
    } else {
      this.notification.showFallback();
    }
  }

  start(): void {
    this.eventSource.onEvent((name) => {
      this.onSyncReceived(name);
    });

    this.eventSource.start();
  }

  private onSyncReceived(name: string | null): void {
    if (!name || !this.listener.isAvailable()) {
      this.refresh();
      return;
    }

    let done = false;

    const unsubscribe = this.listener.onEmit(() => {
      if (done) 
        return;

      done = true;

      unsubscribe();

      this.listener.unsubscribe(name);
      this.refresh();
    });

    this.listener.subscribe(name);

    setTimeout(() => {
      if (done)
        return;

      done = true;
      unsubscribe();

      this.listener.unsubscribe(name);
      this.refresh();
    }, FALLBACK_TIMEOUT_MS);
  }
}
