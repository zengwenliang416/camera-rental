import type { ComponentType } from 'react';

export type WorkspaceTab =
  | 'dashboard'
  | 'schedule'
  | 'orders'
  | 'devices'
  | 'binding'
  | 'exceptions';

export interface WorkspaceNavItem {
  id: WorkspaceTab;
  label: string;
  icon: ComponentType<{ className?: string }>;
  permission?: string;
  badge?: number;
  danger?: boolean;
}
