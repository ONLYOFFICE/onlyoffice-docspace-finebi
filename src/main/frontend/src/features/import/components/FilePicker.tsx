import { useEffect, useState } from "preact/hooks";

import { Dialog } from "@components";
import type { DocSpaceItem } from "@features/docspace/types";
import { useDocSpaceStore } from "@store/docspace";

interface FilePickerProps {
  docSpaceUrl: string;
  onSelect: (items: DocSpaceItem[]) => void;
  onClose: () => void;
}

export function FilePicker({ docSpaceUrl, onSelect, onClose }: FilePickerProps) {
  const [loading, setLoading] = useState(true);
  const frameId = useDocSpaceStore((s) => s.pickerFrameId());

  useEffect(() => {
    let cancelled = false;

    void useDocSpaceStore.getState().launchFileSelector(docSpaceUrl, {
      onAppReady: () => { if (!cancelled) setLoading(false); },
      onSelectCallback: (item) => {
        const items = Array.isArray(item) ? item : (item ? [item] : []);
        onSelect(items);
      },
      onCloseCallback: onClose,
      onAppError: () => onClose(),
    }).catch(() => {
      if (!cancelled) onClose();
    });

    return () => {
      cancelled = true;
      const element = document.getElementById(frameId);
      if (element)
        element.innerHTML = "";
    };
  }, [docSpaceUrl, frameId]);

  return (
    <Dialog title='Select a file, then click "Import to FineBI"' onClose={onClose}>
      {loading && <div class="onlyoffice-dialog__loader">Loading file picker…</div>}
      <div id={frameId} />
    </Dialog>
  );
}
