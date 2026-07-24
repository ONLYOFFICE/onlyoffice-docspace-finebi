import { ExportReportClient } from "@features/export/api/report";
import type { ExportConfig } from "@features/export/types/config";
import { ExportReportUtils } from "@features/export/utils/report";

import { useNotificationStore } from "@store/notification";
import { useReportStore } from "@store/report";

import { FuncUtils } from "@utils/func";

import { translate } from "@i18n";

export class ExportClient {
  private readonly report = new ExportReportClient();

  async run(config: ExportConfig | null): Promise<string> {
    const helper = useReportStore.getState().find();
    if (!helper)
      throw new Error(translate("client.dashboard.missing"));

    const fields = ExportReportUtils.fields(helper);
    const operationId = ExportReportUtils.operationId();
    const data = await ExportReportUtils.data(helper);

    await this.report.excel({ ...fields, operationId, data });
    const result = await this.report.upload(operationId, fields.reportName);

    const roomTitle = config?.roomTitle ?? translate("client.my.documents");
    return translate("export.success", {
      filename: result.filename ?? "export.xlsx",
      room: roomTitle,
    });
  }

  notify(message: string): void {
    useNotificationStore.getState().notify(message);
  }

  notifyError(err: unknown): void {
    useNotificationStore.getState().notify(FuncUtils.errorMessage(err), "error");
  }
}
