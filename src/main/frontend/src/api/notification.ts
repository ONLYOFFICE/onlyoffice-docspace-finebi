import { IHostGlobal } from "@/types/host";

export class HostNotification {
  constructor(private readonly BI: IHostGlobal) {
    this.BI = BI;
  }

  notify(message: string): void {
    if (this.BI?.Msg?.toast) {
      this.BI.Msg.toast(message);
      return;
    }
    window.alert(message);
  }
}
