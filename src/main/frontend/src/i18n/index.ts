import {
  catalogs,
  useLocale,
  type AppLocale,
  type MessageCatalog,
} from "./instance";

export {
  catalogs,
  LOCALES,
  normalizeLocale,
  setLocale,
  useLocale,
} from "./instance";
export type { AppLocale, MessageCatalog };

function interpolate(
  template: string,
  vars?: Record<string, string | number>,
): string {
  if (!vars) return template;
  return template.replace(/\{(\w+)\}/g, (_, name: string) =>
    Object.prototype.hasOwnProperty.call(vars, name) ? String(vars[name]) : `{${name}}`,
  );
}

export function translate(key: string, vars?: Record<string, string | number>): string {
  const locale = useLocale.getState().locale;
  const catalog = catalogs[locale] ?? catalogs.en;
  const template = (catalog as Record<string, string>)[key];
  if (typeof template !== "string") return key;
  return interpolate(template, vars);
}

export function useTranslation(): typeof translate {
  useLocale((s) => s.locale);
  return translate;
}
