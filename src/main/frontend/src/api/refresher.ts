interface IBroadcastService {
  broadcastAllWidgets2Refresh(forceRefresh: boolean): void;
}

interface IBroadcastServiceFactory {
  getBroadcastHelper(): IBroadcastService;
}

export interface IWidgetRefresher {
  refreshAll(): boolean;
}

export class HostWidgetRefresher implements IWidgetRefresher {
  refreshAll(): boolean {
    try {
      const BI = (
        window as {
          BI?: {
            TemplateHelperService?: {
              getMap?(): Map<unknown, IBroadcastServiceFactory>;
            };
          };
        }
      ).BI;

      const map = BI?.TemplateHelperService?.getMap?.();
      if (!map) return false;

      let refreshed = false;
      for (const factory of map.values()) {
        if (factory && typeof factory.getBroadcastHelper === "function") {
          factory.getBroadcastHelper().broadcastAllWidgets2Refresh(true);
          refreshed = true;
        }
      }

      return refreshed;
    } catch {
      return false;
    }
  }
}
