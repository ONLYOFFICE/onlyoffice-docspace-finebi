import { translate } from "@i18n";
import { UrlUtils } from "@utils/url";

import manifest from "@manifest";

interface PluginResult {
  ok?: boolean;
  error?: string;
  errorCode?: string;
  errorParams?: Record<string, string | number>;
}

function toErrorMessage(data: PluginResult | null, fallbackKey: string): string {
  if (data?.errorCode) {
    const localized = translate(data.errorCode, data.errorParams);
    if (localized !== data.errorCode) return localized;
  }

  return data?.error?.trim() || translate(fallbackKey);
}

interface ImportResult {
  ok: boolean;
  count?: number;
  error?: string;
  errorCode?: string;
  errorParams?: Record<string, string | number>;
}

export interface FolderEntry {
  id: string;
  name: string;
}

export interface FoldersResult {
  ok: boolean;
  folders?: FolderEntry[];
  error?: string;
}

export interface LoginFields {
  docspaceUrl?: string;
  email: string;
  userId: string;
  hash: string;
}

export class PluginClient {
  async login(action: string, fields: LoginFields): Promise<void> {
    const response = await fetch(action, {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(fields),
    });

    const data = (await response.json().catch(() => null)) as PluginResult | null;
    if (!response.ok || !data?.ok)
      throw new Error(toErrorMessage(data, "client.save.login"));
  }

  async clearTenant(action: string): Promise<void> {
    const response = await fetch(action, {
      method: "POST",
      headers: { Accept: "application/json" },
    });

    const data = (await response.json().catch(() => null)) as PluginResult | null;
    if (!response.ok || !data?.ok)
      throw new Error(toErrorMessage(data, "client.reset.tenant"));
  }

  async manageTenant(action: string, docspaceUrl: string): Promise<void> {
    const response = await fetch(action, {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ docspaceUrl }),
    });

    const data = (await response.json().catch(() => null)) as PluginResult | null;
    if (!response.ok || !data?.ok)
      throw new Error(toErrorMessage(data, "client.tenant.manage.failed"));
  }

  async logout(action: string): Promise<void> {
    await fetch(action, {
      method: "POST",
      headers: { Accept: "application/json" },
    }).catch(() => {
      /* TODO: Some fallback? */
    });
  }

  async registerWebhook(url: string): Promise<void> {
    await fetch(url, {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    }).catch(() => {
      /* TODO: Some fallback? */
    });
  }

  async importFile(
    importUrl: string,
    params: {
      fileId: string;
      filename: string;
      viewUrl?: string;
      requestToken?: string;
      folderId?: string;
    },
  ): Promise<ImportResult> {
    const resp = await fetch(importUrl, {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(params),
    });

    const data = (await resp.json().catch(() => null)) as ImportResult | null;
    return data ?? { ok: false, error: translate("client.import.failed") };
  }

  async getFolders(foldersUrl: string): Promise<FoldersResult> {
    const resp = await fetch(foldersUrl, { credentials: "same-origin" });
    const data = (await resp.json().catch(() => null)) as FoldersResult | null;
    return data ?? { ok: false, error: translate("client.folders.failed") };
  }
}

export type PluginCoreAuthenticationMode = "setup" | "admin" | "user";

export type PluginCoreNavigationMode = { mode: "navigation" };

export type PluginCorePageMode =
  | "docspace"
  | PluginCoreAuthenticationMode
  | "settings"
  | "notconfigured"
  | "unauthorized"
  | "logout";

type PluginCoreUserCredentials = {
  email: string;
  hash: string;
}

type PluginCoreUserStatus = {
  loginStored: boolean;
  isAdmin: boolean;
  hasSavedTenants: boolean;
  canAddTenant: boolean;
  signedInTenantUrl: string;
}

export type PluginCoreKnownTenant = {
  url: string;
  email: string;
  active: boolean;
}

type PluginCoreLocations = {
  hostOrigin: string;
  pluginUrl: string;
  loginUrl: string;
  logoutUrl: string;
  importUrl: string;
  foldersUrl: string;
  webhookRegistrationUrl: string;
  eventStreamUrl: string;
}

type PluginCoreTenantConfiguration = {
  docSpaceUrl: string;
  sdkVersion: string;
}

type PluginCoreServerActions = {
  submit: string;
  changeTenant: string;
  selectTenant: string;
  removeTenant: string;
  reset: string;
  logout: string;
}

