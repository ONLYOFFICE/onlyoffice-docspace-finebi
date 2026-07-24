import { useRef } from "preact/hooks";

import type { IRegisterableWidgetConfigEntry } from "@api/registry";

import { useRegistryStore } from "@store/registry";

interface NavigationInjectorProps {
  configKey: string;
  entry: IRegisterableWidgetConfigEntry;
}

export function NavigationInjector({ configKey, entry }: NavigationInjectorProps) {
  const injected = useRef(false);
  if (!injected.current) {
    injected.current = true;
    useRegistryStore.getState().inject(configKey, entry);
  }

  return null;
}
