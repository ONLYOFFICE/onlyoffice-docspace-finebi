import { LocationProvider } from "preact-iso";
import { useCallback, useEffect } from "preact/hooks";

import { useDocSpaceStore } from "@store/docspace";
import { usePageStore } from "@store/page";
import { Frame } from "@features/docspace/components/Frame";
import { DatasetObserver } from "@features/import/DatasetObserver";
import { ExportObserver } from "@features/export";
import { HeaderLogoutObserver } from "@features/header";
import { ShellOverlay } from "@features/import/components/ShellOverlay";
import { NavigationInjector } from "@features/navigation/NavigationInjector";
import { NAV_ENTRIES, startNavigationRuntime } from "@features/navigation/runtime";
import { useEventListener } from "@hooks/useEventListener";
import { useEventPublisher } from "@hooks/useEventPublisher";
import { DocSpaceStateEvents } from "@/types/events";
import { AppRoutes } from "@/routes";

import "@features/navigation/navigation.css";

export function PageApp() {
  const config = usePageStore((s) => s.config);
  const framed = useDocSpaceStore((s) => s.frameVisible);
  const { publish } = useEventPublisher();

  useEffect(() => {
    if (!config?.mode) return;
    publish(DocSpaceStateEvents.session, { mode: config.mode });
  }, [config?.mode, publish]);

  useEventListener(
    DocSpaceStateEvents.reset,
    useCallback(() => {
      void usePageStore.getState().load();
    }, []),
  );

  if (!config) return null;

  return (
    <LocationProvider>
      <div class={`onlyoffice-root${framed ? " onlyoffice-root--framed" : ""}`}>
        <Frame />
        <div class="onlyoffice-root__app">
          <AppRoutes />
        </div>
      </div>
    </LocationProvider>
  );
}

export function NavigationApp() {
  useEffect(() => {
    startNavigationRuntime();
  }, []);

  return (
    <>
      <Frame />
      {NAV_ENTRIES.map(({ configKey, entry }) => (
        <NavigationInjector key={configKey} configKey={configKey} entry={entry} />
      ))}
      <HeaderLogoutObserver />
      <ExportObserver />
      <DatasetObserver />
      <ShellOverlay />
    </>
  );
}
