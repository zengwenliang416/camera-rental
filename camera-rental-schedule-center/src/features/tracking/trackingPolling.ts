interface VisibilitySource {
  hidden: boolean;
  addEventListener: (type: 'visibilitychange', listener: () => void) => void;
  removeEventListener: (type: 'visibilitychange', listener: () => void) => void;
}

interface IntervalScheduler {
  setInterval: (callback: () => void, intervalMs: number) => ReturnType<typeof setInterval>;
  clearInterval: (timer: ReturnType<typeof setInterval>) => void;
}

export function startVisibleSummaryPolling({
  visibility,
  scheduler,
  refresh,
  intervalMs = 60_000,
}: {
  visibility: VisibilitySource;
  scheduler: IntervalScheduler;
  refresh: () => void;
  intervalMs?: number;
}) {
  let timer: ReturnType<typeof setInterval> | null = null;

  const stopTimer = () => {
    if (timer === null) return;
    scheduler.clearInterval(timer);
    timer = null;
  };

  const startTimer = () => {
    if (visibility.hidden || timer !== null) return;
    timer = scheduler.setInterval(refresh, intervalMs);
  };

  const onVisibilityChange = () => {
    if (visibility.hidden) {
      stopTimer();
      return;
    }
    refresh();
    startTimer();
  };

  visibility.addEventListener('visibilitychange', onVisibilityChange);
  startTimer();

  return () => {
    visibility.removeEventListener('visibilitychange', onVisibilityChange);
    stopTimer();
  };
}
