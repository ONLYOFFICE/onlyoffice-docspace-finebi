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
}

export const useRegistryStore = create<RegistryState>()((set, get) => {
  const registry = new RegisterableWidgetRegistry(
    new Proxy({} as IHostGlobal, {
      get: (_, prop: string | symbol) =>
        (get().bi as Record<string | symbol, unknown> | null)?.[prop],
    }),
  );

  const pending: [string, IRegisterableWidgetConfigEntry][] = [];

  return {
    bi: null,
    setBi: (bi) => {
      set({ bi });
      for (const [key, entry] of pending.splice(0)) {
        registry.inject(key, entry);
      }
    },
    define: (def) => registry.define(def),
    create: (type, options) => registry.create(type, options),
    createWidget: (type, options) => registry.createWidget(type, options),
    inject: (configKey, entry) => {
      if (!registry.inject(configKey, entry)) {
        pending.push([configKey, entry]);
      }
    },
  };
});
