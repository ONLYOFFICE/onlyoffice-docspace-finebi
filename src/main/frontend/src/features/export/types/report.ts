export interface ExportRequest {
  reportId: string;
  reportName: string;
  operationId: string;
  entryType: string;
  sessionId: string;
  data: Record<string, unknown>;
}

export interface UploadResult {
  ok: boolean;
  filename?: string;
  error?: string;
}
