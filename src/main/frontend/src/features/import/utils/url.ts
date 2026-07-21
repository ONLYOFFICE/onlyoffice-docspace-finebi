import { UrlUtils } from "@utils/url";
import manifest from "@manifest";

export const ImportUrlUtils = Object.freeze({
  /** True when this page was opened as the shell import iframe (?picker=1). */
  isPicker(): boolean {
    return new URLSearchParams(window.location.search).has("picker");
  },

  /** FineBI folder from the shell hash, forwarded as ?folderId=… when set. */
  folderId(): string {
    return new URLSearchParams(window.location.search).get("folderId") ?? "";
  },

  /** Plugin docspace page in picker mode (optional FineBI folder context). */
  picker(folderId = UrlUtils.extractFolder()): string {
    const query = new URLSearchParams({ picker: "1" });
    if (folderId) query.set("folderId", folderId);
    return `${UrlUtils.hostBaseUrl()}/url${manifest.aliases.main.from}?${query}`;
  },
});
