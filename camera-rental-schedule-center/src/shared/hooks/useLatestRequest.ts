import { useCallback, useEffect, useRef } from 'react';

export function useLatestRequest(scope?: unknown) {
  const generation = useRef(0);
  const previousScope = useRef(scope);

  if (!Object.is(previousScope.current, scope)) {
    previousScope.current = scope;
    generation.current += 1;
  }

  useEffect(
    () => () => {
      generation.current += 1;
    },
    []
  );

  return useCallback(() => {
    const current = ++generation.current;
    return () => generation.current === current;
  }, []);
}
