import { useEffect, useState } from "preact/hooks";

import { FilePicker, FolderPicker } from "@features/import";
import type { DocSpaceItem } from "@features/docspace/types";
import { ImportUrlUtils } from "@features/import/utils/url";
import { useDocSpaceStore } from "@store/docspace";
import { useNotificationStore } from "@store/notification";
import { usePageStore } from "@store/page";
import { usePluginStore } from "@store/plugin";
import { EventUtils } from "@utils/event";
import { FileUtils } from "@utils/file";
import { FuncUtils } from "@utils/func";
import manifest from "@manifest";

import { PickerShell } from "./Shell";
import type { Stage } from "./stage";

const close = () => EventUtils.send(manifest.events.frontend.filePicker, "close");

export function PickerPage() {
  const config = usePageStore((s) => s.config);
  const [stage, setStage] = useState<Stage>({ name: "connecting" });

  useEffect(() => {
    if (!config) return;
    document.title = "Import from DocSpace";

    useDocSpaceStore.getState().connect(config)
      .then(() => setStage({ name: "file" }))
      .catch((err: unknown) => {
        setStage({ name: "connect-error", message: FuncUtils.errorMessage(err) });
      });
  }, [config]);

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
    setStage({ name: "finishing", text: `Importing "${file.title}"…` });
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

  return (
    <>
      {stage.name === "connecting" && (
        <PickerShell text="Connecting to DocSpace…" onClose={close} />
      )}
      {stage.name === "connect-error" && (
        <PickerShell text={stage.message} onClose={close} />
      )}
      {stage.name === "file" && (
        <FilePicker
          docSpaceUrl={session.tenant.docSpaceUrl}
          onSelect={onFileSelect}
          onClose={close}
        />
      )}
      {stage.name === "folder" && (
        <FolderPicker
          file={stage.file}
          foldersUrl={session.locations.foldersUrl}
          onConfirm={onImport}
          onCancel={close}
        />
      )}
      {stage.name === "finishing" && (
        <PickerShell text={stage.text} onClose={close} />
      )}
    </>
  );
}
