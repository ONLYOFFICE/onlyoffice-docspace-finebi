import { useEffect } from "preact/hooks";

import { ExportInjector } from "./ExportInjector";

export function ExportObserver() {
  useEffect(() => {
    const injector = new ExportInjector();
    const refresh = () => injector.observeAll();

    refresh();
    window.addEventListener("hashchange", refresh);
    const timer = window.setInterval(refresh, 2000);

    return () => {
      window.clearInterval(timer);
      window.removeEventListener("hashchange", refresh);
      injector.disconnect();
    };
  }, []);

  return null;
}
