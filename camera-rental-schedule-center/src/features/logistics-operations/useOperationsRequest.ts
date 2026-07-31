import { useCallback, useReducer, useRef } from 'react';

import { isAuthenticationFailure } from '../../api/client';
import { classifySafeError } from '../../shared/lib/safeError';
import { useSession } from '../session/SessionContext';
import {
  initialOperationsResourceState,
  operationsResourceReducer,
} from './operationsState';

export function useOperationsRequest<T>(initialData: T | null = null) {
  const { requireAuthentication } = useSession();
  const requestToken = useRef<symbol | null>(null);
  const [state, dispatch] = useReducer(
    operationsResourceReducer<T>,
    initialOperationsResourceState(initialData)
  );

  const run = useCallback(
    async (request: () => Promise<T>, empty?: (value: T) => boolean) => {
      const token = Symbol('operations-request');
      requestToken.current = token;
      dispatch({ type: 'load' });
      try {
        const value = await request();
        if (requestToken.current !== token) return null;
        dispatch({
          type: 'success',
          data: value,
          empty: empty ? empty(value) : false,
        });
        return value;
      } catch (error) {
        if (requestToken.current !== token) return null;
        if (isAuthenticationFailure(error)) {
          requireAuthentication();
          dispatch({ type: 'failure', error: 'authentication' });
        } else {
          const message = error instanceof Error ? error.message : null;
          dispatch({ type: 'failure', error: classifySafeError(message) });
        }
        return null;
      }
    },
    [requireAuthentication]
  );

  const reset = useCallback(() => {
    requestToken.current = null;
    dispatch({ type: 'reset' });
  }, []);

  return { state, run, reset };
}

