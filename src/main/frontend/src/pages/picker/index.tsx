import { useState } from "preact/hooks";

import { FilePicker, FolderPicker } from "@features/import";
import type { DocSpaceItem } from "@features/docspace/types";
import { ImportUrlUtils } from "@features/import/utils/url";

import { useNotificationStore } from "@store/notification";
import { usePageStore } from "@store/page";
import { usePluginStore } from "@store/plugin";

import { EventUtils } from "@utils/event";
import { FileUtils } from "@utils/file";
import { FuncUtils } from "@utils/func";

import manifest from "@manifest";

import type { Stage } from "./stage";

const close = () => EventUtils.send(manifest.events.frontend.filePicker, "close");

export function PickerPage() {
  const config = usePageStore((s) => s.config);
  const [stage, setStage] = useState<Stage>({ name: "picker" });

  if (!config) return null;
  const session = config;

  function finish(message: string, type: "success" | "error", action: "close" | "imported"): void {
    useNotificationStore.getState().notify(message, type);
    EventUtils.send(manifest.events.frontend.filePicker, action);
  }

  function onFileSelect(items: DocSpaceItem[]): void {
    const file = items.find(FileUtils.isImportable);
    if (!file) {
      finish("Only Excel (.xlsx, .xls) and CSV files can be imported.", "error", "close");
      return;
    }

    const folderId = ImportUrlUtils.folderId();
    if (folderId) {
      void onImport(file, folderId);
      return;
    }
    setStage({ name: "folder", file });
  }

  async function onImport(file: DocSpaceItem, folderId: string): Promise<void> {
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
        finish(`Imported as FineBI dataset "${result.datasetName ?? file.title}"`, "success", "imported");
      } else {
        finish(result.error ?? "Import failed.", "error", "close");
      }
    } catch (err) {
      finish(`Import failed: ${FuncUtils.errorMessage(err)}`, "error", "close");
    }
  }

  if (stage.name === "folder") {
    return (
      <FolderPicker
        file={stage.file}
        foldersUrl={session.locations.foldersUrl}
        onConfirm={onImport}
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
