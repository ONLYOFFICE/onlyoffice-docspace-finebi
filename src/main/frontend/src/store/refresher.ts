import { create } from "zustand";
import { HostWidgetRefresher } from "@api/refresher";
import type { IWidgetRefresher } from "@api/refresher";

export const useRefresherStore = create<IWidgetRefresher>()(() => {
  const instance = new HostWidgetRefresher();

  return {
    refreshAll: () => instance.refreshAll(),
  };
});
