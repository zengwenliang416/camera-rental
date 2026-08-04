import {
  AlertTriangle,
  Check,
  CircleAlert,
  LockKeyhole,
  PackageSearch,
  Search,
  ShieldCheck,
} from 'lucide-react';

import { formatAmount, getOrderBlockReasons } from '../shippingModel';
import { useShippingMessages } from '../shippingMessages';
import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { Button } from '../../../shared/ui/Button';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { PanelHeader } from './PanelHeader';

function valueOrPending(value: string | number | undefined, missingLabel: string) {
  return value === undefined || value === null || value === '' ? missingLabel : String(value);
}

export function OrderSelectionPanel({
  controller,
}: {
  controller: ShippingWorkbenchController;
}) {
  const { locale, text } = useShippingMessages();
  const {
    pendingOrders,
    eligibleOrderCount,
    filteredOrders,
    selectedDevice,
    selectedOrderId,
    setSelectedOrderId,
    orderSearch,
    setOrderSearch,
    canViewPrivateDetails,
    isSearchingOrders,
    orderSearchError,
  } = controller;

  return (
    <section className="sc-workspace-card overflow-hidden rounded-2xl">
      <PanelHeader
        step="03"
        eyebrow="AUTHORIZED ORDER LOOKUP"
        title={text('order.title')}
        badge={<StatusBadge tone="blue">{text('order.pending', { count: pendingOrders.length })}</StatusBadge>}
      />
      <div className="space-y-4 p-4">
        <div className={`flex gap-3 rounded-xl border p-3 ${
          canViewPrivateDetails
            ? 'border-[color-mix(in_srgb,var(--sc-green)_28%,var(--sc-border))] bg-[var(--sc-green-soft)]'
            : 'border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)]'
        }`}>
          {canViewPrivateDetails
            ? <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-[var(--sc-green)]" />
            : <LockKeyhole className="mt-0.5 h-4 w-4 shrink-0 text-[var(--sc-amber)]" />}
          <div className="text-[11px] leading-5">
            <strong className={canViewPrivateDetails ? 'text-[var(--sc-green)]' : 'text-[var(--sc-amber)]'}>
              {canViewPrivateDetails ? text('order.privateEnabled') : text('order.privateDisabled')}
            </strong>
            <p className="text-[var(--sc-ink-soft)]">
              {canViewPrivateDetails
                ? text('order.privateEnabledDetail')
                : text('order.privateDisabledDetail')}
            </p>
          </div>
        </div>

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">
            {canViewPrivateDetails ? text('order.searchPrivate') : text('order.searchOrder')}
          </span>
          <span className="relative block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
            <input
              value={orderSearch}
              onChange={(event) => setOrderSearch(event.target.value)}
              placeholder={canViewPrivateDetails ? text('order.placeholderPrivate') : text('order.placeholderOrder')}
              autoComplete="off"
              className="sc-form-control min-h-11 w-full rounded-lg border py-2 pl-10 pr-3 text-xs text-[var(--sc-ink)] outline-none focus:border-[var(--sc-blue)] focus:bg-[var(--sc-surface)]"
            />
          </span>
        </label>

        <div className="grid grid-cols-3 gap-2">
          {[
            [text('order.metricPending'), pendingOrders.length],
            [text('order.metricEligible'), eligibleOrderCount],
            [text('order.metricResults'), filteredOrders.length],
          ].map(([label, value]) => (
            <div key={String(label)} className="sc-soft-panel rounded-lg px-3 py-2">
              <span className="block text-[9px] text-[var(--sc-ink-muted)]">{label}</span>
              <strong className="font-mono text-sm text-[var(--sc-ink)]">{value}</strong>
            </div>
          ))}
        </div>

        <div className="max-h-[30rem] space-y-3 overflow-y-auto pr-1">
          {!orderSearch.trim() ? (
            <EmptyState
              icon={<PackageSearch className="h-5 w-5 text-[var(--sc-ink-muted)]" />}
              title={text('order.enterQuery')}
              description={text('order.enterQueryDetail')}
            />
          ) : isSearchingOrders ? (
            <EmptyState
              icon={<Search className="h-5 w-5 animate-pulse text-[var(--sc-blue)]" />}
              title={text('order.searching')}
              description={text('order.searchingDetail')}
            />
          ) : orderSearchError ? (
            <EmptyState
              icon={<CircleAlert className="h-5 w-5 text-[var(--sc-red)]" />}
              title={text('order.searchFailed')}
              description={orderSearchError}
            />
          ) : filteredOrders.length === 0 ? (
            <EmptyState
              icon={<CircleAlert className="h-5 w-5 text-[var(--sc-amber)]" />}
              title={text('order.noMatches')}
              description={text('order.noMatchesDetail')}
            />
          ) : (
            filteredOrders.map(({ order, details }) => {
              const selected = selectedOrderId === order.id;
              const blockReasons = getOrderBlockReasons(order, selectedDevice, locale);
              return (
                <Button
                  key={order.id}
                  type="button"
                  onClick={() => setSelectedOrderId(order.id)}
                  variant="glass"
                  size="sm"
                  className={`!flex !flex-col !items-stretch !justify-start !gap-0 !whitespace-normal w-full overflow-hidden rounded-xl border p-0 text-left ${
                    selected
                      ? '!border-[color-mix(in_srgb,var(--sc-blue)_42%,var(--sc-border))] !bg-[var(--sc-blue-soft)]'
                      : '!border-[var(--sc-border)] !bg-[var(--sc-surface)] hover:!border-[var(--sc-border-strong)]'
                  }`}
                >
                  <span className="flex items-start gap-3 border-b border-inherit p-3">
                    <span className={`mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-lg ${
                      selected
                        ? 'bg-[var(--sc-blue)] text-[var(--sc-on-accent)]'
                        : blockReasons.length
                          ? 'bg-[var(--sc-amber-soft)] text-[var(--sc-amber)]'
                          : 'bg-[var(--sc-green-soft)] text-[var(--sc-green)]'
                    }`}>
                      {selected
                        ? <Check className="h-4 w-4" />
                        : blockReasons.length
                          ? <AlertTriangle className="h-4 w-4" />
                          : <ShieldCheck className="h-4 w-4" />}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="flex flex-wrap items-center justify-between gap-2">
                        <strong className="font-mono text-xs text-[var(--sc-ink)]">{order.orderNumber}</strong>
                        <StatusBadge tone={blockReasons.length ? 'amber' : 'green'}>
                          {blockReasons.length ? text('order.reviewOnly') : text('order.gatesPassed')}
                        </StatusBadge>
                      </span>
                      <span className="mt-1 block text-[10px] text-[var(--sc-ink-muted)]">
                        {order.rentalPeriodLabel}
                      </span>
                    </span>
                  </span>

                  <span className="grid gap-x-4 gap-y-2 p-3 text-[10px] sm:grid-cols-2">
                    <span>
                      <b className="block text-[var(--sc-ink-muted)]">{text('order.receiver')}</b>
                      <span className="text-[var(--sc-ink-soft)]">
                        {canViewPrivateDetails
                          ? valueOrPending(details?.receiverName, text('order.missing'))
                          : valueOrPending(order.customerName, text('order.missing'))}
                      </span>
                    </span>
                    <span>
                      <b className="block text-[var(--sc-ink-muted)]">{text('order.phone')}</b>
                      <span className="font-mono text-[var(--sc-ink-soft)]">
                        {canViewPrivateDetails
                          ? valueOrPending(details?.receiverPhone, text('order.missing'))
                          : valueOrPending(order.customerPhone, text('order.missing'))}
                      </span>
                    </span>
                    {canViewPrivateDetails && (
                      <>
                        <span className="sm:col-span-2">
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.address')}</b>
                          <span className="text-[var(--sc-ink-soft)]">{valueOrPending(details?.receiverAddress, text('order.missing'))}</span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.goods')}</b>
                          <span className="text-[var(--sc-ink-soft)]">
                            {valueOrPending(details?.goodsTitle, text('order.missing'))} · {text('order.quantity', { count: valueOrPending(details?.goodsQuantity, text('order.missing')) })}
                          </span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.buyer')}</b>
                          <span className="text-[var(--sc-ink-soft)]">{valueOrPending(details?.buyerNick, text('order.missing'))}</span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.amount')}</b>
                          <span className="font-mono text-[var(--sc-ink-soft)]">{formatAmount(details?.amountCents)}</span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.channelStatus')}</b>
                          <span className="font-mono text-[var(--sc-ink-soft)]">
                            {valueOrPending(details?.channelStatus, text('order.missing'))} / {valueOrPending(details?.conversionStatus, text('order.missing'))}
                          </span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.shop')}</b>
                          <span className="font-mono text-[var(--sc-ink-soft)]">{valueOrPending(details?.shopId, text('order.missing'))}</span>
                        </span>
                        <span>
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.rentalOrder')}</b>
                          <span className="font-mono text-[var(--sc-ink-soft)]">
                            {valueOrPending(details?.rentalOrderId, text('order.missing'))}
                          </span>
                        </span>
                        <span className="sm:col-span-2">
                          <b className="block text-[var(--sc-ink-muted)]">{text('order.remark')}</b>
                          <span className="whitespace-pre-wrap text-[var(--sc-ink-soft)]">
                            {valueOrPending(details?.sellerRemark, text('order.missing'))}
                          </span>
                        </span>
                      </>
                    )}
                  </span>

                  {blockReasons.length > 0 && (
                    <span className="flex gap-2 border-t border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)] px-3 py-2 text-[10px] leading-5 text-[var(--sc-amber)]">
                      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                      {blockReasons.join('；')}
                    </span>
                  )}
                </Button>
              );
            })
          )}
        </div>

        <p className="text-[10px] leading-5 text-[var(--sc-ink-muted)]">
          {text('order.memoryOnly')}
        </p>
      </div>
    </section>
  );
}
