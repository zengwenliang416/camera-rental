import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import type { WorkspaceTab } from '../../app/navigation';
import { useScheduleCenterData } from '../data/ScheduleCenterDataContext';
import { usePermissions } from '../permissions/PermissionContext';
import { useSession } from '../session/SessionContext';
import { resolveSelectedModelId, resolveWorkspaceTab } from './workspaceModel';
import { useQuickBindingWorkspace } from './useQuickBindingWorkspace';

interface WorkspaceContextValue {
  selectedModelId: string;
  setSelectedModelId: (id: string) => void;
  activeTab: WorkspaceTab;
  setActiveTab: (tab: WorkspaceTab) => void;
  selectedOrderIdForAllocation: string | null;
  openAllocationModal: (orderId: string | null) => void;
  selectedDeviceIdForDetail: string | null;
  openDeviceDetail: (deviceId: string | null) => void;
  isQuickBindingOpen: boolean;
  openQuickBinding: (orderId: string | null) => void;
  closeQuickBinding: () => void;
  preselectedOrderForBinding: string | null;
  setPreselectedOrderForBinding: (orderId: string | null) => void;
  isLoginPageVisible: boolean;
  setIsLoginPageVisible: (visible: boolean) => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { permissions } = usePermissions();
  const { models } = useScheduleCenterData();
  const { isLoggedIn, sessionRevision } = useSession();
  const [selectedModelId, setSelectedModelId] = useState('');
  const [activeTab, setActiveTabState] = useState<WorkspaceTab>('dashboard');
  const [selectedOrderIdForAllocation, openAllocationModal] =
    useState<string | null>(null);
  const [selectedDeviceIdForDetail, openDeviceDetail] =
    useState<string | null>(null);
  const [isLoginPageVisible, setIsLoginPageVisible] = useState(false);

  const setActiveTab = useCallback(
    (tab: WorkspaceTab) => setActiveTabState(resolveWorkspaceTab(permissions, tab)),
    [permissions]
  );
  const quickBinding = useQuickBindingWorkspace(setActiveTab);

  useEffect(() => {
    setActiveTabState((tab) => resolveWorkspaceTab(permissions, tab));
  }, [permissions]);

  useEffect(() => {
    setSelectedModelId((current) =>
      resolveSelectedModelId(models.map((model) => model.id), current)
    );
  }, [models]);

  useEffect(() => {
    setActiveTabState('dashboard');
    openAllocationModal(null);
    openDeviceDetail(null);
    quickBinding.resetQuickBinding();
    if (!isLoggedIn) setIsLoginPageVisible(false);
  }, [isLoggedIn, quickBinding.resetQuickBinding, sessionRevision]);

  return (
    <WorkspaceContext.Provider
      value={{
        selectedModelId,
        setSelectedModelId,
        activeTab,
        setActiveTab,
        selectedOrderIdForAllocation,
        openAllocationModal,
        selectedDeviceIdForDetail,
        openDeviceDetail,
        isQuickBindingOpen: quickBinding.isQuickBindingOpen,
        openQuickBinding: quickBinding.openQuickBinding,
        closeQuickBinding: quickBinding.closeQuickBinding,
        preselectedOrderForBinding: quickBinding.preselectedOrderForBinding,
        setPreselectedOrderForBinding:
          quickBinding.setPreselectedOrderForBinding,
        isLoginPageVisible,
        setIsLoginPageVisible,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error('useWorkspace must be used within WorkspaceProvider');
  return context;
}
