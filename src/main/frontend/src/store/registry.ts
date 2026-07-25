import { create } from "zustand";
import type { IHostGlobal, IRegisterableWidgetInstance } from "@/types/host";

import { RegisterableWidgetRegistry } from "@api/registry";
import type { IRegisterableWidgetDefinition, IRegisterableWidgetConfigEntry } from "@api/registry";

interface RegistryState {
  bi: IHostGlobal | null;
  setBi(bi: IHostGlobal): void;
  define<O extends Record<string, unknown>>(def: IRegisterableWidgetDefinition<O>): void;
  create<O extends Record<string, unknown>>(type: string, options?: O): HTMLElement | null;
  createWidget<O extends Record<string, unknown>>(type: string, options?: O): IRegisterableWidgetInstance | null;
  inject(configKey: string, entry: IRegisterableWidgetConfigEntry): void;
  configure(configKey: string, transform: (items: unknown[]) => unknown[]): void;
  activate(type: string): void;
}

export const useRegistryStore = create<RegistryState>()((set, get) => {
  const registry = new RegisterableWidgetRegistry(
    new Proxy({} as IHostGlobal, {
      get: (_, prop: string | symbol) =>
        (get().bi as Record<string | symbol, unknown> | null)?.[prop],
    }),
  );

  const pending: [string, IRegisterableWidgetConfigEntry][] = [];
  const pendingConfig: [string, (items: unknown[]) => unknown[]][] = [];
  const pendingActivations: string[] = [];

  return {
    bi: null,
    setBi: (bi) => {
      set({ bi });
      for (const type of pendingActivations.splice(0))
        registry.activate(type);

      for (const [key, entry] of pending.splice(0))
        registry.inject(key, entry);

      for (const [key, transform] of pendingConfig.splice(0))
        registry.configure(key, transform);
    },
    define: (def) => registry.define(def),
    create: (type, options) => registry.create(type, options),
    createWidget: (type, options) => registry.createWidget(type, options),
    inject: (configKey, entry) => {
      if (!registry.inject(configKey, entry)) {
        pending.push([configKey, entry]);
      }
    },
    configure: (configKey, transform) => {
      if (!registry.configure(configKey, transform))
        pendingConfig.push([configKey, transform]);
    },
    activate: (type) => {
      if (!registry.activate(type))
        pendingActivations.push(type);
    },
  };
});
