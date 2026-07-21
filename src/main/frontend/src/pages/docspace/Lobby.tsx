import { useState } from "preact/hooks";

import { FormError, Field, GenericButton, LoaderButton } from "@components";
import { AuthenticationContainer } from "@features/authentication/components/Container";
import { useDocSpaceStore } from "@store/docspace";
import { usePageStore } from "@store/page";
import { usePluginStore } from "@store/plugin";
import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";

interface LobbyProps {
  onEnter: () => void;
}

export function Lobby({ onEnter }: LobbyProps) {
  const config = usePageStore((s) => s.config);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  if (!config) return null;

  const session = config;
  const tenantUrl = UrlUtils.normalize(session.tenant.docSpaceUrl);

  function logout(): void {
    setLoading(true);
    setError("");
    void (async () => {
      try {
        await useDocSpaceStore.getState().logout(tenantUrl);
        await usePluginStore.getState().logout(session.actions.logout);
        await usePageStore.getState().load();
      } catch (err) {
        setError(FuncUtils.errorMessage(err));
        setLoading(false);
      }
    })();
  }

  return (
    <AuthenticationContainer
      lead="You are signed in to ONLYOFFICE DocSpace"
      address={UrlUtils.extractHost(session.tenant.docSpaceUrl)}
    >
      <div className="onlyoffice-authentication-container__card">
        <FormError message={error} />
        <Field
          id="docspaceUrl"
          label="DocSpace Server Address"
          type="text"
          disabled
          value={session.tenant.docSpaceUrl}
        />
        <Field
          id="account"
          label="DocSpace Email"
          type="text"
          disabled
          value={session.credentials.email}
        />
        <GenericButton disabled={loading} onClick={onEnter}>
          Enter
        </GenericButton>
        <LoaderButton
          className="onlyoffice-button--secondary"
          loading={loading}
          onClick={logout}
        >
          Logout
        </LoaderButton>
      </div>
    </AuthenticationContainer>
  );
}
