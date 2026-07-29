import { normalizeLocale, setLocale, type AppLocale } from "./instance";

export function detectHostLocale(): AppLocale {
  const raw = typeof navigator !== "undefined" ? navigator.language : null;
  return normalizeLocale(raw);
}

export function syncHostLocale(): AppLocale {
  const locale = detectHostLocale();
  setLocale(locale);
  return locale;
}
