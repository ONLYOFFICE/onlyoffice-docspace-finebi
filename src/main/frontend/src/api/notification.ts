import { IHostGlobal } from "@/types/host";

export type NotificationLevel = "success" | "error";

export class HostNotification {
  constructor(private readonly BI: IHostGlobal) {
    this.BI = BI;
  }

  notify(message: string, level?: NotificationLevel): void {
    if (this.BI?.Msg?.toast) {
      this.BI.Msg.toast(message, { level: level ?? "normal" });
      return;
    }

    window.alert(message);
  }
}
