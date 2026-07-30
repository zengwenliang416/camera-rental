export type QueryHealth = 'ready' | 'partial';

export function queryHealth(failures: string[]): QueryHealth {
  return failures.length > 0 ? 'partial' : 'ready';
}
