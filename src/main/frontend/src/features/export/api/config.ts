import { ExportUrlUtils } from "@features/export/utils/url";
import type { ExportConfig } from "@features/export/types/config";

export async function fetchExportConfig(): Promise<ExportConfig> {
  const response = await fetch(ExportUrlUtils.config(), {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json" },
  });

  if (!response.ok) {
    throw new Error("Could not load DocSpace export settings. Try again.");
  }

  return (await response.json()) as ExportConfig;
}
