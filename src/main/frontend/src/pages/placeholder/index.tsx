import { useEffect } from "preact/hooks";

import { PlaceholderContainer } from "@components";
import { ImportUrlUtils } from "@features/import/utils/url";
import { usePageStore } from "@store/page";
import { EventUtils } from "@utils/event";
import manifest from "@manifest";

export function PlaceholderPage() {
  const config = usePageStore((s) => s.config);

  const picker = ImportUrlUtils.isPicker();

  useEffect(() => {
    if (picker) EventUtils.send(manifest.events.frontend.filePicker, "close");
  }, [picker]);

  if (picker) return null;

  if (!config) {
    return (
      <PlaceholderContainer
        header="Couldn't load DocSpace"
        message="Something went wrong. Please reload the page to try again."
      />
    );
  }

  const header =
    config.mode === "unauthorized" ? "Access denied" : "Please wait";
  return (
    <PlaceholderContainer header={header} message={config.response.message} />
  );
}
