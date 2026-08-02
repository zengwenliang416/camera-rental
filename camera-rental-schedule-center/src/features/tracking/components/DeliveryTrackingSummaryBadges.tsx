import { AlertTriangle, Package2 } from 'lucide-react';

import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { usePreferences } from '../../preferences/PreferenceContext';
import { trackingCopy, trackingStatusLabel } from '../trackingCopy';
import {
  trackingStatusPresentation,
  type DeliveryOrderSummary,
  type DeliveryPackageSummary,
  type DeliveryTrackingStatus,
} from '../trackingModel';

const statusPriority: Record<DeliveryTrackingStatus, number> = {
  EXCEPTION: 0,
  CUSTOMS: 1,
  OUT_FOR_DELIVERY: 2,
  IN_TRANSIT: 3,
  RETURNING: 4,
  PICKED_UP: 5,
  INFO_RECEIVED: 6,
  CREATED: 7,
  DELIVERED: 8,
  RETURNED: 9,
  UNKNOWN: 10,
};

export function primaryTrackingPackage(
  summary: DeliveryOrderSummary
): DeliveryPackageSummary | undefined {
  return [...summary.packages].sort(
    (left, right) =>
      statusPriority[left.trackingStatus] - statusPriority[right.trackingStatus]
  )[0];
}

export function deliveryTrackingSummaryText(
  locale: 'zh-CN' | 'en',
  summary: DeliveryOrderSummary
) {
  const primaryPackage = primaryTrackingPackage(summary);
  const status = primaryPackage
    ? trackingStatusLabel(locale, primaryPackage.trackingStatus)
    : trackingCopy(locale, 'summary.unshipped');
  if (summary.packageCount <= 1) return status;
  const packageLabel = locale === 'en'
    ? `${summary.packageCount} ${trackingCopy(locale, 'summary.multiple')}`
    : `${summary.packageCount}${trackingCopy(locale, 'summary.multiple')}`;
  return `${packageLabel} · ${status}`;
}

export function DeliveryTrackingSummaryBadges({
  summary,
}: {
  summary: DeliveryOrderSummary;
}) {
  const { locale } = usePreferences();
  const primaryPackage = primaryTrackingPackage(summary);
  const state = primaryPackage
    ? trackingStatusPresentation(primaryPackage.trackingStatus)
    : null;

  return (
    <span className="flex flex-wrap items-center gap-1.5">
      {summary.packageCount > 1 && (
        <StatusBadge tone="neutral" icon={<Package2 className="h-3 w-3" />}>
          {locale === 'en'
            ? `${summary.packageCount} ${trackingCopy(locale, 'summary.multiple')}`
            : `${summary.packageCount}${trackingCopy(locale, 'summary.multiple')}`}
        </StatusBadge>
      )}
      {primaryPackage && state && (
        <StatusBadge tone={state.tone}>
          {trackingStatusLabel(locale, primaryPackage.trackingStatus)}
        </StatusBadge>
      )}
      {summary.risks.length > 0 && (
        <StatusBadge tone="red" icon={<AlertTriangle className="h-3 w-3" />}>
          {trackingCopy(locale, 'summary.risk')} {summary.risks.length}
        </StatusBadge>
      )}
    </span>
  );
}
