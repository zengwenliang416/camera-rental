import { Moon, Sun } from 'lucide-react';
import type { Ref } from 'react';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { PreferenceChoice } from './PreferenceChoice';

export function ThemeToggle({ firstButtonRef }: { firstButtonRef?: Ref<HTMLButtonElement> }) {
  const { theme, setTheme, t } = usePreferences();
  return (
    <fieldset>
      <legend className="mb-2 flex items-center gap-2 text-xs font-semibold leading-5 text-[var(--sc-ink-muted)]">
        <Sun className="h-4 w-4" />
        {t('preference.theme')}
      </legend>
      <div className="grid grid-cols-2 gap-2">
        <PreferenceChoice
          ref={firstButtonRef}
          active={theme === 'light'}
          icon={<Sun className="h-4 w-4" />}
          label={t('preference.light')}
          onSelect={() => setTheme('light')}
        />
        <PreferenceChoice
          active={theme === 'dark'}
          icon={<Moon className="h-4 w-4" />}
          label={t('preference.dark')}
          onSelect={() => setTheme('dark')}
        />
      </div>
    </fieldset>
  );
}
