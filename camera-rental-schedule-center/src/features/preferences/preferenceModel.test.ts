import assert from 'node:assert/strict';
import test from 'node:test';
import {
  LOCALE_STORAGE_KEY,
  THEME_STORAGE_KEY,
  normalizeLocale,
  normalizeTheme,
  persistLocale,
  persistTheme,
  readPreferences,
} from './preferenceModel';

class MemoryStorage {
  values = new Map<string, string>();

  getItem(key: string) {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string) {
    this.values.set(key, value);
  }
}

test('invalid stored preferences use the safe system and zh-CN defaults', () => {
  const storage = new MemoryStorage();
  storage.setItem(THEME_STORAGE_KEY, 'auto');
  storage.setItem(LOCALE_STORAGE_KEY, 'fr');

  assert.deepEqual(readPreferences(storage, true), { theme: 'dark', locale: 'zh-CN' });
  assert.equal(normalizeTheme('invalid', false), 'light');
  assert.equal(normalizeLocale(null), 'zh-CN');
});

test('only explicit theme and locale values are persisted', () => {
  const storage = new MemoryStorage();

  persistTheme(storage, 'light');
  persistLocale(storage, 'en');

  assert.equal(storage.getItem(THEME_STORAGE_KEY), 'light');
  assert.equal(storage.getItem(LOCALE_STORAGE_KEY), 'en');
});

test('unavailable browser storage falls back safely', () => {
  const storage = {
    getItem() {
      throw new Error('blocked');
    },
    setItem() {
      throw new Error('blocked');
    },
  };
  assert.deepEqual(readPreferences(storage, false), { theme: 'light', locale: 'zh-CN' });
  assert.doesNotThrow(() => persistTheme(storage, 'dark'));
  assert.doesNotThrow(() => persistLocale(storage, 'en'));
});
