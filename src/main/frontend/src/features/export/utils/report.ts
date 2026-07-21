import type { ReportTemplateHelper } from "@api/report";

export interface ExportReportFields {
  reportId: string;
  reportName: string;
  entryType: string;
  sessionId: string;
}

export const ExportReportUtils = Object.freeze({
  fields(th: ReportTemplateHelper): ExportReportFields {
    return {
      reportId: String(th.getReportId?.() ?? th.reportId).trim(),
      reportName: String(th.getName?.() ?? th.reportName ?? "").trim(),
      entryType: String(th.getEntryType?.() ?? "4"),
      sessionId: String(th.getSessionId?.() ?? ""),
    };
  },

  data(th: ReportTemplateHelper, ms = 15_000): Promise<Record<string, unknown>> {
    return new Promise((resolve) => {
      let done = false;
      const finish = (data: Record<string, unknown> | null) => {
        if (done) return;
        done = true;
        resolve(data ?? {});
      };
      setTimeout(() => finish(null), ms);
      try { th.getGlobalExportData(finish); }
      catch { finish(null); }
    });
  },

  operationId(): string {
    const bytes = new Uint8Array(8);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  },
});
