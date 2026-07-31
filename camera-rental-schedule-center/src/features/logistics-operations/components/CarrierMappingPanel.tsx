import {
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Trash2,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';

import {
  deleteRentalLogisticsCarrierMapping,
  fetchRentalLogisticsCarrierMappings,
  saveRentalLogisticsCarrierMapping,
  type RentalLogisticsCarrierMappingSaveReqVO,
  type RentalLogisticsCarrierMappingVO,
} from '../../../api/rental';
import { ConfirmDialogShell } from '../../../shared/ui/ConfirmDialogShell';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { OperationResultPanel } from '../../../shared/ui/OperationResultPanel';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { LocalePreference } from '../../preferences/preferenceModel';
import {
  carrierMappingDraftIsValid,
  emptyCarrierMappingDraft,
  operationsStatusTone,
  type LogisticsOperationsAccess,
} from '../operationsModel';
import {
  operationsCodeLabel,
  operationsCopy,
  operationsErrorCopy,
} from '../operationsCopy';
import { useOperationsRequest } from '../useOperationsRequest';
import {
  fieldClassName,
  OperationsPanel,
  PanelQueryBoundary,
  primaryButtonClassName,
  secondaryButtonClassName,
} from './OperationsPanel';

function MappingForm({
  locale,
  draft,
  setDraft,
  disabled,
}: {
  locale: LocalePreference;
  draft: RentalLogisticsCarrierMappingSaveReqVO;
  setDraft: (draft: RentalLogisticsCarrierMappingSaveReqVO) => void;
  disabled: boolean;
}) {
  const update = (
    key: keyof RentalLogisticsCarrierMappingSaveReqVO,
    value: string
  ) => setDraft({ ...draft, [key]: value });
  const fields: Array<{
    key: keyof RentalLogisticsCarrierMappingSaveReqVO;
    label: string;
  }> = [
    { key: 'sourceType', label: operationsCopy(locale, 'mapping.source') },
    { key: 'sourceCarrierCode', label: operationsCopy(locale, 'mapping.sourceCode') },
    { key: 'canonicalCarrierCode', label: operationsCopy(locale, 'mapping.canonical') },
    { key: 'displayName', label: operationsCopy(locale, 'mapping.displayName') },
    { key: 'providerCode', label: operationsCopy(locale, 'mapping.provider') },
    { key: 'providerCarrierCode', label: operationsCopy(locale, 'mapping.providerCode') },
  ];
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {fields.map((field) => (
        <label key={field.key} className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
          {field.label}
          <input
            value={String(draft[field.key] || '')}
            disabled={disabled}
            onChange={(event) => update(field.key, event.target.value)}
            className={`${fieldClassName} mt-1.5`}
          />
        </label>
      ))}
      <label className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
        {operationsCopy(locale, 'mapping.phone')}
        <select
          value={draft.phoneRequirement}
          disabled={disabled}
          onChange={(event) => update('phoneRequirement', event.target.value)}
          className={`${fieldClassName} mt-1.5`}
        >
          <option value="NONE">NONE</option>
          <option value="OPTIONAL">OPTIONAL</option>
          <option value="REQUIRED">REQUIRED</option>
        </select>
      </label>
      <label className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
        {operationsCopy(locale, 'mapping.state')}
        <select
          value={draft.status}
          disabled={disabled}
          onChange={(event) => update('status', event.target.value)}
          className={`${fieldClassName} mt-1.5`}
        >
          <option value="ENABLED">ENABLED</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </label>
    </div>
  );
}
export function CarrierMappingPanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const query = useOperationsRequest<RentalLogisticsCarrierMappingVO[]>();
  const mutation = useOperationsRequest<RentalLogisticsCarrierMappingVO | boolean>();
  const [draft, setDraft] =
    useState<RentalLogisticsCarrierMappingSaveReqVO | null>(null);
  const [deleteTarget, setDeleteTarget] =
    useState<RentalLogisticsCarrierMappingVO | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!access.canQueryMappings) return Promise.resolve(null);
    return query.run(
      () => fetchRentalLogisticsCarrierMappings(),
      (items) => items.length === 0
    );
  }, [access.canQueryMappings, query.run]);

  useEffect(() => {
    if (access.canQueryMappings) void load();
    else query.reset();
  }, [access.canQueryMappings, load, query.reset]);

  const openNew = () => {
    setSuccessMessage(null);
    setDraft(emptyCarrierMappingDraft());
  };

  const openEdit = (mapping: RentalLogisticsCarrierMappingVO) => {
    setSuccessMessage(null);
    setDraft({ ...mapping });
  };

  const submit = async () => {
    if (!draft || !access.canUpdateMappings || !carrierMappingDraftIsValid(draft)) return;
    const result = await mutation.run(() =>
      saveRentalLogisticsCarrierMapping(draft)
    );
    if (result) {
      setDraft(null);
      setSuccessMessage(operationsCopy(locale, 'mapping.saved'));
      await load();
    }
  };

  const remove = async () => {
    if (!deleteTarget || !access.canDeleteMappings) return;
    const result = await mutation.run(() =>
      deleteRentalLogisticsCarrierMapping(deleteTarget.id)
    );
    if (result) {
      setDeleteTarget(null);
      setSuccessMessage(operationsCopy(locale, 'mapping.deleted'));
      await load();
    }
  };

  const mappings = query.state.data || [];
  const isMutating = mutation.state.status === 'loading';

  return (
    <>
      <OperationsPanel
        title={operationsCopy(locale, 'mapping.title')}
        description={operationsCopy(locale, 'mapping.description')}
        actions={
          <>
            {access.canQueryMappings && (
              <button
                type="button"
                onClick={() => void load()}
                disabled={query.state.status === 'loading'}
                className={secondaryButtonClassName}
              >
                <RefreshCw className={`h-3.5 w-3.5 ${query.state.status === 'loading' ? 'animate-spin' : ''}`} />
                {operationsCopy(locale, 'common.refresh')}
              </button>
            )}
            <button
              type="button"
              onClick={openNew}
              disabled={!access.canUpdateMappings || !access.canQueryMappings}
              title={!access.canUpdateMappings ? operationsCopy(locale, 'common.noPermission') : undefined}
              className={primaryButtonClassName}
            >
              <Plus className="h-3.5 w-3.5" />
              {operationsCopy(locale, 'mapping.new')}
            </button>
          </>
        }
      >
        <PanelQueryBoundary
          allowed={access.canQueryMappings}
          state={query.state}
          locale={locale}
          onRetry={() => void load()}
          isEmpty={query.state.status === 'empty'}
          emptyTitle={operationsCopy(locale, 'mapping.empty')}
          emptyDetail={operationsCopy(locale, 'mapping.emptyDetail')}
        >
          <div className="space-y-3">
            {mutation.state.status === 'error' && (
              <OperationResultPanel
                state="error"
                message={operationsErrorCopy(locale, mutation.state.error)}
              />
            )}
            {successMessage && (
              <OperationResultPanel state="success" message={successMessage} />
            )}

            <div className="hidden overflow-x-auto sm:block">
              <table className="w-full min-w-[760px] border-collapse text-left">
                <thead>
                  <tr className="border-b border-[var(--sc-border)] text-[9px] uppercase tracking-[0.12em] text-[var(--sc-ink-muted)]">
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.source')}</th>
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.sourceCode')}</th>
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.canonical')}</th>
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.providerCode')}</th>
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.phone')}</th>
                    <th className="px-2 py-3">{operationsCopy(locale, 'mapping.state')}</th>
                    <th className="px-2 py-3 text-right">{operationsCopy(locale, 'mapping.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {mappings.map((mapping) => (
                    <tr key={mapping.id} className="border-b border-[var(--sc-border)] last:border-0">
                      <td className="px-2 py-3 text-[10px] font-bold">{mapping.sourceType}</td>
                      <td className="sc-data px-2 py-3 text-[10px]">{mapping.sourceCarrierCode}</td>
                      <td className="sc-data px-2 py-3 text-[10px]">{mapping.canonicalCarrierCode}</td>
                      <td className="sc-data px-2 py-3 text-[10px]">{mapping.providerCarrierCode}</td>
                      <td className="px-2 py-3 text-[10px]">{mapping.phoneRequirement}</td>
                      <td className="px-2 py-3">
                        <StatusBadge tone={operationsStatusTone(mapping.status)}>
                          {operationsCodeLabel(locale, mapping.status)}
                        </StatusBadge>
                      </td>
                      <td className="px-2 py-3">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openEdit(mapping)}
                            disabled={!access.canUpdateMappings}
                            aria-label={operationsCopy(locale, 'common.edit')}
                            className="grid h-10 w-10 place-items-center rounded-lg border border-[var(--sc-border)] disabled:opacity-40"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(mapping)}
                            disabled={!access.canDeleteMappings}
                            aria-label={operationsCopy(locale, 'common.delete')}
                            className="grid h-10 w-10 place-items-center rounded-lg border border-[var(--sc-border)] text-[var(--sc-red)] disabled:opacity-40"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="grid gap-3 sm:hidden">
              {mappings.map((mapping) => (
                <article key={mapping.id} className="rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-black text-[var(--sc-ink)]">{mapping.displayName}</p>
                      <p className="sc-data mt-1 text-[10px] text-[var(--sc-ink-muted)]">
                        {mapping.sourceType}:{mapping.sourceCarrierCode} → {mapping.providerCarrierCode}
                      </p>
                    </div>
                    <StatusBadge tone={operationsStatusTone(mapping.status)}>
                      {operationsCodeLabel(locale, mapping.status)}
                    </StatusBadge>
                  </div>
                  <div className="mt-3 flex gap-2">
                    <button type="button" onClick={() => openEdit(mapping)} disabled={!access.canUpdateMappings} className={secondaryButtonClassName}>
                      <Pencil className="h-3.5 w-3.5" />
                      {operationsCopy(locale, 'common.edit')}
                    </button>
                    <button type="button" onClick={() => setDeleteTarget(mapping)} disabled={!access.canDeleteMappings} className={`${secondaryButtonClassName} text-[var(--sc-red)]`}>
                      <Trash2 className="h-3.5 w-3.5" />
                      {operationsCopy(locale, 'common.delete')}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </PanelQueryBoundary>
      </OperationsPanel>

      {draft && (
        <ConfirmDialogShell
          ariaLabel={draft.id ? operationsCopy(locale, 'mapping.dialogEdit') : operationsCopy(locale, 'mapping.dialogNew')}
          closeLabel={operationsCopy(locale, 'common.close')}
          title={draft.id ? operationsCopy(locale, 'mapping.dialogEdit') : operationsCopy(locale, 'mapping.dialogNew')}
          description={operationsCopy(locale, 'mapping.dialogDescription')}
          onClose={() => setDraft(null)}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setDraft(null)} className={secondaryButtonClassName}>
                {operationsCopy(locale, 'common.cancel')}
              </button>
              <button type="button" onClick={() => void submit()} disabled={isMutating || !carrierMappingDraftIsValid(draft)} className={primaryButtonClassName}>
                {isMutating ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
                {isMutating ? operationsCopy(locale, 'common.saving') : operationsCopy(locale, 'common.save')}
              </button>
            </div>
          }
        >
          <MappingForm locale={locale} draft={draft} setDraft={setDraft} disabled={isMutating} />
          {!carrierMappingDraftIsValid(draft) && (
            <div className="mt-3">
              <OperationResultPanel state="error" message={operationsCopy(locale, 'mapping.invalid')} />
            </div>
          )}
        </ConfirmDialogShell>
      )}

      {deleteTarget && (
        <ConfirmDialogShell
          ariaLabel={operationsCopy(locale, 'mapping.deleteTitle')}
          closeLabel={operationsCopy(locale, 'common.close')}
          title={operationsCopy(locale, 'mapping.deleteTitle')}
          description={operationsCopy(locale, 'mapping.deleteDetail')}
          onClose={() => setDeleteTarget(null)}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setDeleteTarget(null)} className={secondaryButtonClassName}>
                {operationsCopy(locale, 'common.cancel')}
              </button>
              <button type="button" onClick={() => void remove()} disabled={isMutating} className={`${primaryButtonClassName} bg-[var(--sc-red)]`}>
                {isMutating ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Trash2 className="h-3.5 w-3.5" />}
                {isMutating ? operationsCopy(locale, 'common.deleting') : operationsCopy(locale, 'common.delete')}
              </button>
            </div>
          }
        >
          <p className="sc-data rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-3 text-xs">
            {deleteTarget.sourceType}:{deleteTarget.sourceCarrierCode} → {deleteTarget.providerCarrierCode}
          </p>
        </ConfirmDialogShell>
      )}
    </>
  );
}
