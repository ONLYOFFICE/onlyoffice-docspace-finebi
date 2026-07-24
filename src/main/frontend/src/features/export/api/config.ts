import { ExportUrlUtils } from "@features/export/utils/url";
import type { ExportConfig } from "@features/export/types/config";
import { translate } from "@i18n";

export async function fetchExportConfig(): Promise<ExportConfig> {
  const response = await fetch(ExportUrlUtils.config(), {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json" },
  });

  if (!response.ok)
    throw new Error(translate("client.export.config.failed"));

  return (await response.json()) as ExportConfig;
}
