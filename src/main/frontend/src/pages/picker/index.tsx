import { useState } from "preact/hooks";

import { FilePicker, FolderPicker } from "@features/import";
import type { DocSpaceItem } from "@features/docspace/types";
import { ImportUrlUtils } from "@features/import/utils/url";

import { useEventPublisher } from "@hooks/useEventPublisher";

import { useNotificationStore } from "@store/notification";
import { usePluginStore } from "@store/plugin";

import { FileUtils } from "@utils/file";
import { FuncUtils } from "@utils/func";

import { translate } from "@i18n";

import manifest from "@manifest";

import type { Stage } from "./stage";

function importErrorMessage(result: {
  error?: string;
  errorCode?: string;
  errorParams?: Record<string, string | number>;
}): string {
  if (result.errorCode) {
    const localized = translate(result.errorCode, result.errorParams);
    if (localized !== result.errorCode)
      return localized;
  }

  return result.error ?? translate("import.failed");
}

export function PickerPage() {
  const config = usePluginStore((s) => s.config);
  const [stage, setStage] = useState<Stage>({ name: "picker" });
  const { publish } = useEventPublisher();

  if (!config) return null;
  const session = config;

  const close = () => publish(manifest.events.frontend.filePicker, { action: "close" });

  function finish(message: string, type: "success" | "error", action: "close" | "imported"): void {
    useNotificationStore.getState().notify(message, type);
    publish(manifest.events.frontend.filePicker, { action });
  }

  function onFileSelect(items: DocSpaceItem[]): void {
    const file = items.find(FileUtils.isImportable);
    if (!file) {
      finish(translate("import.only.excel.csv"), "error", "close");
      return;
    }

    const folderId = ImportUrlUtils.folderId();
    if (folderId) {
      void onImport(file, folderId, true);
      return;
    }

    setStage({ name: "folder", file });
  }

  async function onImport(file: DocSpaceItem, folderId: string, cover = false): Promise<void> {
    if (cover)
      setStage({ name: "finishing" });
    try {
      const requestToken = file.requestTokens?.[0]?.requestToken || undefined;
      const result = await usePluginStore.getState().importFile(session.locations.importUrl, {
        fileId: String(file.id),
        filename: file.title,
        viewUrl: file.viewUrl,
        requestToken,
        folderId,
      });
      if (result.ok) {
        const count = result.count ?? 1;
        const message =
          count === 1
            ? translate("import.success.one")
            : translate("import.success.many", { count });
        finish(message, "success", "imported");
      } else {
        finish(importErrorMessage(result), "error", "close");
      }
    } catch (err) {
      finish(`${translate("import.failed")} ${FuncUtils.errorMessage(err)}`, "error", "close");
    }
  }

  if (stage.name === "folder") {
    return (
      <FolderPicker
        file={stage.file}
        foldersUrl={session.locations.foldersUrl}
        onConfirm={(file, folderId) => onImport(file, folderId)}
        onCancel={close}
      />
    );
  }

  return (
    <FilePicker
      config={session}
      finishing={stage.name === "finishing"}
      onSelect={onFileSelect}
      onClose={close}
    />
  );
}
