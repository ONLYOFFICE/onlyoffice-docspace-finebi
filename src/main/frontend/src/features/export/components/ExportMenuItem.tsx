import { useState } from "preact/hooks";
import type { FunctionComponent } from "preact";

import { ExportClient } from "@features/export/api/client";
import { fetchExportConfig } from "@features/export/api/config";
import { useLoggedIn } from "@hooks/useSessionMode";

import { useTranslation } from "@i18n";

import { ExportItem } from "./ExportItem";

export const ExportMenuItem: FunctionComponent = () => {
  const loggedIn = useLoggedIn();
  const translate = useTranslation();
  const [running, setRunning] = useState(false);

  if (!loggedIn) return null;

  const run = async (event: MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    if (running) return;

    setRunning(true);
    const client = new ExportClient();
    try {
      const config = await fetchExportConfig().catch(() => null);
      client.notify(await client.run(config));
    } catch (err) {
      client.notifyError(err);
    } finally {
      setRunning(false);
    }
  };

  return (
    <ExportItem
      label={running ? translate("export.uploading") : translate("export.menu")}
      running={running}
      onActivate={(event) => void run(event)}
    />
  );
};
