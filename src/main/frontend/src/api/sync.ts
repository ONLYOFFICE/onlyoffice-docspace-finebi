export type EventSourceHandler = (data: string | null) => void;

export interface IEventSource {
  start(): void;
  onEvent(handler: EventSourceHandler): () => void;
}

export interface IEventSourceConfig {
  /** Return false to skip this message entirely. */
  accepts(payload: unknown): boolean;
  /** Extract the data value forwarded to handlers. */
  extract(payload: unknown): string | null;
}

export class HostEventSource implements IEventSource {
  private eventSource: EventSource | null = null;
  private readonly handlers = new Set<EventSourceHandler>();

  constructor(
    private readonly url: string,
    private readonly config: IEventSourceConfig,
  ) {}

  start(): void {
    if (this.eventSource) return;
    this.eventSource = new EventSource(this.url);
    this.eventSource.onmessage = (event) => {
      let payload: unknown;
      try {
        payload = JSON.parse(event.data as string);
      } catch {
        return;
      }

      if (!this.config.accepts(payload)) return;

      const data = this.config.extract(payload);
      this.handlers.forEach((handler) => handler(data));
    };
  }

  onEvent(handler: EventSourceHandler): () => void {
    this.handlers.add(handler);
    return () => {
      this.handlers.delete(handler);
    };
  }
}

interface HostSocket {
  emit(event: string, data: unknown, cb: () => void, opts: { subscribe: boolean }): void;
  on(event: string, handler: (data: unknown) => void): void;
  off(event: string, handler: (data: unknown) => void): void;
}

export type EmitterHandler = () => void;

export interface ISocketFactory {
  isAvailable(): boolean;
  create(): HostSocket;
}

export interface ISocketEmitterListener {
  subscribe(topic: string): void;
  unsubscribe(topic: string): void;
  onEmit(handler: EmitterHandler): () => void;
}

export interface ISocketChannelConfig {
  subscribeEvent: string;
  listenEvent: string;
  emitState(topic: string, active: boolean): unknown;
  matches?(data: unknown, topic: string): boolean;
}

export class HostSocketEmitterListener implements ISocketFactory, ISocketEmitterListener {
  private readonly handlers = new Set<EmitterHandler>();
  private activeTopic: string | null = null;
  private activeSocketHandler: ((data: unknown) => void) | null = null;

  constructor(private readonly config: ISocketChannelConfig) {}

  isAvailable(): boolean {
    const socket = (window as { BI?: { socket?: HostSocket } }).BI?.socket;
    return socket !== undefined && typeof socket.emit === "function" && typeof socket.on === "function";
  }

  create(): HostSocket {
    if (!this.isAvailable()) throw new Error("FineBI socket not available");

    const socket = (window as { BI?: { socket?: HostSocket } }).BI?.socket;
    if (!socket) throw new Error("FineBI socket not found");

    return socket;
  }

  subscribe(topic: string): void {
    const socket = this.create();

    this.activeTopic = topic;
    this.activeSocketHandler = (data: unknown) => {
      if (this.config.matches && !this.config.matches(data, topic)) return;
      this.handlers.forEach((handler) => handler());
    };

    socket.on(this.config.listenEvent, this.activeSocketHandler);
    socket.emit(this.config.subscribeEvent, this.config.emitState(topic, true), () => {}, { subscribe: true });
  }

  unsubscribe(topic: string): void {
    if (!this.activeSocketHandler) return;
    const socket = this.create();

    socket.off(this.config.listenEvent, this.activeSocketHandler);
    socket.emit(this.config.subscribeEvent, this.config.emitState(topic, false), () => {}, { subscribe: false });

    this.activeTopic = null;
    this.activeSocketHandler = null;
  }

  onEmit(handler: EmitterHandler): () => void {
    this.handlers.add(handler);
    return () => this.handlers.delete(handler);
  }
}

interface IBroadcastService {
  broadcastAllWidgets2Refresh(forceRefresh: boolean): void;
}

interface IBroadcastServiceFactory {
  getBroadcastHelper(): IBroadcastService;
}

export interface IWidgetRefresher {
  refreshAll(): boolean;
}

export class HostWidgetRefresher implements IWidgetRefresher {
  refreshAll(): boolean {
    try {
      const BI = (
        window as {
          BI?: {
            TemplateHelperService?: {
              getMap?(): Map<unknown, IBroadcastServiceFactory>;
            };
          };
        }
      ).BI;

      const map = BI?.TemplateHelperService?.getMap?.();
      if (!map) return false;

      let refreshed = false;
      for (const factory of map.values()) {
        if (factory && typeof factory.getBroadcastHelper === "function") {
          factory.getBroadcastHelper().broadcastAllWidgets2Refresh(true);
          refreshed = true;
        }
      }

      return refreshed;
    } catch {
      return false;
    }
  }
}

// TODO: Make it configurable
const FALLBACK_TIMEOUT_MS = 30_000;

export interface INotification {
  showSuccess(): void;
  showFallback(): void;
}

/** Bridges dataset SSE → FineBI socket → widget refresh. */
export class Synchronizer {
  constructor(
    private readonly eventSource: IEventSource,
    private readonly listener: ISocketEmitterListener & { isAvailable(): boolean },
    private readonly refresher: IWidgetRefresher,
    private readonly notification: INotification,
  ) {}

  private refresh(): void {
    const refreshed = this.refresher.refreshAll();
    if (refreshed) this.notification.showSuccess();
    else this.notification.showFallback();
  }

  start(): void {
    this.eventSource.onEvent((name) => this.onSyncReceived(name));
    this.eventSource.start();
  }

  private onSyncReceived(name: string | null): void {
    if (!name || !this.listener.isAvailable()) {
      this.refresh();
      return;
    }

    let done = false;

    const unsubscribe = this.listener.onEmit(() => {
      if (done) return;
      done = true;
      unsubscribe();
      this.listener.unsubscribe(name);
      this.refresh();
    });

    this.listener.subscribe(name);

    setTimeout(() => {
      if (done) return;
      done = true;
      unsubscribe();
      this.listener.unsubscribe(name);
      this.refresh();
    }, FALLBACK_TIMEOUT_MS);
  }
}
