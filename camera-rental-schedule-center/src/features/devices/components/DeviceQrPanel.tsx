import { QrCode, ShieldCheck } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

import { usePreferences } from '../../preferences/PreferenceContext';
import { OperationResultPanel } from '../../../shared/ui/OperationResultPanel';
import type { DeviceQrState } from '../deviceQrModel';

export function DeviceQrPanel({
  state,
  deviceNo,
  serialNumber,
}: {
  state: DeviceQrState;
  deviceNo: string;
  serialNumber: string;
}) {
  const { t } = usePreferences();

  return (
    <section className="grid gap-4 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div>
        <span className="inline-flex items-center gap-1.5 text-[10px] font-black uppercase tracking-[0.12em] text-[var(--sc-blue)]">
          <QrCode className="h-3.5 w-3.5" />
          {t('deviceDetail.qrTitle')}
        </span>
        <h3 className="mt-2 text-sm font-black text-[var(--sc-ink)]">{deviceNo}</h3>
        <p className="sc-data mt-1 break-all text-[10px] text-[var(--sc-ink-muted)]">{serialNumber}</p>
        <p className="mt-3 inline-flex items-start gap-1.5 text-[10px] leading-4 text-[var(--sc-ink-muted)]">
          <ShieldCheck className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--sc-green)]" />
          {t('deviceDetail.qrRenderOnly')}
        </p>
      </div>
      <div className="grid min-h-28 min-w-28 place-items-center rounded-xl border border-[var(--sc-border)] bg-white p-3">
        {state.status === 'ready' ? (
          <QRCodeSVG
            value={state.payload}
            size={96}
            level="M"
            role="img"
            aria-label={t('deviceDetail.qrReady')}
          />
        ) : (
          <div className="w-40 max-w-full">
            <OperationResultPanel
              state={state.status === 'loading' || state.status === 'idle' ? 'pending' : 'error'}
              message={
                state.status === 'forbidden'
                  ? t('deviceDetail.qrForbidden')
                  : state.status === 'error'
                    ? t('deviceDetail.qrError')
                    : t('deviceDetail.qrLoading')
              }
            />
          </div>
        )}
      </div>
    </section>
  );
}
