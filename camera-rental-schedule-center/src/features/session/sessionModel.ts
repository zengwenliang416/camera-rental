export interface SessionState<User> {
  isLoggedIn: boolean;
  authRequired: boolean;
  currentUser?: User;
}

export function createSessionState<User>(
  accessToken: string | undefined,
  currentUser?: User
): SessionState<User> {
  return {
    isLoggedIn: Boolean(accessToken),
    authRequired: !accessToken,
    currentUser,
  };
}

export function resetSessionState<User>(): SessionState<User> {
  return {
    isLoggedIn: false,
    authRequired: true,
    currentUser: undefined,
  };
}
