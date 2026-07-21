import { HostSpec, HostWidgetEvent, IHostGlobal, IRegisterableWidgetInstance } from "@/types/host";

export interface IRegisterableWidgetConfiguration {
  /** CSS class(es) appended to the widget root (merged with the base class cls). */
  cls?: string;
  /** Fixed pixel width for the root element. */
  width?: number;
  /** Fixed pixel height for the root element. */
  height?: number;
  /** Any additional FineUI config fields. */
  [key: string]: unknown;
}

export type HostWidgetAttached = (event: HostWidgetEvent, handler: () => void) => void;
export interface IHostWidget {
  _super(): void;
  element: { 0?: HTMLElement } & ArrayLike<HTMLElement>;
  options: Record<string, unknown>;
  on(event: HostWidgetEvent, handler: () => void): void;
}

export interface IRegisterableWidgetConfigEntry {
  /** Deduplication key — matches the registerable widget config item's `value` field. */
  value: string;
  text?: string;
  id?: string;
  cls?: string;
  iconCls?: string;
  cardType?: { src: string };
  [key: string]: unknown;
}

export interface IRegisterableWidgetDefinition<
  O extends Record<string, unknown> = Record<string, unknown>,
> {
  /** FineUI shortcut name, e.g. "docspace.dataset.item". */
  type: string;
  /**
   * Extra config fields merged on top of BI.BasicButton's _defaultConfig.
   * Most commonly { cls: "my-class" } to set the widget root's CSS class.
   */
  defaultConfig?: IRegisterableWidgetConfiguration;
  /**
   * Called once after BI.BasicButton's _init runs on the widget root element.
   * - Build the element's inner DOM here.
   * - Use `on` to wire FineUI semantic events (keyboard + mouse, EVENT_CHANGE).
   * - Use DOM addEventListener only for behaviour that needs the raw Event
   *   object (e.g. stopPropagation / preventDefault).
   */
  init: (element: HTMLElement, options: O, on: HostWidgetAttached) => void;
}

export class RegisterableWidgetRegistry {
  private readonly definitions = new Map<string, IRegisterableWidgetDefinition>();
  private readonly registered = new Set<string>();
  
  constructor(private readonly BI: IHostGlobal) {
    this.BI = BI;
  }

  private register(type: string): void {
    if (this.registered.has(type)) return;

    const definition = this.definitions.get(type);
    if (!definition) return;

    if (!this.BI?.inherit || !this.BI?.shortcut || !this.BI?.BasicButton) return;

    const { defaultConfig, init } = definition;
    const spec = {
      _defaultConfig(this: { _super(): Record<string, unknown> }): Record<string, unknown> {
        const base = this._super();
        return defaultConfig ? { ...base, ...defaultConfig } : base;
      },
      _init(this: IHostWidget): void {
        this._super();
        const element = this.element[0];
        if (element)
          init(element, this.options, (event, handler) => this.on(event, handler));
      },
    };
    
    try {
      this.BI.shortcut!(type, this.BI.inherit!(this.BI.BasicButton!, spec as HostSpec));
      this.registered.add(type);
    } catch(e) {
      console.warn("[DocSpace] Widget registration failed:", e);
    }
  }

  /**
   * Store a widget definition. Safe to call before window.BI is ready —
   * BI.shortcut registration is deferred until the first create/createWidget call.
   */
  define<O extends Record<string, unknown> = Record<string, unknown>>(
    definition: IRegisterableWidgetDefinition<O>,
  ): void {
    this.definitions.set(definition.type, definition as IRegisterableWidgetDefinition);
  }

   /**
   * Create a widget and return the full BiWidgetInstance. Use when you need
   * to wire additional FineUI events after creation, call widget methods, or
   * compose the widget inside a FineUI container.
   * Returns null when window.BI is unavailable or creation fails.
   */
   createWidget<O extends Record<string, unknown> = Record<string, unknown>>(
    type: string,
    options?: O,
  ): IRegisterableWidgetInstance | null {
    this.register(type);
    if (!this.BI?.createWidget) return null;
    try {
      return this.BI.createWidget({ type, ...(options ?? {}) }) as IRegisterableWidgetInstance | null;
    } catch {
      return null;
    }
  }

  /**
   * Create a widget and return its root element. Primary API for injecting
   * widgets into existing FineBI DOM (nav popups, export dropdowns, etc.).
   * Returns null when window.BI is unavailable or creation fails; callers
   * should fall back to plain DOM construction.
   */
  create<O extends Record<string, unknown> = Record<string, unknown>>(
    type: string,
    options?: O,
  ): HTMLElement | null {
    const widget = this.createWidget<O>(type, options);
    const element = widget?.element?.[0];
    return element instanceof HTMLElement ? element : null;
  }

  /**
   * Append an entry to a FineUI declarative config array (BI.config).
   * Deduplicates by `entry.value`. Returns false when BI.config is not yet
   * available so the caller can retry later.
   */
  inject(configKey: string, entry: IRegisterableWidgetConfigEntry): boolean {
    if (!this.BI?.config) return false;
    try {
      this.BI.config(configKey, (items) => {
        if (!Array.isArray(items)) return items;
        if (items.some((item) => (item as IRegisterableWidgetConfigEntry)?.value === entry.value)) return items;
        items.push(entry);
        return items;
      });
    } catch (e) {
      console.warn("[DocSpace] Config injection skipped:", e);
    }

    return true;
  }
}
