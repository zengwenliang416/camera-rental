import type { ReactNode } from 'react';
import { Button } from './Button';

export function PermissionAwareAction({
  allowed,
  label,
  deniedLabel,
  icon,
  onSelect,
  tone = 'secondary',
}: {
  allowed: boolean;
  label: string;
  deniedLabel: string;
  icon?: ReactNode;
  onSelect: () => void;
  tone?: 'primary' | 'secondary';
}) {
  return (
    <Button
      disabled={!allowed}
      onClick={onSelect}
      variant={tone === 'primary' ? 'secondary' : 'outline'}
      icon={icon}
    >
      {allowed ? label : deniedLabel}
    </Button>
  );
}
