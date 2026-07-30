import { useApp } from '../context/AppContext';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { ShippingWorkbench } from '../features/shipping/components/ShippingWorkbench';
import { ConfirmDialogShell } from '../shared/ui/ConfirmDialogShell';

export function QuickBindingModal() {
  const { isQuickBindingOpen, closeQuickBinding } = useApp();
  const { t } = usePreferences();

  if (!isQuickBindingOpen) return null;

  return (
    <ConfirmDialogShell
      ariaLabel={t('shipping.modalTitle')}
      closeLabel={t('shipping.modalClose')}
      title={t('shipping.modalTitle')}
      description={t('shipping.modalDescription')}
      onClose={closeQuickBinding}
    >
      <ShippingWorkbench embedded />
    </ConfirmDialogShell>
  );
}
