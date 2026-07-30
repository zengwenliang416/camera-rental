import type { ReactNode } from 'react';

import { ScheduleCenterCommandsProvider } from '../commands/ScheduleCenterCommandsContext';
import { ScheduleCenterDataProvider } from '../data/ScheduleCenterDataContext';
import { PermissionProvider } from '../permissions/PermissionContext';
import { SessionProvider } from '../session/SessionContext';
import { WorkspaceProvider } from './WorkspaceContext';

export function ScheduleCenterProviders({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <PermissionProvider>
        <ScheduleCenterDataProvider>
          <WorkspaceProvider>
            <ScheduleCenterCommandsProvider>
              {children}
            </ScheduleCenterCommandsProvider>
          </WorkspaceProvider>
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  );
}
