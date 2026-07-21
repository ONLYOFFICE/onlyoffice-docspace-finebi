import { useEffect } from "preact/hooks";

import { ExportInjector } from "./ExportInjector";
import { useExportStore } from "./store/export";

export function ExportObserver() {
  useEffect(() => {
    const injector = new ExportInjector();
    const refresh = () => injector.observeAll();

    refresh();

    void useExportStore.getState().fetch().catch(() => {});
    const unsubscribe = useExportStore.subscribe(() => injector.sync());

    window.addEventListener("hashchange", refresh);

    const timer = window.setInterval(refresh, 2000);
    return () => {
      unsubscribe();
      window.clearInterval(timer);
      window.removeEventListener("hashchange", refresh);
      injector.disconnect();
    };
  }, []);

  return null;
}
