import { useState } from "preact/hooks";
import { DocSpaceStateEvents } from "@/types/events";

import { FormError, Field, GenericButton, LoaderButton } from "@components";
import { AuthenticationContainer } from "@features/authentication/components/Container";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";

import { useEventPublisher } from "@hooks/useEventPublisher";

import { useDocSpaceStore } from "@store/docspace";
import { usePluginStore } from "@store/plugin";

import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";

import { useTranslation } from "@i18n";

export function SettingsPage() {
  const translate = useTranslation();
  const config = usePluginStore((s) => s.config);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { publish } = useEventPublisher();
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
      publish(DocSpaceStateEvents.reset, { teardown: true });
    });
  }

  function changeTenant(): void {
    void run(async () => {
      await useDocSpaceStore.getState().logout(tenantUrl);
      await usePluginStore.getState().clearTenant(session.actions.changeTenant);
      await usePluginStore.getState().load();
      publish(DocSpaceStateEvents.reset, { teardown: true });
    });
  }

  function resetTenant(): void {
    void run(async () => {
      await useDocSpaceStore.getState().logout(tenantUrl);
      await usePluginStore.getState().clearTenant(session.actions.reset);
      await usePluginStore.getState().load();
      publish(DocSpaceStateEvents.reset, { teardown: true });
    });
  }

  return (
    <AuthenticationContainer
      lead={translate("settings.lead")}
      address={UrlUtils.extractHost(session.tenant.docSpaceUrl)}
    >
      <div className="onlyoffice-authentication-container__card">
        <FormError message={error} />
        <Field
          id="docspaceUrl"
          label={translate("auth.url.label")}
          type="text"
          disabled
          value={session.tenant.docSpaceUrl}
        />
        <Field
          id="account"
          label={translate("settings.signed.in.as")}
          type="text"
          disabled
          value={session.credentials.email}
        />
        <LoaderButton loading={loading} onClick={logout}>
          {translate("settings.logout")}
        </LoaderButton>
        <GenericButton
          className="onlyoffice-button--secondary"
          disabled={loading}
          onClick={changeTenant}
        >
          {translate("auth.change.tenant")}
        </GenericButton>
        <GenericButton
          className="onlyoffice-button--danger"
          disabled={loading}
          onClick={resetTenant}
        >
          {translate("settings.reset")}
        </GenericButton>
      </div>
    </AuthenticationContainer>
  );
}
