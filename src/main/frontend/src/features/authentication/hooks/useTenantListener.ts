import { useEffect } from "preact/hooks";

import { UrlUtils } from "@utils/url";
import { useDocSpaceStore } from "@store/docspace";
import { usePageStore } from "@store/page";
import manifest from "@manifest";

function parseEventType(data: string): string {
  try {
    return (JSON.parse(data) as { type?: string }).type ?? "";
  } catch {
    return "";
  }
}

export function useTenantListener(): void {
  useEffect(() => {
    const session = usePageStore.getState().config;
    if (!session?.locations.eventStreamUrl) return;

    const docSpaceUrl = UrlUtils.normalize(session.tenant.docSpaceUrl);
    const eventSource = new EventSource(session.locations.eventStreamUrl);

    eventSource.onmessage = (event) => {
      if (parseEventType(event.data) !== manifest.events.backend.tenantReset) return;
      eventSource.close();
      void useDocSpaceStore
        .getState()
        .logout(docSpaceUrl)
        .finally(() => void usePageStore.getState().load());
    };

    return () => eventSource.close();
  }, []);
}
