import { useEffect, useState } from "preact/hooks";

import manifest from "@manifest";

import "./overlay.css";

/**
 * Imperative bridge into the mounted {@link ShellOverlay}.
 * Dataset-menu clicks call {@link openShellOverlay}; the host component
 * registers the real opener while it is mounted.
 */
let showPicker: ((url: string) => void) | null = null;

/** Open the FineBI-shell iframe that runs the ?picker=1 import flow. */
export function openShellOverlay(pickerUrl: string): void {
  showPicker?.(pickerUrl);
}

/**
 * Full-screen iframe host for import (DocSpace file → FineBI folder).
 * Mount once in navigation mode; stays idle until {@link openShellOverlay}.
 * Closes when the iframe posts {@link manifest.events.frontend.filePicker}.
 */
export function ShellOverlay() {
  const [pickerUrl, setPickerUrl] = useState<string | null>(null);

  useEffect(() => {
    showPicker = (url) => {
      setPickerUrl((current) => current ?? url);
    };
    return () => { showPicker = null; };
  }, []);

  useEffect(() => {
    if (!pickerUrl) return;

    const onMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin) return;
      const type = (event.data as { type?: string } | null)?.type;
      if (type === manifest.events.frontend.filePicker) setPickerUrl(null);
    };

    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, [pickerUrl]);

  if (!pickerUrl) return null;

  return (
    <div class="onlyoffice-shell-overlay">
      <iframe src={pickerUrl} title="Import from DocSpace" allowTransparency />
    </div>
  );
}
