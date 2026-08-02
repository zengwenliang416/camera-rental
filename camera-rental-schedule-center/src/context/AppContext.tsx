import type { ReactNode } from 'react';

import type { DeviceStatus } from '../types';
import { useScheduleCenterCommands } from '../features/commands/ScheduleCenterCommandsContext';
import { useScheduleCenterData } from '../features/data/ScheduleCenterDataContext';
import { usePermissions } from '../features/permissions/PermissionContext';
import { useSession } from '../features/session/SessionContext';
import { ScheduleCenterProviders } from '../features/workspace/ScheduleCenterProviders';
import { useWorkspace } from '../features/workspace/WorkspaceContext';

interface LoginParams {
  tenantName?: string;
  username: string;
  password: string;
  rememberMe?: boolean;
}

export function AppProvider({ children }: { children: ReactNode }) {
  return <ScheduleCenterProviders>{children}</ScheduleCenterProviders>;
}

export function useApp() {
  const session = useSession();
  const permission = usePermissions();
  const data = useScheduleCenterData();
  const workspace = useWorkspace();
  const commands = useScheduleCenterCommands();

  const login = async (params: LoginParams) => {
    await session.login(params);
    workspace.setIsLoginPageVisible(false);
  };

  const logout = async () => {
    await session.logout();
  };

  const syncFromManagementSystem = async () => {
    commands.clearCommandError();
    await permission.refreshPermissions();
  };

  const setPreselectedOrderForBinding = (orderId: string | null) => {
    if (orderId) {
      workspace.openQuickBinding(orderId);
      return;
    }
    workspace.setPreselectedOrderForBinding(null);
  };

  return {
    categories: data.categories,
    models: data.models,
    devices: data.devices,
    orders: data.orders,
    blocks: data.blocks,
    exceptions: data.exceptions,
    isLoading:
      session.isSessionPending
      || permission.isPermissionLoading
      || data.isDataLoading
      || commands.isCommandPending,
    loadError:
      commands.commandError
      || permission.permissionError
      || data.dataError,
    authRequired: session.authRequired,
    accessDenied: permission.accessDenied,
    permissions: permission.permissions,
    hasPermission: permission.hasPermission,
    xianyuConfig: data.xianyuConfig,
    xianyuConfigUnavailable: data.xianyuConfigUnavailable,
    currentUser: session.currentUser,
    isLoggedIn: session.isLoggedIn,
    isLoginPageVisible: workspace.isLoginPageVisible,
    setIsLoginPageVisible: workspace.setIsLoginPageVisible,
    login,
    logout,
    selectedModelId: workspace.selectedModelId,
    setSelectedModelId: workspace.setSelectedModelId,
    activeTab: workspace.activeTab,
    setActiveTab: workspace.setActiveTab,
    selectedOrderIdForAllocation: workspace.selectedOrderIdForAllocation,
    openAllocationModal: workspace.openAllocationModal,
    selectedDeviceIdForDetail: workspace.selectedDeviceIdForDetail,
    openDeviceDetail: workspace.openDeviceDetail,
    isQuickBindingOpen: workspace.isQuickBindingOpen,
    openQuickBinding: workspace.openQuickBinding,
    closeQuickBinding: workspace.closeQuickBinding,
    preselectedOrderForBinding: workspace.preselectedOrderForBinding,
    setPreselectedOrderForBinding,
    assignDevicesToOrder: commands.assignDevicesToOrder,
    bindDeviceWithOrderAndLogistics: commands.bindDeviceWithOrderAndLogistics,
    dispatchOrder: commands.dispatchOrder,
    returnOrder: commands.returnOrder,
    updateDeviceStatus: (
      deviceId: string,
      status: DeviceStatus,
      note?: string
    ) => commands.updateDeviceStatus(deviceId, status, note),
    resolveException: commands.resolveException,
    syncFromManagementSystem,
    lastSyncAt: data.lastSyncAt,
    lastSyncDeviceCount: data.lastSyncDeviceCount,
    lastSyncOrderCount: data.lastSyncOrderCount,
  };
}
