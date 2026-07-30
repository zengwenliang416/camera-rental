export type ThemePreference = 'light' | 'dark';
export type LocalePreference = 'zh-CN' | 'en';

export const THEME_STORAGE_KEY = 'schedule-center.theme';
export const LOCALE_STORAGE_KEY = 'schedule-center.locale';

export interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

export function normalizeTheme(value: string | null, systemDark = false): ThemePreference {
  if (value === 'light' || value === 'dark') return value;
  return systemDark ? 'dark' : 'light';
}

export function normalizeLocale(value: string | null): LocalePreference {
  return value === 'en' ? 'en' : 'zh-CN';
}

export function readPreferences(storage: StorageLike | null, systemDark = false) {
  let theme: string | null = null;
  let locale: string | null = null;
  try {
    theme = storage?.getItem(THEME_STORAGE_KEY) ?? null;
    locale = storage?.getItem(LOCALE_STORAGE_KEY) ?? null;
  } catch {
    // Restricted storage must not prevent the shell from rendering.
  }
  return {
    theme: normalizeTheme(theme, systemDark),
    locale: normalizeLocale(locale),
  };
}

export function persistTheme(storage: StorageLike | null, theme: ThemePreference) {
  try {
    storage?.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Preferences remain usable in memory when storage access is unavailable.
  }
}

export function persistLocale(storage: StorageLike | null, locale: LocalePreference) {
  try {
    storage?.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // Preferences remain usable in memory when storage access is unavailable.
  }
}
