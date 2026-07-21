import { useNotificationStore } from "@store/notification";
import { useReportStore } from "@store/report";
import { ExportReportClient } from "@features/export/api/report";
import type { ExportConfig } from "@features/export/types/config";
import { ExportReportUtils } from "@features/export/utils/report";
import { FuncUtils } from "@utils/func";

export class ExportClient {
  private readonly report = new ExportReportClient();

  async run(config: ExportConfig | null): Promise<string> {
    const helper = useReportStore.getState().find();
    if (!helper) {
      throw new Error("Could not find dashboard. Make sure a dashboard is fully open.");
    }

    const fields = ExportReportUtils.fields(helper);
    const operationId = ExportReportUtils.operationId();
    const data = await ExportReportUtils.data(helper);

    await this.report.excel({ ...fields, operationId, data });
    const result = await this.report.upload(operationId, fields.reportName);

    const roomTitle = config?.roomTitle ?? "My documents";
    return `Uploaded ${result.filename ?? "export.xlsx"} to DocSpace (${roomTitle}).`;
  }

  notify(message: string): void {
    useNotificationStore.getState().notify(message);
  }

  notifyError(err: unknown): void {
    useNotificationStore.getState().notify(FuncUtils.errorMessage(err), "error");
  }
}
