import { useEffect, useState } from "preact/hooks";

import { usePluginStore } from "@store/plugin";
import { usePageStore } from "@store/page";
import { useAuthenticationStore } from "@store/authentication";
import { useDocSpaceStore } from "@store/docspace";
import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";

interface AuthFields {
  url: string;
  email: string;
  password: string;
}

export function useAuthentication() {
  const config = usePageStore((s) => s.config);
  if (!config) {
    throw new Error("Auth form requires a loaded session config");
  }

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
    useDocSpaceStore.getState().ensureFrame(tenantUrl)
      .catch((err) => setError(FuncUtils.errorMessage(err)));
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
      ? "DocSpace URL, email, and password are required."
      : "Email and password are required.";
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

      await usePageStore.getState().load();
    });
  }

  function changeTenant(): void {
    void run(async () => {
      const url = UrlUtils.normalize(session.tenant.docSpaceUrl);
      if (url) await useDocSpaceStore.getState().logout(url);
      else useDocSpaceStore.getState().reset();
      await usePluginStore.getState().clearTenant(session.actions.reset);
      await usePageStore.getState().load();
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
