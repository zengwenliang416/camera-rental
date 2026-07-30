import type { LocalePreference } from '../preferences/preferenceModel';
import { shippingMessage } from './shippingMessages';

export function safeShippingError(
  error: unknown,
  fallback: string,
  locale: LocalePreference = 'zh-CN'
) {
  const message = error instanceof Error ? error.message.trim() : '';
  if (!message) return fallback;
  if (/failed to fetch|networkerror|load failed/i.test(message)) {
    return shippingMessage(locale, 'runtime.networkError');
  }
  if (/401|auth_required|no_refresh_token/i.test(message)) {
    return shippingMessage(locale, 'runtime.authenticationError');
  }
  if (/403|forbidden|permission/i.test(message)) {
    return shippingMessage(locale, 'runtime.permissionError');
  }
  return fallback;
}
