import { Languages } from 'lucide-react';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { PreferenceChoice } from './PreferenceChoice';

export function LocaleToggle() {
  const { locale, setLocale, t } = usePreferences();
  return (
    <fieldset>
      <legend className="mb-2 flex items-center gap-2 text-xs font-semibold leading-5 text-[var(--sc-ink-muted)]">
        <Languages className="h-4 w-4" />
        {t('preference.locale')}
      </legend>
      <div className="grid grid-cols-2 gap-2">
        <PreferenceChoice
          active={locale === 'zh-CN'}
          icon={<span className="text-xs font-semibold">中</span>}
          label={t('preference.zh')}
          onSelect={() => setLocale('zh-CN')}
        />
        <PreferenceChoice
          active={locale === 'en'}
          icon={<span className="text-[11px] font-semibold">EN</span>}
          label={t('preference.en')}
          onSelect={() => setLocale('en')}
        />
      </div>
    </fieldset>
  );
}
