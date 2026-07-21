import { HostRecord } from "@/types/host";

export interface ReportTemplateHelper {
  reportId: string;
  reportName: string;
  getReportId?(): string;
  getName?(): string;
  getSessionId?(): string;
  getEntryType?(): string | number;
  getId?(): string;
  getGlobalExportData(
    callback: (data: Record<string, unknown> | null) => void,
  ): void;
}

function _isReportTemplateHelper(
  value: unknown,
): value is ReportTemplateHelper {
  if (!value || typeof value !== "object") return false;

  const record = value as HostRecord;
  const rid = typeof record.reportId === "string" ? record.reportId.trim() : "";
  return (
    /^[0-9a-f]{32}$/i.test(rid) &&
    typeof record.getGlobalExportData === "function"
  );
}

function _toHelper(value: unknown): ReportTemplateHelper | null {
  if (_isReportTemplateHelper(value)) return value;
  const rec = value as HostRecord;
  for (const key of ["templateHelper", "_templateHelper"]) {
    if (_isReportTemplateHelper(rec[key]))
      return rec[key] as ReportTemplateHelper;
  }

  return null;
}

export class ReportTemplateHelperService {
  private getActiveTabId(): string {
    return window.location.hash.match(/[?&]activeTab=([^&\s]+)/i)?.[1] ?? "";
  }

  private normalizeId(id: string): string {
    return id.toLowerCase().replace(/-/g, "");
  }

  find(): ReportTemplateHelper | null {
    const tabId = this.getActiveTabId();
    if (!tabId) return null;

    const target = this.normalizeId(tabId);
    if (!target) 
      return null;

    const BI = (window as unknown as HostRecord).BI as HostRecord | undefined;
    const service = BI?.TemplateHelperService as HostRecord | undefined;

    if (!service || typeof service.getMap !== "function") return null;

    let map: Map<string, unknown>;
    try {
      map = (service.getMap as () => Map<string, unknown>)();
    } catch {
      return null;
    }

    if (!map?.forEach) 
      return null;

    let match: ReportTemplateHelper | null = null;
    map.forEach((value) => {
      if (match || !value || typeof value !== "object") return;

      const record = value as HostRecord;
      const entryId = (record.basePool as HostRecord | undefined)
        ?.reportEntryId;
      if (typeof entryId !== "string" || this.normalizeId(entryId) !== target)
        return;

      match = _toHelper(record);
    });

    return match;
  }
}
