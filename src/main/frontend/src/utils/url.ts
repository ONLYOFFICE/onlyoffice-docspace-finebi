import manifest from "@manifest";

export const UrlUtils = Object.freeze({
  normalize(url: string | null | undefined): string {
    return (url || "").trim().replace(/\/+$/, "");
  },
  extractHost(url: string): string {
    try {
      return new URL(url).host || url;
    } catch {
      return url;
    }
  },
  extractFolder(): string {
    const match = /#\/config\/folder\/([0-9A-Za-z-]+)/.exec(window.location.hash);
    return match ? match[1] : "";
  },
  hostBaseUrl(): string {
    return window.Dec?.fineServletURL ?? "/webroot/decision";
  },
  pluginUrl(path: string): string {
    return `${this.hostBaseUrl()}/plugin/private/${manifest.pluginId}${path}`;
  },
});
