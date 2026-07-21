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
        if (!this.isAvailable()) 
          throw new Error("FineBI socket not available");

        const socket = (window as { BI?: { socket?: HostSocket } }).BI?.socket;
        if (!socket)
          throw new Error("FineBI socket not found");

        return socket;
    }

    subscribe(topic: string): void {
        const socket = this.create();

        this.activeTopic = topic;
        this.activeSocketHandler = (data: unknown) => {
            if (this.config.matches && !this.config.matches(data, topic))
              return;
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
