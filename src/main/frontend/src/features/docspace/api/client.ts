import type { PluginCoreServerConfiguration } from "@api/plugin";
import type {
  DocSpaceFrame,
  DocSpaceSdk,
  FileSelectorOptions,
  LoginResult,
} from "@features/docspace/types";
import { FuncUtils } from "@utils/func";
import { UrlUtils } from "@utils/url";
import docspace from "@config/docspace.json";
import manifest from "@manifest";

const SDK_MISSING = "DocSpace SDK is not loaded";
const LOGOUT_MS = 4_000;
const LOGIN_BACKOFF_MS = [0, 100, 500] as const;

function cspError(err: unknown): Error {
  const text = String(err);
  return new Error(
    /\(CSP\)/.test(text)
      ? "DocSpace CSP blocked this FineBI origin. Add it under Developer Tools → JavaScript SDK."
      : text,
  );
}

export class DocSpaceClient {
  private cached: { url: string; frame: Promise<DocSpaceFrame> } | null = null;

  private sdkSrc(docSpaceUrl: string): string {
    return `${UrlUtils.normalize(docSpaceUrl)}/static/scripts/sdk/${manifest.sdkVersion}/api.js`;
  }

  private waitForSdk(script: HTMLElement, timeoutMs = 12_000): Promise<DocSpaceSdk> {
    return new Promise((resolve, reject) => {
      let settled = false;
      const finish = (fn: () => void) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timer);
        fn();
      };
      const fail = () => finish(() => reject(new Error(SDK_MISSING)));
      const succeed = (sdk: DocSpaceSdk) => finish(() => resolve(sdk));

      const check = () => {
        const sdk = window.DocSpace?.SDK;
        if (sdk) succeed(sdk);
      };

      script.addEventListener("load", () => {
        check();
        if (!window.DocSpace?.SDK) fail();
      }, { once: true });
      script.addEventListener("error", fail, { once: true });
      const timer = window.setTimeout(fail, timeoutMs);
      check();
    });
  }

  private injectSdk(docSpaceUrl: string): Promise<DocSpaceSdk> {
    const script = document.createElement("script");
    script.id = docspace.sdkScriptId;
    script.src = this.sdkSrc(docSpaceUrl);
    script.async = true;
    (document.head ?? document.documentElement).appendChild(script);
    return this.waitForSdk(script);
  }

  /**
   * Prefer the server-preloaded {@code #ds-sdk-script}. When it is missing
   * (first-time setup, or SPA after tenant reset), load api.js from the
   * DocSpace URL the caller already has — no full page reload required.
   */
  private ensureSdk(docSpaceUrl?: string): Promise<DocSpaceSdk> {
    if (window.DocSpace?.SDK) return Promise.resolve(window.DocSpace.SDK);

    const existing = document.getElementById(docspace.sdkScriptId);
    if (existing) {
      return this.waitForSdk(existing).catch((err) => {
        if (!docSpaceUrl) throw err;
        existing.remove();
        return this.injectSdk(docSpaceUrl);
      });
    }

    if (!docSpaceUrl) return Promise.reject(new Error(SDK_MISSING));
    return this.injectSdk(docSpaceUrl);
  }

  private async restoreSession(
    frame: DocSpaceFrame,
    email: string,
    hash: string,
  ): Promise<void> {
    const user = await frame.getUserInfo();
    if (user?.id) return;
    if (!email || !hash) {
      throw new Error("DocSpace session expired. Sign out and sign in again.");
    }

    await this.login(frame, email, hash);
  }

  private async openSystem(url: string): Promise<DocSpaceFrame> {
    const sdk = await this.ensureSdk(url);
    if (typeof sdk.initSystem !== "function") throw new Error(SDK_MISSING);

    const id = this.systemFrameId();
    return new Promise((resolve, reject) => {
      sdk.initSystem({
        src: url,
        frameId: id,
        width: "0px",
        height: "0px",
        checkCSP: false,
        events: {
          onAppReady: () => {
            const frame = sdk.frames[id];
            if (frame) resolve(frame);
            else
              reject(
                new Error("DocSpace SDK is not ready. Refresh and try again."),
              );
          },
          onAppError: (err) => reject(cspError(err)),
        },
      });
    });
  }

  private async login(
    frame: DocSpaceFrame,
    email: string,
    hash: string,
  ): Promise<LoginResult> {
    for (const [i, wait] of LOGIN_BACKOFF_MS.entries()) {
      if (wait) await FuncUtils.sleep(wait);
      try {
        const result = await frame.login(email, hash);
        if (result?.url) return result;
      } catch (err) {
        if (i === LOGIN_BACKOFF_MS.length - 1) throw err;
      }
    }
    throw new Error("DocSpace login failed. Sign out and sign in again.");
  }

  frameId(): string {
    return docspace.frameId;
  }

  systemFrameId(): string {
    return docspace.systemFrameId;
  }

  pickerFrameId(): string {
    return docspace.pickerFrameId;
  }

  ensureFrame(url: string): Promise<DocSpaceFrame> {
    const normalised = UrlUtils.normalize(url);
    if (!this.cached || this.cached.url !== normalised) {
      const frame = this.openSystem(normalised);
      this.cached = { url: normalised, frame };
      frame.catch(() => {
        if (this.cached?.frame === frame) this.cached = null;
      });
    }

    return this.cached.frame;
  }

  async connect(config: PluginCoreServerConfiguration): Promise<string> {
    const url = UrlUtils.normalize(config.tenant.docSpaceUrl);
    if (!url) throw new Error("DocSpace is not configured.");

    const frame = await this.ensureFrame(url);
    const { email, hash } = config.credentials;
    await this.restoreSession(frame, email, hash);
    return url;
  }

  async logout(url: string): Promise<void> {
    try {
      const frame = this.cached?.url === UrlUtils.normalize(url)
        ? await this.cached.frame
        : null;
      if (frame) {
        await Promise.race([
          frame.logout(),
          FuncUtils.sleep(LOGOUT_MS).then(() => {
            throw new Error("SDK logout timed out");
          }),
        ]);
      }
    } catch {
      // ignore — see contract above
    } finally {
      this.reset();
    }
  }

  /** Drop the cached system frame and clear its mount node. */
  reset(): void {
    this.cached = null;
    this.destroyById(this.systemFrameId());
    this.destroyById(this.frameId());
  }

  /** Tear down the visible manager iframe and clear its mount node. */
  destroyManager(): void {
    this.destroyById(this.frameId());
  }

  private destroyById(id: string): void {
    const sdk = window.DocSpace?.SDK;
    const frame = sdk?.frames[id];
    try {
      frame?.destroyFrame();
    } catch {
      // best-effort — DOM wipe below still runs
    }

    const el = document.getElementById(id);
    if (el) el.innerHTML = "";
  }

  async launchManager(url: string): Promise<void> {
    const sdk = await this.ensureSdk(url);
    if (!sdk.initManager) throw new Error(SDK_MISSING);

    return new Promise((resolve, reject) => {
      sdk.initManager({
        frameId: this.frameId(),
        src: url,
        mode: "manager",
        width: "100%",
        height: "100%",
        showHeader: false,
        checkCSP: false,
        events: {
          onAppReady: () => resolve(),
          onAppError: (err) => reject(cspError(err)),
        },
      });
    });
  }

  async launchFileSelector(
    url: string,
    events: NonNullable<FileSelectorOptions["events"]>,
  ): Promise<void> {
    const sdk = await this.ensureSdk(url);
    if (!sdk.initFileSelector) throw new Error(SDK_MISSING);

    sdk.initFileSelector({
      frameId: this.pickerFrameId(),
      src: url,
      width: "100%",
      height: "100%",
      checkCSP: false,
      acceptButtonLabel: "Import to FineBI",
      events,
    });
  }
}
