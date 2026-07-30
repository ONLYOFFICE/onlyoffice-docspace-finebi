import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import { useDocSpaceStore } from "@store/docspace";
import { useAuthenticationStore } from "@store/authentication";

import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";

import { translate } from "@i18n";

interface AuthFields {
  url: string;
  email: string;
  password: string;
}

export function useAuthentication() {
  const config = usePluginStore((s) => s.config);
  if (!config)
    throw new Error(translate("auth.config.required"));

  const session = config;
  const isSetup = session.mode === "setup";
  const tenantUrl = session.tenant.docSpaceUrl;

  const [fields, setFields] = useState<AuthFields>({
    url: tenantUrl,
    email: "",
    password: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(session.response.error);

  useEffect(() => {
    if (isSetup || !tenantUrl) return;
    let cancelled = false;
    useDocSpaceStore.getState().ensureFrame(tenantUrl)
      .catch((err) => {
        if (!cancelled) setError(FuncUtils.errorMessage(err));
      });
    return () => {
      cancelled = true;
      useDocSpaceStore.getState().destroySystem();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function setField<K extends keyof AuthFields>(key: K, value: string): void {
    setFields((current) => ({ ...current, [key]: value }));
  }

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

  function validationError(): string | null {
    const url = UrlUtils.normalize(isSetup ? fields.url : tenantUrl);
    if (url && fields.email.trim() && fields.password) return null;
    return isSetup
      ? translate("auth.required.setup")
      : translate("auth.required.login");
  }

  function submit(event: Event): void {
    event.preventDefault();
    const invalid = validationError();
    if (invalid) {
      setError(invalid);
      return;
    }
    const url = UrlUtils.normalize(isSetup ? fields.url : tenantUrl);
    void run(async () => {
      const frame = await useDocSpaceStore.getState().ensureFrame(url);
      await useAuthenticationStore.getState().authenticate({
        frame,
        email: fields.email.trim(),
        password: fields.password,
        action: session.actions.submit,
        extraFields: isSetup ? { docspaceUrl: url } : {},
      });

      await usePluginStore.getState().load();
    });
  }

  function changeTenant(): void {
    void run(async () => {
      const url = UrlUtils.normalize(session.tenant.docSpaceUrl);
      if (url) await useDocSpaceStore.getState().logout(url);
      else useDocSpaceStore.getState().reset();
      await usePluginStore.getState().clearTenant(session.actions.reset);
      await usePluginStore.getState().load();
    });
  }

  return {
    isSetup,
    fields,
    setField,
    loading,
    error,
    submit,
    changeTenant,
  };
}
