import { useEffect } from "preact/hooks";

import { openImportPickerFrame } from "@features/import/components/ImportPickerFrame";
import { ImportUrlUtils } from "@features/import/utils/url";

import { useLoggedIn } from "@hooks/useSessionMode";

import { usePluginStore } from "@store/plugin";
import { useRegistryStore } from "@store/registry";

import { translate } from "@i18n";

import finebi from "@config/finebi.json";

const OPEN_DEBOUNCE_MS = 500;
let lastOpenAt = 0;

interface AddTableItem {
  value?: string;
  [key: string]: unknown;
}

interface PackListStore {
  openAddTableLayer?(value: unknown): void;
  folderItemOperator?(operation: unknown, folder: unknown, ref: unknown): void;
  __onlyofficeImportPatched?: boolean;
}

interface PackListWidget {
  store?: PackListStore;
}

function openDocSpaceImport(): void {
  if (!usePluginStore.getState().loggedIn)
    return;
  const now = Date.now();
  if (now - lastOpenAt < OPEN_DEBOUNCE_MS)
    return;
  lastOpenAt = now;
  openImportPickerFrame(ImportUrlUtils.picker());
}

function isDocSpaceType(value: unknown): boolean {
  return value === finebi.dataset.tableType;
}

function patchPackListStore(store: PackListStore | undefined): void {
  if (!store || store.__onlyofficeImportPatched) return;
  store.__onlyofficeImportPatched = true;

  const openOriginal = store.openAddTableLayer?.bind(store);
  if (openOriginal)
    store.openAddTableLayer = (value) => {
      if (isDocSpaceType(value)) return openDocSpaceImport();
      return openOriginal(value);
    };

  const folderOriginal = store.folderItemOperator?.bind(store);
  if (folderOriginal)
    store.folderItemOperator = (operation, folder, ref) => {
      if (isDocSpaceType(operation)) return openDocSpaceImport();
      return folderOriginal(operation, folder, ref);
    };
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

    useRegistryStore.getState().registerObject(finebi.dataset.packListType, (instance) => {
      patchPackListStore((instance as PackListWidget).store);
    });
  }, []);

  useEffect(() => {
    document.body.classList.toggle(finebi.dataset.hiddenClass, !loggedIn);
    return () => document.body.classList.remove(finebi.dataset.hiddenClass);
  }, [loggedIn]);

  return null;
}
