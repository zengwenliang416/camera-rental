import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { message, type MessageKey } from './messages';
import {
  persistLocale,
  persistTheme,
  readPreferences,
  type LocalePreference,
  type ThemePreference,
} from './preferenceModel';

interface PreferenceContextValue {
  theme: ThemePreference;
  locale: LocalePreference;
  setTheme: (theme: ThemePreference) => void;
  setLocale: (locale: LocalePreference) => void;
  t: (key: MessageKey) => string;
}

const PreferenceContext = createContext<PreferenceContextValue | null>(null);

function browserPreferences() {
  if (typeof window === 'undefined') return readPreferences(null);
  return readPreferences(window.localStorage, window.matchMedia?.('(prefers-color-scheme: dark)').matches);
}

export function PreferenceProvider({ children }: { children: ReactNode }) {
  const initial = browserPreferences();
  const [theme, setThemeState] = useState<ThemePreference>(initial.theme);
  const [locale, setLocaleState] = useState<LocalePreference>(initial.locale);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    persistTheme(window.localStorage, theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.lang = locale;
    persistLocale(window.localStorage, locale);
  }, [locale]);

  return (
    <PreferenceContext.Provider
      value={{
        theme,
        locale,
        setTheme: setThemeState,
        setLocale: setLocaleState,
        t: (key) => message(locale, key),
      }}
    >
      {children}
    </PreferenceContext.Provider>
  );
}

export function usePreferences() {
  const context = useContext(PreferenceContext);
  if (!context) throw new Error('usePreferences must be used within PreferenceProvider');
  return context;
}
