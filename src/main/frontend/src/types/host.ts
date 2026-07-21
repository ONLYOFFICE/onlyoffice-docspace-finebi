export type HostRecord = Record<string, unknown>;

export type HostSpec = {
  _init?: (this: IHostGlobal) => void;
  _defaultConfig?: (this: { _super(): Record<string, unknown> }) => Record<string, unknown>;
  [key: string]: unknown;
};

export type HostWidgetEvent = "EVENT_CHANGE" | (string & {});

export interface IRegisterableWidgetInstance {
    element: { 0?: HTMLElement } & ArrayLike<HTMLElement>;
    options: Record<string, unknown>;
    on(event: HostWidgetEvent, handler: () => void): void;
  }

export interface IHostGlobal {
  config?(key: string, fn: (items: unknown[]) => unknown[]): void;
  Msg?: { toast?(text: string, options?: { level?: string }): void };
  createWidget?(
    opts: Record<string, unknown>,
  ): IRegisterableWidgetInstance | null;
  inherit?(base: new (...args: unknown[]) => unknown, spec: HostSpec): unknown;
  shortcut?(name: string, cls: unknown): void;
  extend?(
    target: Record<string, unknown>,
    ...rest: Record<string, unknown>[]
  ): Record<string, unknown>;
  BasicButton?: new (...args: unknown[]) => unknown;
}
