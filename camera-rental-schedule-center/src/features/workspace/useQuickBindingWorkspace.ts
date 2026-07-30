import { useCallback, useState } from 'react';

import type { WorkspaceTab } from '../../app/navigation';

export function useQuickBindingWorkspace(
  setActiveTab: (tab: WorkspaceTab) => void
) {
  const [isQuickBindingOpen, setIsQuickBindingOpen] = useState(false);
  const [preselectedOrderForBinding, setPreselectedOrderForBinding] =
    useState<string | null>(null);

  const openQuickBinding = useCallback(
    (orderId: string | null) => {
      if (!orderId) {
        setPreselectedOrderForBinding(null);
        setIsQuickBindingOpen(false);
        setActiveTab('binding');
        return;
      }
      setPreselectedOrderForBinding(orderId);
      setIsQuickBindingOpen(true);
    },
    [setActiveTab]
  );

  const closeQuickBinding = useCallback(() => {
    setIsQuickBindingOpen(false);
    setPreselectedOrderForBinding(null);
  }, []);

  const resetQuickBinding = useCallback(() => {
    setIsQuickBindingOpen(false);
    setPreselectedOrderForBinding(null);
  }, []);

  return {
    isQuickBindingOpen,
    openQuickBinding,
    closeQuickBinding,
    resetQuickBinding,
    preselectedOrderForBinding,
    setPreselectedOrderForBinding,
  };
}
