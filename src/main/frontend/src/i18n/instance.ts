import { create } from "zustand";

import en from "./lang/en.json";

export const LOCALES = ["en"] as const;
export type AppLocale = (typeof LOCALES)[number];
export type MessageCatalog = typeof en;

export const catalogs: Record<AppLocale, MessageCatalog> = { en };

interface I18nState {
  locale: AppLocale;
  setLocale(code?: string | null): void;
}

export const useLocale = create<I18nState>()((set) => ({
  locale: "en",
  setLocale(_code) {
    set({ locale: "en" });
  },
}));

export function setLocale(code?: string | null): void {
  useLocale.getState().setLocale(code);
}

export function normalizeLocale(_code?: string | null): AppLocale {
  return "en";
}
