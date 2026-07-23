import { useState } from "preact/hooks";

import { FormError, Field, GenericButton, LoaderButton } from "@components";
import { AuthenticationContainer } from "@features/authentication/components/Container";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";
import { useDocSpaceStore } from "@store/docspace";
import { usePluginStore } from "@store/plugin";
import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";

export function SettingsPage() {
  const config = usePluginStore((s) => s.config);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useTenantListener();

  if (!config) return null;

  const session = config;
  const tenantUrl = UrlUtils.normalize(session.tenant.docSpaceUrl);

  async function run(action: () => Promise<void>): Promise<void> {
    setLoading(true);
    setError("");
    try {
      await action();
    } catch (err) {
      setError(FuncUtils.errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  function logout(): void {
    void run(async () => {
      await useDocSpaceStore.getState().logout(tenantUrl);
      await usePluginStore.getState().logout(session.actions.logout);
      await usePluginStore.getState().load();
    });
  }

  function changeTenant(): void {
    void run(async () => {
      await useDocSpaceStore.getState().logout(tenantUrl);
      await usePluginStore.getState().clearTenant(session.actions.reset);
      await usePluginStore.getState().load();
    });
  }

  return (
    <AuthenticationContainer
      lead="FineBI is connected to your ONLYOFFICE DocSpace"
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
          label="Signed in as"
          type="text"
          disabled
          value={session.credentials.email}
        />
        <LoaderButton loading={loading} onClick={logout}>
          Logout
        </LoaderButton>
        <GenericButton
          className="onlyoffice-button--secondary"
          disabled={loading}
          onClick={changeTenant}
        >
          Change DocSpace
        </GenericButton>
      </div>
    </AuthenticationContainer>
  );
}
