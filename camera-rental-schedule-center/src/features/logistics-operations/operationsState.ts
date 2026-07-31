import type { SafeErrorCategory } from '../../shared/lib/safeError';

export type OperationsResourceStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'empty'
  | 'error';

export interface OperationsResourceState<T> {
  status: OperationsResourceStatus;
  data: T | null;
  error: SafeErrorCategory | null;
}

export type OperationsResourceAction<T> =
  | { type: 'load' }
  | { type: 'success'; data: T; empty?: boolean }
  | { type: 'failure'; error: SafeErrorCategory }
  | { type: 'reset' };

export function initialOperationsResourceState<T>(
  data: T | null = null
): OperationsResourceState<T> {
  return {
    status: data === null ? 'idle' : 'ready',
    data,
    error: null,
  };
}

export function operationsResourceReducer<T>(
  state: OperationsResourceState<T>,
  action: OperationsResourceAction<T>
): OperationsResourceState<T> {
  if (action.type === 'load') {
    return { ...state, status: 'loading', error: null };
  }
  if (action.type === 'success') {
    return {
      status: action.empty ? 'empty' : 'ready',
      data: action.data,
      error: null,
    };
  }
  if (action.type === 'failure') {
    return {
      status: 'error',
      data: state.data,
      error: action.error,
    };
  }
  return initialOperationsResourceState<T>();
}

