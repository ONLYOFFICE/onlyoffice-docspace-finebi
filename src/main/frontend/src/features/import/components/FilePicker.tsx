import { useEffect, useState } from "preact/hooks";

import { Cross, Spinner } from "@components";
import type { DocSpaceItem } from "@features/docspace/types";

import type { PluginCoreServerConfiguration } from "@api/plugin";

import { useDocSpaceStore } from "@store/docspace";

import { FuncUtils } from "@utils/func";

import { useTranslation } from "@i18n";

import "@components/overlays/dialog.css";

interface FilePickerProps {
  config: PluginCoreServerConfiguration;
  finishing?: boolean;
  onSelect: (items: DocSpaceItem[]) => void;
  onClose: () => void;
}

export function FilePicker({
  config,
  finishing = false,
  onSelect,
  onClose,
}: FilePickerProps) {
  const translate = useTranslation();
  const [running, setRunning] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const frameId = useDocSpaceStore((s) => s.pickerFrameId());
  const docSpaceUrl = config.tenant.docSpaceUrl;
  const showOverlay = running || finishing || !!error;

  useEffect(() => {
    let cancelled = false;
    setRunning(true);
    setError(null);

    void (async () => {
      try {
        await useDocSpaceStore.getState().connect(config);
        if (cancelled) return;

        await useDocSpaceStore.getState().launchFileSelector(
          docSpaceUrl,
          {
            onAppReady: () => {
              if (!cancelled) setRunning(false);
            },
            onSelectCallback: (item) => {
              if (cancelled) return;
              const items = Array.isArray(item) ? item : (item ? [item] : []);
              onSelect(items);
            },
            onCloseCallback: () => {
              if (!cancelled) onClose();
            },
            onAppError: () => {
              if (!cancelled) onClose();
            },
          },
          () => cancelled,
        );
      } catch (err) {
        if (!cancelled) setError(FuncUtils.errorMessage(err));
      }
    })();

    return () => {
      cancelled = true;
      useDocSpaceStore.getState().reset();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="onlyoffice-dialog-overlay">
      <div
        className="onlyoffice-dialog-backdrop"
        onClick={finishing ? undefined : onClose}
      />
      <div
        className="onlyoffice-dialog"
        role="dialog"
        aria-busy={showOverlay || undefined}
        aria-label={translate("import.dialog")}
      >
        <div className="onlyoffice-dialog__header">
          <button
            type="button"
            className="onlyoffice-dialog__close"
            aria-label={translate("import.close")}
            disabled={finishing}
            onClick={onClose}
          >
            <Cross />
          </button>
        </div>
        <div className="onlyoffice-dialog__body">
          {showOverlay && (
            <div className="onlyoffice-dialog__loader">
              {error ?? <Spinner className="onlyoffice-spinner--lg" />}
            </div>
          )}
          <div id={frameId} />
        </div>
      </div>
    </div>
  );
}
