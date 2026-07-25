import { HeaderLogout } from "./HeaderLogout";

import { useRegistryStore } from "@store/registry";
import { useRendererStore } from "@store/renderer";

import finebi from "@config/finebi.json";

interface HeaderItem {
  type?: string;
}

let registered = false;

export function useRegisterHeaderControl(): void {
  if (registered) return;
  registered = true;

  const registry = useRegistryStore.getState();

  registry.define({
    type: finebi.header.itemType,
    defaultConfig: { cls: finebi.header.hostCls },
    init: (element) => {
      useRendererStore.getState().mount(<HeaderLogout />, element);
    },
  });
  registry.activate(finebi.header.itemType);

  registry.configure(finebi.header.itemsKey, (items) => {
    const row = items as HeaderItem[];
    if (Array.isArray(row) && !row.some((item) => item?.type === finebi.header.itemType)) {
      row.unshift({ type: finebi.header.itemType });
    }
    return items;
  });
}
