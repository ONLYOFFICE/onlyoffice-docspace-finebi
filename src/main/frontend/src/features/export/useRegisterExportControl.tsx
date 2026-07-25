import { ExportClient } from "@features/export/api/client";
import { fetchExportConfig } from "@features/export/api/config";

import { usePluginStore } from "@store/plugin";
import { useRegistryStore } from "@store/registry";

import { translate } from "@i18n";

import finebi from "@config/finebi.json";

interface ComboItem {
  value?: string;
  [key: string]: unknown;
}

interface ExportWidget {
  combo?: {
    options?: { items?: ComboItem[][] };
    populate(items: ComboItem[][]): void;
    hideView(): void;
    on(event: string, handler: () => void): void;
  };
  operate(value: unknown): void;
}

let registered = false;
let running = false;


async function runExport(): Promise<void> {
  if (running) return;
  running = true;
  const client = new ExportClient();
  try {
    client.notify(translate("export.uploading"));
    const config = await fetchExportConfig().catch(() => null);
    client.notify(await client.run(config));
  } catch (err) {
    client.notifyError(err);
  } finally {
    running = false;
  }
}

export function useRegisterExportControl(): void {
  if (registered) return;
  registered = true;

  useRegistryStore.getState().registerObject(finebi.export.widgetType, (instance) => {
    const widget = instance as ExportWidget;
    const combo = widget.combo;
    if (!combo?.populate || !combo.on) return;

    combo.on(finebi.export.beforePopupEvent, () => {
      if (!usePluginStore.getState().loggedIn) return;
      const base = (combo.options?.items?.[0] ?? []).slice();
      if (base.some((item) => item?.value === finebi.export.itemValue)) return;
      base.push({
        text: translate("export.menu"),
        value: finebi.export.itemValue,
        cls: finebi.export.itemCls,
      });
      combo.populate([base]);
    });

    const original = widget.operate.bind(widget);
    widget.operate = (value) => {
      if (value !== finebi.export.itemValue) return original(value);
      combo.hideView();
      void runExport();
    };
  });
}