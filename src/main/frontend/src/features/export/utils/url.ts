import { UrlUtils } from "@utils/url";
import manifest from "@manifest";

const EXCEL_PATH = "/v5/design/report/data/global/export/excel";

export const ExportUrlUtils = Object.freeze({
  excel(reportId: string, entryType: string, operationId: string): string {
    const query = new URLSearchParams({ reportId, entryType, operationId });
    return `${UrlUtils.hostBaseUrl()}${EXCEL_PATH}?${query}`;
  },

  upload(): string {
    return UrlUtils.pluginUrl(manifest.endpoints.exportTrigger);
  },

  config(): string {
    return UrlUtils.pluginUrl(manifest.endpoints.exportConfig);
  },
});
