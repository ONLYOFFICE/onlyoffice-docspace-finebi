import { useEffect, useState } from "preact/hooks";

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

  useEffect(() => {
    if (!pickerUrl)
      return;

    const onMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin) return;
      const type = (event.data as { type?: string } | null)?.type;
      if (type === manifest.events.frontend.filePicker) {
        setPickerUrl(null);
        setLoaded(false);
      }
    };

    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, [pickerUrl]);

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
