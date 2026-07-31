import type { ReactNode } from 'react';

import { ScheduleCenterCommandsProvider } from '../commands/ScheduleCenterCommandsContext';
import { ScheduleCenterDataProvider } from '../data/ScheduleCenterDataContext';
import { PermissionProvider } from '../permissions/PermissionContext';
import { SessionProvider } from '../session/SessionContext';
import { DeliveryTrackingProvider } from '../tracking/TrackingContext';
import { WorkspaceProvider } from './WorkspaceContext';

export function ScheduleCenterProviders({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <PermissionProvider>
        <ScheduleCenterDataProvider>
          <DeliveryTrackingProvider>
            <WorkspaceProvider>
              <ScheduleCenterCommandsProvider>
                {children}
              </ScheduleCenterCommandsProvider>
            </WorkspaceProvider>
          </DeliveryTrackingProvider>
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  );
}
