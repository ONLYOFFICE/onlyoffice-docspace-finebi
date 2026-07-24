import { useNotificationStore } from "@store/notification";
import { useSyncStore } from "@store/sync";
import type { INotification } from "@api/sync";
import type { IRegisterableWidgetConfigEntry } from "@api/registry";
import { UrlUtils } from "@utils/url";

import { translate } from "@i18n";

import finebi from "@config/finebi.json";
import manifest from "@manifest";

const notify: INotification = {
  showSuccess() {
    useNotificationStore.getState().notify(translate("sync.refreshed"));
  },
  showFallback() {
    useNotificationStore.getState().notify(translate("sync.fallback"));
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

export function startNavigationRuntime(): void {
  useSyncStore.getState().start({
    eventUrl: `${UrlUtils.hostBaseUrl()}/url${manifest.aliases.events.from}`,
    eventConfig: {
      accepts: (payload) => {
        const p = payload as { type?: string };
        return !p.type || p.type === manifest.events.backend.datasetUpdated;
      },
      extract: (payload) => (payload as { tableName?: string }).tableName ?? null,
    },
    socketConfig: {
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
    },
    notification: notify,
  });
}
