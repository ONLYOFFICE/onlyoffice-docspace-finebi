import { useEventSourceStore } from "@store/eventsource";
import { useNotificationStore } from "@store/notification";
import { useRefresherStore } from "@store/refresher";
import { useSocketStore } from "@store/socket";
import { useSynchronizerStore } from "@store/synchronizer";
import type { INotification } from "@api/synchronizer";
import type { IRegisterableWidgetConfigEntry } from "@api/registry";
import { UrlUtils } from "@utils/url";
import finebi from "@config/finebi.json";
import manifest from "@manifest";

const notify: INotification = {
  showSuccess() {
    useNotificationStore.getState().notify("DocSpace dataset refreshed");
  },
  showFallback() {
    useNotificationStore.getState().notify(
      "DocSpace dataset was updated — refresh the page to see the changes.",
    );
  },
};

export const NAV_ENTRIES: { configKey: string; entry: IRegisterableWidgetConfigEntry }[] = [
  {
    configKey: finebi.navMenuKey,
    entry: {
      value: manifest.module.value,
      text: manifest.module.displayName,
      cls: "onlyoffice-navigation",
      cardType: { src: `${UrlUtils.hostBaseUrl()}/url${manifest.aliases.main.from}` },
    },
  },
  {
    configKey: finebi.managementNavKey,
    entry: {
      value: manifest.module.value,
      id: manifest.module.id,
      text: manifest.module.displayName,
      cls: "onlyoffice-navigation",
      iconCls: "onlyoffice-navigation",
      cardType: { src: `${UrlUtils.hostBaseUrl()}/url${manifest.aliases.admin.from}` },
    },
  },
];

/** Start dataset-sync SSE + FineBI socket listeners. */
export function startNavigationRuntime(): void {
  useEventSourceStore.getState().init(`${UrlUtils.hostBaseUrl()}/url${manifest.aliases.events.from}`, {
    accepts: (payload) => {
      const p = payload as { type?: string };
      return !p.type || p.type === manifest.events.backend.datasetUpdated;
    },
    extract: (payload) => (payload as { tableName?: string }).tableName ?? null,
  });

  useSocketStore.getState().init({
    subscribeEvent: "tableUpdateFinishListener",
    listenEvent: "tableUpdateFinish",
    emitState: (topic, active) => ({
      tableName: encodeURIComponent(topic),
      open: active,
    }),
    matches: (data, topic) => {
      const encoded = encodeURIComponent(topic);
      if (data == null || typeof data !== "object" || !("tableName" in data)) return true;
      return (data as { tableName: string }).tableName === encoded;
    },
  });

  const sync = useSynchronizerStore.getState();
  sync.init(
    useEventSourceStore.getState(),
    useSocketStore.getState(),
    useRefresherStore.getState(),
    notify,
  );
  sync.start();
}
