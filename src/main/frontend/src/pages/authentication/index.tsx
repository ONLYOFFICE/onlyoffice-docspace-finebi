import {
  FormError,
  Field,
  GenericButton,
  Hint,
  LoaderButton,
  PasswordField,
} from "@components";
import { AuthenticationContainer } from "@/features/authentication/components/Container";
import { useAuthentication } from "@/features/authentication/hooks/useAuthentication";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";
import type { PluginCoreAuthenticationMode } from "@api/plugin";
import { usePluginStore } from "@store/plugin";
import { UrlUtils } from "@utils/url";

import { useTranslation } from "@i18n";

import { authModeText } from "./mode";

export function AuthenticationPage() {
  const translate = useTranslation();
  const config = usePluginStore((s) => s.config);
  const authentication = useAuthentication();
  useTenantListener();

  if (!config) return null;

  const mode = authModeText(config.mode as PluginCoreAuthenticationMode);
  const isLogin = config.mode !== "setup";
  const address =
    isLogin && config.tenant.docSpaceUrl
      ? UrlUtils.extractHost(config.tenant.docSpaceUrl)
      : undefined;

  return (
    <AuthenticationContainer lead={mode.lead} address={address}>
      <form
        className="onlyoffice-authentication-container__card"
        onSubmit={authentication.submit}
      >
        <FormError message={authentication.error} />
        {authentication.isSetup && (
          <Field
            id="docspaceUrl"
            label={translate("auth.url.label")}
            type="url"
            required
            value={authentication.fields.url}
            onInput={(e) =>
              authentication.setField("url", e.currentTarget.value)
            }
            placeholder={translate("auth.url.placeholder")}
          />
        )}
        <Field
          id="email"
          label={mode.emailLabel}
          type="email"
          required
          value={authentication.fields.email}
          onInput={(e) =>
            authentication.setField("email", e.currentTarget.value)
          }
          autocomplete="username"
          placeholder={mode.emailPlaceholder}
        />
        <PasswordField
          id="password"
          label={mode.passwordLabel}
          required
          value={authentication.fields.password}
          onInput={(e) =>
            authentication.setField("password", e.currentTarget.value)
          }
          autocomplete="current-password"
          placeholder={mode.passwordPlaceholder}
        />
        <LoaderButton type="submit" loading={authentication.loading}>
          {mode.submit}
        </LoaderButton>
        {config.mode === "admin" && config.actions.reset && (
          <GenericButton
            className="onlyoffice-button--secondary"
            disabled={authentication.loading}
            onClick={authentication.changeTenant}
          >
            {translate("auth.change.tenant")}
          </GenericButton>
        )}
        {authentication.isSetup && config.locations.hostOrigin && (
          <Hint>
            {translate("auth.csp.hint.before")}{" "}
            <strong>{config.locations.hostOrigin}</strong>{" "}
            {translate("auth.csp.hint.after")}
          </Hint>
        )}
      </form>
    </AuthenticationContainer>
  );
}
