import { create } from "zustand";

import de from "./lang/de.json";
import en from "./lang/en.json";
import es from "./lang/es.json";
import fr from "./lang/fr.json";
import it from "./lang/it.json";
import ja from "./lang/ja.json";
import ko from "./lang/ko.json";
import pt from "./lang/pt.json";
import ru from "./lang/ru.json";
import zhCN from "./lang/zh-CN.json";
import zhTW from "./lang/zh-TW.json";

export const LOCALES = [
  "en",
  "de",
  "es",
  "fr",
  "it",
  "pt",
  "ru",
  "ja",
  "ko",
  "zh-CN",
  "zh-TW",
] as const;
export type AppLocale = (typeof LOCALES)[number];
export type MessageCatalog = typeof en;

export const catalogs: Record<AppLocale, MessageCatalog> = {
  en,
  de,
  es,
  fr,
  it,
  pt,
  ru,
  ja,
  ko,
  "zh-CN": zhCN,
  "zh-TW": zhTW,
};

const DEFAULT_LOCALE: AppLocale = "en";

export function normalizeLocale(code?: string | null): AppLocale {
  if (!code) return DEFAULT_LOCALE;

  const tag = code.trim().toLowerCase().replace(/_/g, "-");
  if (!tag) return DEFAULT_LOCALE;

  if (tag.startsWith("zh")) {
    return /\b(tw|hk|mo|hant)\b/.test(tag) ? "zh-TW" : "zh-CN";
  }

  const primary = tag.split("-", 1)[0];
  const match = LOCALES.find((locale) => locale === primary);
  return match ?? DEFAULT_LOCALE;
}

interface I18nState {
  locale: AppLocale;
  setLocale(code?: string | null): void;
}

export const useLocale = create<I18nState>()((set) => ({
  locale: DEFAULT_LOCALE,
  setLocale(code) {
    set({ locale: normalizeLocale(code) });
  },
}));

export function setLocale(code?: string | null): void {
  useLocale.getState().setLocale(code);
}
