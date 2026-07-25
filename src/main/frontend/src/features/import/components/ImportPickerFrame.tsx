import { useCallback, useEffect, useState } from "preact/hooks";

import { useEventListener } from "@hooks/useEventListener";

import { useTranslation } from "@i18n";

import manifest from "@manifest";

import "./overlay.css";

let showPicker: ((url: string) => void) | null = null;

export function openImportPickerFrame(pickerUrl: string): void {
  showPicker?.(pickerUrl);
}

export function ImportPickerFrame() {
  const translate = useTranslation();
  const [loaded, setLoaded] = useState(false);
  const [pickerUrl, setPickerUrl] = useState<string | null>(null);

  useEffect(() => {
    showPicker = (url) => {
      setPickerUrl((current) => current ?? url);
    };

    return () => { showPicker = null; };
  }, []);

  useEventListener(
    manifest.events.frontend.filePicker,
    useCallback(() => {
      setPickerUrl(null);
      setLoaded(false);
    }, []),
  );

  if (!pickerUrl)
    return null;

  return (
    <div class="onlyoffice-import-picker-frame">
      <iframe
        src={pickerUrl}
        title={translate("import.dialog")}
        className={loaded ? "is-loaded" : undefined}
        onLoad={() => setLoaded(true)}
        allowTransparency
      />
    </div>
  );
}
