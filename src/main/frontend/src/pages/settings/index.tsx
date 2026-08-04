import { useState } from "preact/hooks";
import { DocSpaceStateEvents } from "@/types/events";

import { FormError, GenericButton, LoaderButton, Hint, Field } from "@components";
import { AuthenticationContainer } from "@features/authentication/components/Container";
import { KnownTenantList } from "@features/authentication/components/KnownTenantList";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";

import { useEventPublisher } from "@hooks/useEventPublisher";

import { useDocSpaceStore } from "@store/docspace";
import { usePluginStore } from "@store/plugin";

import type { PluginCoreKnownTenant } from "@api/plugin";

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
  const knownTenants = session.knownTenants;
  const signedInTenantUrl = session.status.signedInTenantUrl;
  const signedIn = Boolean(session.credentials.email && signedInTenantUrl);

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
      const portal = UrlUtils.normalize(signedInTenantUrl) || tenantUrl;
      if (portal)
        await useDocSpaceStore.getState().logout(portal);
      else
        useDocSpaceStore.getState().reset();
      await usePluginStore.getState().logout(session.actions.logout);
      await usePluginStore.getState().load();
      publish(DocSpaceStateEvents.reset, { teardown: true });
    });
  }

  function changeTenant(): void {
    void run(async () => {
      const portal = UrlUtils.normalize(signedInTenantUrl) || tenantUrl;
      if (portal)
        await useDocSpaceStore.getState().logout(portal);
      else
        useDocSpaceStore.getState().reset();
      await usePluginStore.getState().clearTenant(session.actions.changeTenant);
      await usePluginStore.getState().load();
      publish(DocSpaceStateEvents.reset, { teardown: true });
    });
  }

  function selectTenant(tenant: PluginCoreKnownTenant): void {
    if (!session.actions.selectTenant) return;
    if (UrlUtils.normalize(tenant.url) === UrlUtils.normalize(signedInTenantUrl)) return;

    void run(async () => {
      const portal = UrlUtils.normalize(signedInTenantUrl) || tenantUrl;
      if (portal)
        await useDocSpaceStore.getState().logout(portal);
      else
        useDocSpaceStore.getState().reset();
      await usePluginStore.getState().manageTenant(session.actions.selectTenant, tenant.url);
      await usePluginStore.getState().load();
    });
  }

  function removeTenant(tenant: PluginCoreKnownTenant): void {
    if (!session.actions.removeTenant) return;
    void run(async () => {
      const removingSignedIn =
        UrlUtils.normalize(tenant.url) === UrlUtils.normalize(signedInTenantUrl);
      if (removingSignedIn || tenant.active || tenant.url === tenantUrl) {
        const portal = UrlUtils.normalize(signedInTenantUrl) || tenantUrl;
        if (portal)
          await useDocSpaceStore.getState().logout(portal);
        else
          useDocSpaceStore.getState().reset();
      }

      await usePluginStore.getState().manageTenant(session.actions.removeTenant, tenant.url);
      await usePluginStore.getState().load();
      if (removingSignedIn || tenant.active)
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
        <KnownTenantList
          tenants={knownTenants}
          disabled={loading}
          signedInTenantUrl={signedInTenantUrl}
          onSelect={selectTenant}
          onRemove={removeTenant}
        />
        {signedIn && (
          <>
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
          </>
        )}
        {session.status.canAddTenant ? (
          <GenericButton
            className="onlyoffice-button--secondary"
            disabled={loading}
            onClick={changeTenant}
          >
            {translate("auth.change.tenant")}
          </GenericButton>
        ) : (
          <Hint>{translate("settings.tenants.limit")}</Hint>
        )}
      </div>
    </AuthenticationContainer>
  );
}
