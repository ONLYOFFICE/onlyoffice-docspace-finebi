import { ExportUrlUtils } from "@features/export/utils/url";
import type { ExportRequest, UploadResult } from "@features/export/types/report";

export class ExportReportClient {
  async excel(request: ExportRequest): Promise<void> {
    const { reportId, reportName, operationId, entryType, sessionId, data } = request;
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "application/json",
    };
    if (sessionId) headers.sessionId = sessionId;

    const response = await fetch(ExportUrlUtils.excel(reportId, entryType, operationId), {
      method: "POST",
      credentials: "same-origin",
      headers,
      body: JSON.stringify({ ...data, reportId, reportName, base64: "" }),
    });

    if (!response.ok) {
      const detail = await response.text().catch(() => "");
      throw new Error(detail || "Could not export the dashboard to Excel. Try again.");
    }
  }

  async upload(operationId: string, reportName: string): Promise<UploadResult> {
    const response = await fetch(ExportUrlUtils.upload(), {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ operationId, reportName }),
    });

    const result = (await response.json()) as UploadResult;
    if (!response.ok || !result.ok) {
      throw new Error(
        result.error ?? "Could not upload the export to DocSpace. Check DocSpace login and try again.",
      );
    }
    return result;
  }
}
