import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import type { FolderEntry } from "@api/plugin";
import { Address, DialogButton, FormError, Lead, Spinner } from "@components";
import type { DocSpaceItem } from "@features/docspace/types";
import { useTranslation } from "@i18n";
import { FolderOverlay } from "./FolderOverlay";
import { FolderSelector } from "./FolderSelector";

import "./picker.css";

interface FolderPickerProps {
  file: DocSpaceItem;
  foldersUrl: string;
  onConfirm: (file: DocSpaceItem, folderId: string) => void | Promise<void>;
  onCancel: () => void;
}

export function FolderPicker({
  file, foldersUrl, onConfirm, onCancel }: FolderPickerProps) {
  const translate = useTranslation();
  const [folders, setFolders] = useState<FolderEntry[] | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState("");
  const [importing, setImporting] = useState(false);

  useEffect(() => {
    usePluginStore.getState().getFolders(foldersUrl)
      .then((data) => {
        if (!data.ok || !data.folders?.length) {
          setFetchError(data.error ?? translate("import.folder.empty"));
          return;
        }

        setFolders(data.folders);
        setSelectedId(data.folders[0].id);
      })
      .catch((err: unknown) => {
        setFetchError(err instanceof Error ? err.message : translate("import.folder.load.failed"));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [foldersUrl]);

  async function handleImport(): Promise<void> {
    if (!folders || !selectedId || importing) return;
    setImporting(true);
    try {
      await onConfirm(file, selectedId);
    } catch {
      setImporting(false);
    }
  }

  return (
    <FolderOverlay onClose={importing ? undefined : onCancel}>
      <div className="onlyoffice-folder-dialog">
        <Lead>{translate("import.folder.title")}</Lead>
        <Address>{`"${file.title}"`}</Address>

        {!folders && !fetchError && (
          <div className="onlyoffice-folder-dialog__loading">
            <Spinner />
          </div>
        )}

        {fetchError && <FormError message={fetchError} />}

        {folders && (
          <FolderSelector
            folders={folders}
            value={selectedId}
            onChange={setSelectedId}
            disabled={importing}
          />
        )}

        <div className="onlyoffice-folder-dialog__actions">
          <DialogButton onClick={onCancel} disabled={importing}>
            {translate("import.folder.cancel")}
          </DialogButton>
          <DialogButton
            primary
            disabled={!folders || !selectedId || importing}
            onClick={() => void handleImport()}
          >
            {importing ? translate("import.folder.importing") : translate("import.folder.confirm")}
          </DialogButton>
        </div>
      </div>
    </FolderOverlay>
  );
}
