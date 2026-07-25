import { LocationProvider } from "preact-iso";
import { useCallback, useEffect, useRef } from "preact/hooks";

import { DocSpaceStateEvents } from "@/types/events";

import { useViewOpen } from "@features/header/useViewOpen";
import { Frame } from "@features/docspace/components/Frame";
import { ImportInitializer } from "@features/import/ImportInitializer";
import { ShellOverlay } from "@features/import/components/ShellOverlay";
import { NavigationInjector } from "@features/navigation/NavigationInjector";
import { NAV_ENTRIES, startNavigationRuntime } from "@features/navigation/runtime";

import { useEventListener } from "@hooks/useEventListener";
import { useEventPublisher } from "@hooks/useEventPublisher";

import { useDocSpaceStore } from "@store/docspace";
import { usePluginStore } from "@store/plugin";

import { AppRoutes } from "@/routes";

import "@features/navigation/navigation.css";

export function PageApp() {
  const config = usePluginStore((s) => s.config);
  const framed = useDocSpaceStore((s) => s.frameVisible);
  const { publish } = useEventPublisher();

  useEffect(() => {
    if (!config?.mode) return;
    publish(DocSpaceStateEvents.session, { mode: config.mode });
  }, [config?.mode, publish]);

  useEventListener(
    DocSpaceStateEvents.reset,
    useCallback((data: Record<string, unknown>) => {
      if (data.teardown === true) useDocSpaceStore.getState().destroyManager();
      void usePluginStore.getState().load();
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
  const { publish } = useEventPublisher();
  const viewOpen = useViewOpen();
  const wasOpen = useRef(false);

  useEffect(() => {
    startNavigationRuntime();
  }, []);

  useEffect(() => {
    if (viewOpen && !wasOpen.current) publish(DocSpaceStateEvents.reset);
    wasOpen.current = viewOpen;
  }, [viewOpen, publish]);

  return (
    <>
      <Frame />
      {NAV_ENTRIES.map(({ configKey, entry }) => (
        <NavigationInjector key={configKey} configKey={configKey} entry={entry} />
      ))}
      <ImportInitializer />
      <ShellOverlay />
    </>
  );
}
