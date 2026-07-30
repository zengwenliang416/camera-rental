export type LoginValidation = 'ready' | 'username' | 'password';
export type LoginErrorPresentation =
  | 'network'
  | 'authentication'
  | 'permission'
  | 'timeout'
  | 'unknown';

export function validateLoginCredentials(username: string, password: string): LoginValidation {
  if (!username.trim()) return 'username';
  if (!password) return 'password';
  return 'ready';
}

export function loginErrorPresentation(category: string): LoginErrorPresentation {
  if (
    category === 'network'
    || category === 'authentication'
    || category === 'permission'
    || category === 'timeout'
  ) {
    return category;
  }
  return 'unknown';
}
