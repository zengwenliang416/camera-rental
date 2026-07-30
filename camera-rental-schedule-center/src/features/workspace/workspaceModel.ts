import { canAccessTab } from '../../app/accessModel';
import type { WorkspaceTab } from '../../app/navigation';

export function resolveWorkspaceTab(
  permissions: string[],
  requested: WorkspaceTab
): WorkspaceTab {
  return canAccessTab(permissions, requested) ? requested : 'dashboard';
}

export function resolveSelectedModelId(
  modelIds: string[],
  selectedModelId: string
) {
  if (selectedModelId && modelIds.includes(selectedModelId)) return selectedModelId;
  return modelIds[0] || '';
}
