import { useState } from "preact/hooks";

import { LoaderButton, RoomIllustration } from "@components";
import { useDocSpace } from "@features/docspace/hooks/useDocSpace";
import { useDocSpaceStore } from "@store/docspace";
import { usePageStore } from "@store/page";
import { usePluginStore } from "@store/plugin";
import { UrlUtils } from "@utils/url";

export function Room() {
  const config = usePageStore((s) => s.config)!;
  const room = useDocSpace(config);
  const [resetting, setResetting] = useState(false);
  const tenantUrl = UrlUtils.normalize(config.tenant.docSpaceUrl);

  async function resetSession(): Promise<void> {
    setResetting(true);
    try {
      await useDocSpaceStore.getState().logout(tenantUrl);
    } catch {}
    try {
      await usePluginStore.getState().logout(config.actions.logout);
    } catch {}
    await usePageStore.getState().load();
  }

  if (room.status !== "error") return null;

  return (
    <div class="onlyoffice-status onlyoffice-status--error">
      <RoomIllustration />
      <h2 class="onlyoffice-status__title">Welcome to DocSpace</h2>
      <p class="onlyoffice-status__text">{room.error}</p>
      <div class="onlyoffice-status__actions">
        <LoaderButton loading={resetting} onClick={() => void resetSession()}>
          Reset session
        </LoaderButton>
      </div>
    </div>
  );
}
