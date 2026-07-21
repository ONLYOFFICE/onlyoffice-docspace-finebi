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

      if (!this.config.accepts(payload))
        return;

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
