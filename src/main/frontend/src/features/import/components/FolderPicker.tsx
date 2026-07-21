import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import type { FolderEntry } from "@api/plugin";
import { Address, DialogButton, FormError, Lead, Spinner } from "@components";
import type { DocSpaceItem } from "@features/docspace/types";
import { FolderOverlay } from "./FolderOverlay";
import { FolderSelector } from "./FolderSelector";

import "./picker.css";

interface FolderPickerProps {
  file: DocSpaceItem;
  foldersUrl: string;
  onConfirm: (file: DocSpaceItem, folderId: string) => void;
  onCancel: () => void;
}

export function FolderPicker({ file, foldersUrl, onConfirm, onCancel }: FolderPickerProps) {
  const [folders, setFolders] = useState<FolderEntry[] | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState("");

  useEffect(() => {
    usePluginStore.getState().getFolders(foldersUrl)
      .then((data) => {
        if (!data.ok || !data.folders?.length) {
          setFetchError(data.error ?? "No folders found in FineBI Public Data.");
          return;
        }

        setFolders(data.folders);
        setSelectedId(data.folders[0].id);
      })
      .catch((err: unknown) => {
        setFetchError(err instanceof Error ? err.message : "Could not load folders.");
      });
  }, [foldersUrl]);

  return (
    <FolderOverlay onClose={onCancel}>
      <div className="onlyoffice-folder-dialog">
        <Lead>Import to FineBI</Lead>
        <Address>{`"${file.title}"`}</Address>

        {!folders && !fetchError && (
          <div className="onlyoffice-folder-dialog__loading">
            <Spinner />
          </div>
        )}

        {fetchError && <FormError message={fetchError} />}

        {folders && (
          <FolderSelector folders={folders} value={selectedId} onChange={setSelectedId} />
        )}

        <div className="onlyoffice-folder-dialog__actions">
          <DialogButton onClick={onCancel}>Cancel</DialogButton>
          <DialogButton
            primary
            disabled={!folders || !selectedId}
            onClick={() => { if (folders && selectedId) onConfirm(file, selectedId); }}
          >
            Import
          </DialogButton>
        </div>
      </div>
    </FolderOverlay>
  );
}
