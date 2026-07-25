import { useEffect } from "preact/hooks";

import { openShellOverlay } from "@features/import/components/ShellOverlay";
import { ImportUrlUtils } from "@features/import/utils/url";

import { useLoggedIn } from "@hooks/useSessionMode";

import { usePluginStore } from "@store/plugin";
import { useRegistryStore } from "@store/registry";

import { translate } from "@i18n";

import finebi from "@config/finebi.json";

interface AddTableItem {
  value?: string;
  [key: string]: unknown;
}

export function ImportInitializer() {
  const loggedIn = useLoggedIn();

  useEffect(() => {
    useRegistryStore.getState().configure(finebi.dataset.addTableKey, (items) => {
      const row = (items as AddTableItem[][])[0];
      if (Array.isArray(row) && !row.some((item) => item?.value === finebi.dataset.tableType)) {
        row.push({
          text: translate("import.menu"),
          value: finebi.dataset.tableType,
          tableType: finebi.dataset.tableType,
          cls: finebi.dataset.itemCls,
          iconWidth: 16,
          iconHeight: 16,
        });
      }
      return items;
    });

    const onClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null;
      if (!target?.closest?.(`.${finebi.dataset.itemCls}`)) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      if (!usePluginStore.getState().loggedIn) return;
      document.body.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
      openShellOverlay(ImportUrlUtils.picker());
    };

    document.addEventListener("click", onClick, true);
    return () => document.removeEventListener("click", onClick, true);
  }, []);

  useEffect(() => {
    document.body.classList.toggle(finebi.dataset.hiddenClass, !loggedIn);
    return () => document.body.classList.remove(finebi.dataset.hiddenClass);
  }, [loggedIn]);

  return null;
}
