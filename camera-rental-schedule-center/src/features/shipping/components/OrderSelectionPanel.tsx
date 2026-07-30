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
    <section className="overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-sm">
      <PanelHeader
        step="03"
        eyebrow="AUTHORIZED ORDER LOOKUP"
        title={text('order.title')}
        badge={(
          <span className="rounded-full bg-blue-50 px-2.5 py-1 text-[10px] font-bold text-blue-700">
            {text('order.pending', { count: pendingOrders.length })}
          </span>
        )}
      />
      <div className="space-y-4 p-4">
        <div className={`flex gap-3 rounded-xl border p-3 ${
          canViewPrivateDetails
            ? 'border-emerald-200 bg-emerald-50'
            : 'border-amber-200 bg-amber-50'
        }`}>
          {canViewPrivateDetails
            ? <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-700" />
            : <LockKeyhole className="mt-0.5 h-4 w-4 shrink-0 text-amber-700" />}
          <div className="text-[11px] leading-5">
            <strong className={canViewPrivateDetails ? 'text-emerald-900' : 'text-amber-900'}>
              {canViewPrivateDetails ? text('order.privateEnabled') : text('order.privateDisabled')}
            </strong>
            <p className={canViewPrivateDetails ? 'text-emerald-800' : 'text-amber-800'}>
              {canViewPrivateDetails
                ? text('order.privateEnabledDetail')
                : text('order.privateDisabledDetail')}
            </p>
          </div>
        </div>

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-zinc-500">
            {canViewPrivateDetails ? text('order.searchPrivate') : text('order.searchOrder')}
          </span>
          <span className="relative block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <input
              value={orderSearch}
              onChange={(event) => setOrderSearch(event.target.value)}
              placeholder={canViewPrivateDetails ? text('order.placeholderPrivate') : text('order.placeholderOrder')}
              autoComplete="off"
              className="min-h-11 w-full rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] py-2 pl-10 pr-3 text-xs text-[var(--sc-ink)] outline-none transition focus:border-blue-500 focus:bg-[var(--sc-surface)] focus:ring-4 focus:ring-blue-100"
            />
          </span>
        </label>

        <div className="grid grid-cols-3 gap-2">
          {[
            [text('order.metricPending'), pendingOrders.length],
            [text('order.metricEligible'), eligibleOrderCount],
            [text('order.metricResults'), filteredOrders.length],
          ].map(([label, value]) => (
            <div key={String(label)} className="rounded-lg bg-[var(--sc-surface-soft)] px-3 py-2">
              <span className="block text-[9px] text-[var(--sc-ink-muted)]">{label}</span>
              <strong className="font-mono text-sm text-[var(--sc-ink)]">{value}</strong>
            </div>
          ))}
        </div>

        <div className="max-h-[30rem] space-y-3 overflow-y-auto pr-1">
          {!orderSearch.trim() ? (
            <div className="rounded-xl border border-dashed border-[var(--sc-border-strong)] px-4 py-8 text-center">
              <PackageSearch className="mx-auto h-5 w-5 text-[var(--sc-ink-muted)]" />
              <strong className="mt-2 block text-xs text-[var(--sc-ink-soft)]">{text('order.enterQuery')}</strong>
              <small className="mt-1 block text-[10px] leading-5 text-[var(--sc-ink-muted)]">
                {text('order.enterQueryDetail')}
              </small>
            </div>
          ) : isSearchingOrders ? (
            <div className="rounded-xl border border-dashed border-blue-300 bg-blue-50 px-4 py-8 text-center">
              <Search className="mx-auto h-5 w-5 animate-pulse text-blue-700" />
              <strong className="mt-2 block text-xs text-blue-900">{text('order.searching')}</strong>
              <small className="mt-1 block text-[10px] leading-5 text-blue-800">
                {text('order.searchingDetail')}
              </small>
            </div>
          ) : orderSearchError ? (
            <div className="rounded-xl border border-dashed border-rose-300 bg-rose-50 px-4 py-8 text-center">
              <CircleAlert className="mx-auto h-5 w-5 text-rose-700" />
              <strong className="mt-2 block text-xs text-rose-900">{text('order.searchFailed')}</strong>
              <small className="mt-1 block text-[10px] leading-5 text-rose-800">
                {orderSearchError}
              </small>
            </div>
          ) : filteredOrders.length === 0 ? (
            <div className="rounded-xl border border-dashed border-amber-300 bg-amber-50 px-4 py-8 text-center">
              <CircleAlert className="mx-auto h-5 w-5 text-amber-700" />
              <strong className="mt-2 block text-xs text-amber-900">{text('order.noMatches')}</strong>
              <small className="mt-1 block text-[10px] leading-5 text-amber-800">
                {text('order.noMatchesDetail')}
              </small>
            </div>
          ) : (
            filteredOrders.map(({ order, details }) => {
              const selected = selectedOrderId === order.id;
              const blockReasons = getOrderBlockReasons(order, selectedDevice, locale);
              return (
                <button
                  key={order.id}
                  type="button"
                  onClick={() => setSelectedOrderId(order.id)}
                  className={`w-full overflow-hidden rounded-xl border text-left transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100 ${
                    selected
                      ? 'border-blue-600 bg-blue-50'
                      : 'border-[var(--sc-border)] bg-[var(--sc-surface)] hover:border-[var(--sc-border-strong)]'
                  }`}
                >
                  <span className="flex items-start gap-3 border-b border-inherit p-3">
                    <span className={`mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-lg ${
                      selected ? 'bg-blue-600 text-white' : blockReasons.length ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'
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
                        <span className={`rounded-full px-2 py-0.5 text-[9px] font-bold ${
                          blockReasons.length
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {blockReasons.length ? text('order.reviewOnly') : text('order.gatesPassed')}
                        </span>
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
                    <span className="flex gap-2 border-t border-amber-200 bg-amber-50 px-3 py-2 text-[10px] leading-5 text-amber-900">
                      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                      {blockReasons.join('；')}
                    </span>
                  )}
                </button>
              );
            })
          )}
        </div>

        <p className="text-[10px] leading-5 text-zinc-400">
          {text('order.memoryOnly')}
        </p>
      </div>
    </section>
  );
}
