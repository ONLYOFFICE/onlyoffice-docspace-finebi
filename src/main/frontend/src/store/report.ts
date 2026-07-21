import { create } from "zustand";
import { ReportTemplateHelperService } from "@api/report";
import type { ReportTemplateHelper } from "@api/report";

interface ReportState {
  /** Resolve the templateHelper for the currently active dashboard tab. */
  find(): ReportTemplateHelper | null;
}

export const useReportStore = create<ReportState>()(() => {
  const service = new ReportTemplateHelperService();
  return {
    find: () => service.find(),
  };
});
