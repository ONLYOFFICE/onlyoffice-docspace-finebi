import { useEffect } from "preact/hooks";

import { DatasetInjector } from "./DatasetInjector";

export function DatasetObserver() {
  useEffect(() => {
    const injector = new DatasetInjector();
    injector.observe();
    return () => injector.disconnect();
  }, []);

  return null;
}
