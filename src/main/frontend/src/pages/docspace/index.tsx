import { useState } from "preact/hooks";

import { useTenantListener } from "@features/authentication/hooks/useTenantListener";
import { useCachedTab } from "@hooks/useCachedTab";

import { Lobby } from "./Lobby";
import { Room } from "./Room";

export function DocSpacePage() {
  const [inRoom, setInRoom] = useState(false);
  const [lobbyVisible, setLobbyVisible] = useState(true);

  useTenantListener();
  useCachedTab(() => setLobbyVisible(true));

  return (
    <>
      {inRoom && <Room />}
      {lobbyVisible && (
        <div class={inRoom ? "onlyoffice-overlay" : undefined}>
          <Lobby
            onEnter={() => {
              setInRoom(true);
              setLobbyVisible(false);
            }}
          />
        </div>
      )}
    </>
  );
}
