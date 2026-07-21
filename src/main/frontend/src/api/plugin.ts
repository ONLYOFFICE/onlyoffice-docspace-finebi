import manifest from "@manifest";
import { UrlUtils } from "@utils/url";

interface PluginResult {
  ok?: boolean;
  error?: string;
}

interface ImportResult {
  ok: boolean;
  datasetName?: string;
  error?: string;
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

export class PluginClient {
  // TODO: Proper fields type (as a body)
  async login(action: string, fields: Record<string, string>): Promise<void> {
    const body = new URLSearchParams();
    for (const key of Object.keys(fields)) body.set(key, fields[key]);

    const response = await fetch(action, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: body.toString(),
    });

    const data = (await response.json()) as PluginResult;
    if (!response.ok || !data.ok)
      throw new Error(`Persistence: ${data.error || "Could not save login"}`);
  }

  async clearTenant(action: string): Promise<void> {
    const response = await fetch(action, {
      method: "POST",
      headers: { Accept: "application/json" },
    });

    const data = (await response.json()) as PluginResult;
    if (!response.ok || !data.ok)
      throw new Error(
        `Clearing tenant: ${data.error || "Could not reset tenant"}`,
      );
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
    const query = new URLSearchParams({
      fileId: params.fileId,
      filename: params.filename,
    });

    if (params.viewUrl) query.set("viewUrl", params.viewUrl);
    if (params.requestToken) query.set("requestToken", params.requestToken);
    if (params.folderId) query.set("folderId", params.folderId);

    const resp = await fetch(`${importUrl}?${query.toString()}`, {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });

    if (!resp.ok)
      return {
        ok: false,
        error: `Server error ${resp.status}`,
      };

    return resp.json() as Promise<ImportResult>;
  }

  async getFolders(foldersUrl: string): Promise<FoldersResult> {
    const resp = await fetch(foldersUrl, { credentials: "same-origin" });
    if (!resp.ok) return { ok: false, error: `Server error ${resp.status}` };

    return resp.json() as Promise<FoldersResult>;
  }
}

export type PluginCoreAuthenticationMode = "setup" | "admin" | "user"; // old setup, admin-login, user-login

export type PluginCoreNavigationMode = { mode: "navigation" }; // old nav mode

export type PluginCorePageMode =
  | "docspace" // old embed mode
  | PluginCoreAuthenticationMode
  | "settings" // old admin-settings mode
  | "notconfigured" // old not-configured mode
  | "unauthorized" // old access-denied mode
  | "logout";

type PluginCoreUserCredentials = {
  email: string; // old docSpaceEmail
  hash: string; // old docSpaceHash
}

type PluginCoreUserStatus = {
  loginStored: boolean; // old hasLogin
  isAdmin: boolean; // old isAdmin
}

type PluginCoreLocations = {
  hostOrigin: string; // old finebiOrigin
  pluginUrl: string; // old pageUrl
  loginUrl: string; // old setupLink
  logoutUrl: string; // old logoutLink
  importUrl: string; // old importUrl
  foldersUrl: string; // old foldersUrl
  webhookRegistrationUrl: string; // old webhookRegisterUrl
  eventStreamUrl: string; // old syncEventsUrl
}

type PluginCoreTenantConfiguration = {
  docSpaceUrl: string; // old docSpaceUrl
  sdkVersion: string; // old sdkVersion
}

type PluginCoreServerActions = {
  submit: string; // old submitAction
  reset: string; // old resetAction
  logout: string; // old logoutAction
}

type PluginCoreServerTextResponse = {
  message: string; // old message
  error: string; // old error
}

// Served by the server side of the plugin (injected into the templates)
export interface PluginCoreServerConfiguration {
  mode: PluginCorePageMode;
  locations: PluginCoreLocations;
  credentials: PluginCoreUserCredentials;
  status: PluginCoreUserStatus;
  tenant: PluginCoreTenantConfiguration;
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
      },
      tenant: {
        docSpaceUrl: this.toString(tenant.docSpaceUrl),
        sdkVersion: this.toString(tenant.sdkVersion) || manifest.sdkVersion,
      },
      actions: {
        submit: this.toString(actions.submit),
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