type PluginCoreServerTextResponse = {
  message: string;
  error: string;
}

// Served by the server side of the plugin (injected into the templates)
export interface PluginCoreServerConfiguration {
  mode: PluginCorePageMode;
  locations: PluginCoreLocations;
  credentials: PluginCoreUserCredentials;
  status: PluginCoreUserStatus;
  tenant: PluginCoreTenantConfiguration;
  knownTenants: PluginCoreKnownTenant[];
  actions: PluginCoreServerActions;
  response: PluginCoreServerTextResponse;
}

const PAGE_MODES: readonly PluginCorePageMode[] = [
  "docspace", "setup", "admin", "user", "settings", "notconfigured", "unauthorized", "logout",
];

/**
 * Fetches the nested per-session configuration from the plugin's /session REST
 * endpoint (SPA bootstrap) and coerces each field to a safe typed value.
 */
export class PluginCoreServer {
  /**
   * SPA bootstrap: fetch the per-session config from the /session REST endpoint.
   * The server derives the page context (admin console, logout) from the
   * request's Referer, so no context plumbing is needed here. Rejects on
   * failure — the entry point shows a reload prompt instead.
   */
  async load(): Promise<PluginCoreServerConfiguration> {
    const response = await fetch(UrlUtils.pluginUrl(manifest.endpoints.session), {
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) throw new Error(`Session request failed (HTTP ${response.status}).`);
    return this.coerce((await response.json()) as Record<string, unknown>);
  }

  private coerce(raw: Record<string, unknown>): PluginCoreServerConfiguration {
    const locations = this.toObject(raw.locations);
    const credentials = this.toObject(raw.credentials);
    const status = this.toObject(raw.status);
    const tenant = this.toObject(raw.tenant);
    const actions = this.toObject(raw.actions);
    const response = this.toObject(raw.response);

    return {
      mode: this.toMode(raw.mode),
      locations: {
        hostOrigin: this.toString(locations.hostOrigin),
        pluginUrl: this.toString(locations.pluginUrl),
        loginUrl: this.toString(locations.loginUrl),
        logoutUrl: this.toString(locations.logoutUrl),
        importUrl: this.toString(locations.importUrl),
        foldersUrl: this.toString(locations.foldersUrl),
        webhookRegistrationUrl: this.toString(locations.webhookRegistrationUrl),
        eventStreamUrl: this.toString(locations.eventStreamUrl),
      },
      credentials: {
        email: this.toString(credentials.email),
        hash: this.toString(credentials.hash),
      },
      status: {
        loginStored: this.toBoolean(status.loginStored),
        isAdmin: this.toBoolean(status.isAdmin),
        hasSavedTenants: this.toBoolean(status.hasSavedTenants),
        canAddTenant: this.toBoolean(status.canAddTenant),
        signedInTenantUrl: this.toString(status.signedInTenantUrl),
      },
      tenant: {
        docSpaceUrl: this.toString(tenant.docSpaceUrl),
        sdkVersion: this.toString(tenant.sdkVersion) || manifest.sdkVersion,
      },
      knownTenants: this.toKnownTenants(raw.knownTenants),
      actions: {
        submit: this.toString(actions.submit),
        changeTenant: this.toString(actions.changeTenant),
        selectTenant: this.toString(actions.selectTenant),
        removeTenant: this.toString(actions.removeTenant),
        reset: this.toString(actions.reset),
        logout: this.toString(actions.logout),
      },
      response: {
        message: this.toString(response.message),
        error: this.toString(response.error),
      },
    };
  }

  private toObject(value: unknown): Record<string, unknown> {
    return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
  }

  private toKnownTenants(value: unknown): PluginCoreKnownTenant[] {
    if (!Array.isArray(value)) return [];
    return value
      .map((entry) => {
        const row = this.toObject(entry);
        return {
          url: this.toString(row.url),
          email: this.toString(row.email),
          active: this.toBoolean(row.active),
        };
      })
      .filter((entry) => entry.url.length > 0);
  }

  private toMode(value: unknown): PluginCorePageMode {
    return PAGE_MODES.includes(value as PluginCorePageMode) ? (value as PluginCorePageMode) : "notconfigured";
  }

  private toString(value: unknown): string {
    return typeof value === "string" ? value : "";
  }

  private toBoolean(value: unknown): boolean {
    return value === true || value === "true";
  }
}
