import {
  KeyRound,
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  ShieldCheck,
  Trash2,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';

import {
  deleteRentalLogisticsProviderCredential,
  fetchRentalLogisticsProviderConfig,
  saveRentalLogisticsProviderConfig,
  saveRentalLogisticsProviderCredential,
  verifyRentalLogisticsProviderConfig,
  verifyRentalLogisticsProviderCredential,
  type RentalLogisticsProviderConfigVO,
  type RentalLogisticsProviderCredentialVO,
  type RentalLogisticsSecretAction,
} from '../../../api/rental';
import { Button } from '../../../shared/ui/Button';
import { OperationResultPanel } from '../../../shared/ui/OperationResultPanel';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { LocalePreference } from '../../preferences/preferenceModel';
import {
  operationsCopy,
  operationsErrorCopy,
  formatOperationsDateTime,
  operationsCodeLabel,
} from '../operationsCopy';
import {
  emptyProviderCredentialDraft,
  operationsStatusTone,
  providerCredentialDraftFromConfig,
  providerCredentialDraftIsValid,
  providerCredentialPayload,
  providerDraftFromConfig,
  providerDraftIsValid,
  providerUpdatePayload,
  type LogisticsOperationsAccess,
  type ProviderConfigDraft,
  type ProviderCredentialDraft,
  type SecretDraft,
} from '../operationsModel';
import { useOperationsRequest } from '../useOperationsRequest';
import {
  fieldClassName,
  OperationsPanel,
  PanelQueryBoundary,
} from './OperationsPanel';

function ToggleField({
  checked,
  disabled,
  label,
  onChange,
}: {
  checked: boolean;
  disabled: boolean;
  label: string;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="sc-glass-control flex min-h-12 items-center justify-between gap-3 rounded-xl px-3">
      <span className="text-[11px] font-bold text-[var(--sc-ink-soft)]">{label}</span>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 accent-[var(--sc-blue)]"
      />
    </label>
  );
}

function SecretField({
  label,
  configured,
  draft,
  disabled,
  locale,
  onChange,
}: {
  label: string;
  configured: boolean;
  draft: SecretDraft;
  disabled: boolean;
  locale: LocalePreference;
  onChange: (draft: SecretDraft) => void;
}) {
  const actions: RentalLogisticsSecretAction[] = ['KEEP', 'REPLACE', 'CLEAR'];
  return (
    <div className="sc-workspace-card rounded-xl p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-[11px] font-black text-[var(--sc-ink)]">
          <KeyRound className="h-3.5 w-3.5 text-[var(--sc-blue)]" />
          {label}
        </span>
        <StatusBadge tone={configured ? 'green' : 'amber'}>
          {operationsCopy(
            locale,
            configured
              ? 'provider.secret.configured'
              : 'provider.secret.missing'
          )}
        </StatusBadge>
      </div>
      <div className="mt-3 grid gap-2 sm:grid-cols-[minmax(0,11rem)_minmax(0,1fr)]">
        <select
          value={draft.action}
          disabled={disabled}
          onChange={(event) =>
            onChange({
              action: event.target.value as RentalLogisticsSecretAction,
              replacement: '',
            })}
          className={fieldClassName}
        >
          {actions.map((action) => (
            <option key={action} value={action}>
              {operationsCopy(
                locale,
                action === 'KEEP'
                  ? 'provider.secret.keep'
                  : action === 'REPLACE'
                    ? 'provider.secret.replace'
                    : 'provider.secret.clear'
              )}
            </option>
          ))}
        </select>
        {draft.action === 'REPLACE' && (
          <input
            type="password"
            value={draft.replacement}
            disabled={disabled}
            autoComplete="new-password"
            onChange={(event) =>
              onChange({ ...draft, replacement: event.target.value })}
            placeholder={operationsCopy(locale, 'provider.secret.placeholder')}
            className={fieldClassName}
          />
        )}
      </div>
    </div>
  );
}

export function ProviderConfigPanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const query = useOperationsRequest<RentalLogisticsProviderConfigVO>();
  const save = useOperationsRequest<RentalLogisticsProviderConfigVO>();
  const credentialSave = useOperationsRequest<RentalLogisticsProviderCredentialVO>();
  const credentialDelete = useOperationsRequest<boolean>();
  const credentialVerify = useOperationsRequest<{
    valid: boolean;
    reason: string;
    verifiedAt?: string | null;
  }>();
  const verify = useOperationsRequest<{
    valid: boolean;
    reason: string;
    verifiedAt?: string | null;
  }>();
  const [draft, setDraft] = useState<ProviderConfigDraft | null>(null);
  const [credentialDraft, setCredentialDraft] =
    useState<ProviderCredentialDraft | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!access.canQueryConfig) return Promise.resolve(null);
    return query.run(() => fetchRentalLogisticsProviderConfig());
  }, [access.canQueryConfig, query.run]);

  useEffect(() => {
    if (access.canQueryConfig) void load();
    else query.reset();
  }, [access.canQueryConfig, load, query.reset]);

  useEffect(() => {
    if (query.state.data) {
      setDraft(providerDraftFromConfig(query.state.data));
    }
  }, [query.state.data]);

  const setDraftValue = <K extends keyof ProviderConfigDraft>(
    key: K,
    value: ProviderConfigDraft[K]
  ) => {
    setDraft((current) => (current ? { ...current, [key]: value } : current));
    setSuccessMessage(null);
  };

  const submit = async () => {
    if (!draft || !providerDraftIsValid(draft) || !access.canUpdateConfig) return;
    setSuccessMessage(null);
    const result = await save.run(() =>
      saveRentalLogisticsProviderConfig(providerUpdatePayload(draft))
    );
    if (result) {
      setDraft(providerDraftFromConfig(result));
      setSuccessMessage(operationsCopy(locale, 'provider.saved'));
      await load();
    }
  };

  const runVerify = async () => {
    if (!access.canVerifyConfig) return;
    setSuccessMessage(null);
    const result = await verify.run(() =>
      verifyRentalLogisticsProviderConfig()
    );
    if (result) {
      setSuccessMessage(
        operationsCopy(
          locale,
          result.valid ? 'provider.verified' : 'provider.verifyFailed'
        )
      );
      await load();
    }
  };

  const saveCredential = async () => {
    if (!credentialDraft
      || !providerCredentialDraftIsValid(credentialDraft)
      || !access.canUpdateConfig) return;
    const result = await credentialSave.run(() =>
      saveRentalLogisticsProviderCredential(
        providerCredentialPayload(credentialDraft)
      )
    );
    if (result) {
      setCredentialDraft(null);
      setSuccessMessage(operationsCopy(locale, 'provider.credential.saved'));
      await load();
    }
  };

  const deleteCredential = async (credential: RentalLogisticsProviderCredentialVO) => {
    if (!access.canUpdateConfig
      || !window.confirm(operationsCopy(locale, 'provider.credential.deleteConfirm', {
        name: credential.credentialName,
      }))) return;
    const result = await credentialDelete.run(() =>
      deleteRentalLogisticsProviderCredential(credential.id)
    );
    if (result) {
      setCredentialDraft(null);
      setSuccessMessage(operationsCopy(locale, 'provider.credential.deleted'));
      await load();
    }
  };

  const verifyCredential = async (credentialId: number) => {
    if (!access.canVerifyConfig) return;
    const result = await credentialVerify.run(() =>
      verifyRentalLogisticsProviderCredential(credentialId)
    );
    if (result) {
      setSuccessMessage(operationsCopy(
        locale,
        result.valid
          ? 'provider.credential.verified'
          : 'provider.credential.verifyFailed'
      ));
      await load();
    }
  };

  const config = query.state.data;
  const disabled =
    !access.canUpdateConfig || save.state.status === 'loading' || !draft;

  return (
    <OperationsPanel
      title={operationsCopy(locale, 'provider.title')}
      description={operationsCopy(locale, 'provider.description')}
      actions={
        access.canQueryConfig ? (
          <Button
            variant="outline"
            size="md"
            type="button"
            onClick={() => void load()}
            disabled={query.state.status === 'loading'}
            icon={<RefreshCw className={`h-3.5 w-3.5 ${query.state.status === 'loading' ? 'animate-spin' : ''}`} />}
          >
            {operationsCopy(locale, 'common.refresh')}
          </Button>
        ) : undefined
      }
    >
      <PanelQueryBoundary
        allowed={access.canQueryConfig}
        state={query.state}
        locale={locale}
        onRetry={() => void load()}
      >
        {config && draft && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge tone={operationsStatusTone(config.configStatus)}>
                {operationsCodeLabel(locale, config.configStatus)}
              </StatusBadge>
              <span className="text-[10px] text-[var(--sc-ink-muted)]">
                {operationsCopy(locale, 'provider.lastVerified')}: {' '}
                {formatOperationsDateTime(locale, config.lastVerifiedAt)
                  || operationsCopy(locale, 'provider.neverVerified')}
              </span>
            </div>

            <div className="grid gap-2 sm:grid-cols-3">
              <ToggleField
                checked={draft.enabled}
                disabled={disabled}
                label={operationsCopy(locale, 'provider.enabled')}
                onChange={(value) => setDraftValue('enabled', value)}
              />
              <ToggleField
                checked={draft.queryEnabled}
                disabled={disabled}
                label={operationsCopy(locale, 'provider.queryEnabled')}
                onChange={(value) => setDraftValue('queryEnabled', value)}
              />
              <ToggleField
                checked={draft.subscribeEnabled}
                disabled={disabled}
                label={operationsCopy(locale, 'provider.subscribeEnabled')}
                onChange={(value) => setDraftValue('subscribeEnabled', value)}
              />
            </div>

            <div className="grid gap-3 lg:grid-cols-2">
              <label className="sc-field-label">
                {operationsCopy(locale, 'provider.callbackBaseUrl')}
                <input
                  value={draft.callbackBaseUrl}
                  disabled={disabled}
                  onChange={(event) =>
                    setDraftValue('callbackBaseUrl', event.target.value)}
                  className={`${fieldClassName} mt-1.5`}
                  placeholder="https://api.example.com/rental/webhooks"
                />
              </label>
              <div className="grid grid-cols-2 gap-3">
                <label className="sc-field-label">
                  {operationsCopy(locale, 'provider.minimumInterval')}
                  <input
                    type="number"
                    min={1800}
                    max={86400}
                    value={draft.minimumQueryIntervalSeconds}
                    disabled={disabled}
                    onChange={(event) =>
                      setDraftValue(
                        'minimumQueryIntervalSeconds',
                        Number(event.target.value)
                      )}
                    className={`${fieldClassName} mt-1.5`}
                  />
                </label>
                <label className="sc-field-label">
                  {operationsCopy(locale, 'provider.resultVersion')}
                  <input
                    value={draft.resultVersion}
                    disabled={disabled}
                    onChange={(event) =>
                      setDraftValue('resultVersion', event.target.value)}
                    className={`${fieldClassName} mt-1.5`}
                  />
                </label>
              </div>
            </div>

            <div className="grid gap-3">
              <SecretField
                label={operationsCopy(locale, 'provider.secret.callbackSecret')}
                configured={config.callbackSecretConfigured}
                draft={draft.callbackSecret}
                disabled={disabled}
                locale={locale}
                onChange={(value) => setDraftValue('callbackSecret', value)}
              />
            </div>

            <div className="sc-workspace-card rounded-2xl p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-xs font-black text-[var(--sc-ink)]">
                    {operationsCopy(locale, 'provider.credentials.title')}
                  </div>
                  <div className="mt-1 text-[10px] leading-5 text-[var(--sc-ink-muted)]">
                    {operationsCopy(locale, 'provider.credentials.description')}
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="md"
                  type="button"
                  disabled={!access.canUpdateConfig}
                  onClick={() => setCredentialDraft(emptyProviderCredentialDraft())}
                  icon={<Plus className="h-3.5 w-3.5" />}
                >
                  {operationsCopy(locale, 'provider.credential.new')}
                </Button>
              </div>

              <div className="mt-3 grid gap-2">
                {config.credentials.length === 0 && (
                  <div className="rounded-lg border border-dashed border-[var(--sc-border)] px-3 py-5 text-center text-[11px] text-[var(--sc-ink-muted)]">
                    {operationsCopy(locale, 'provider.credentials.empty')}
                  </div>
                )}
                {config.credentials.map((credential) => (
                  <div
                    key={credential.id}
                    className="sc-glass-control flex flex-wrap items-center justify-between gap-3 rounded-xl px-3 py-3"
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[11px] font-black text-[var(--sc-ink)]">
                          {credential.credentialName}
                        </span>
                        <StatusBadge tone={credential.enabled ? 'green' : 'neutral'}>
                          {operationsCopy(
                            locale,
                            credential.enabled
                              ? 'provider.credential.enabled'
                              : 'provider.credential.disabled'
                          )}
                        </StatusBadge>
                        <StatusBadge tone={operationsStatusTone(credential.configStatus)}>
                          {operationsCodeLabel(locale, credential.configStatus)}
                        </StatusBadge>
                      </div>
                      <div className="mt-1 text-[10px] text-[var(--sc-ink-muted)]">
                        {operationsCopy(locale, 'provider.credential.order')}: {credential.sortOrder}
                        {' · '}
                        {operationsCopy(locale, 'provider.credential.secretState', {
                          customer: credential.customerCodeConfigured ? '✓' : '×',
                          key: credential.apiKeyConfigured ? '✓' : '×',
                        })}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        type="button"
                        disabled={!access.canVerifyConfig}
                        onClick={() => void verifyCredential(credential.id)}
                        icon={<ShieldCheck className="h-3.5 w-3.5" />}
                      >
                        {operationsCopy(locale, 'provider.verify')}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        type="button"
                        disabled={!access.canUpdateConfig}
                        onClick={() =>
                          setCredentialDraft(providerCredentialDraftFromConfig(credential))}
                        icon={<Pencil className="h-3.5 w-3.5" />}
                      >
                        {operationsCopy(locale, 'common.edit')}
                      </Button>
                      <Button
                        variant="danger"
                        size="sm"
                        type="button"
                        disabled={!access.canUpdateConfig}
                        onClick={() => void deleteCredential(credential)}
                        icon={<Trash2 className="h-3.5 w-3.5" />}
                      >
                        {operationsCopy(locale, 'common.delete')}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>

              {credentialDraft && (
                <div className="sc-overlay-surface mt-4 space-y-3 rounded-xl p-3">
                  <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_8rem]">
                    <label className="sc-field-label">
                      {operationsCopy(locale, 'provider.credential.name')}
                      <input
                        value={credentialDraft.credentialName}
                        disabled={!access.canUpdateConfig}
                        onChange={(event) => setCredentialDraft((current) =>
                          current ? { ...current, credentialName: event.target.value } : current)}
                        className={`${fieldClassName} mt-1.5`}
                      />
                    </label>
                    <label className="sc-field-label">
                      {operationsCopy(locale, 'provider.credential.order')}
                      <input
                        type="number"
                        min={0}
                        max={10000}
                        value={credentialDraft.sortOrder}
                        disabled={!access.canUpdateConfig}
                        onChange={(event) => setCredentialDraft((current) =>
                          current ? { ...current, sortOrder: Number(event.target.value) } : current)}
                        className={`${fieldClassName} mt-1.5`}
                      />
                    </label>
                  </div>
                  <ToggleField
                    checked={credentialDraft.enabled}
                    disabled={!access.canUpdateConfig}
                    label={operationsCopy(locale, 'provider.credential.enabled')}
                    onChange={(enabled) => setCredentialDraft((current) =>
                      current ? { ...current, enabled } : current)}
                  />
                  <SecretField
                    label={operationsCopy(locale, 'provider.secret.customerCode')}
                    configured={config.credentials.find(
                      (item) => item.id === credentialDraft.id
                    )?.customerCodeConfigured ?? false}
                    draft={credentialDraft.customerCode}
                    disabled={!access.canUpdateConfig}
                    locale={locale}
                    onChange={(customerCode) => setCredentialDraft((current) =>
                      current ? { ...current, customerCode } : current)}
                  />
                  <SecretField
                    label={operationsCopy(locale, 'provider.secret.apiKey')}
                    configured={config.credentials.find(
                      (item) => item.id === credentialDraft.id
                    )?.apiKeyConfigured ?? false}
                    draft={credentialDraft.apiKey}
                    disabled={!access.canUpdateConfig}
                    locale={locale}
                    onChange={(apiKey) => setCredentialDraft((current) =>
                      current ? { ...current, apiKey } : current)}
                  />
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      size="md"
                      type="button"
                      onClick={() => setCredentialDraft(null)}
                    >
                      {operationsCopy(locale, 'common.cancel')}
                    </Button>
                    <Button
                      variant="primary"
                      size="md"
                      type="button"
                      disabled={!providerCredentialDraftIsValid(credentialDraft)
                        || credentialSave.state.status === 'loading'}
                      onClick={() => void saveCredential()}
                      icon={credentialSave.state.status === 'loading'
                        ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                        : <Save className="h-3.5 w-3.5" />}
                    >
                      {operationsCopy(locale, 'common.save')}
                    </Button>
                  </div>
                </div>
              )}
            </div>

            {!providerDraftIsValid(draft) && (
              <OperationResultPanel
                state="error"
                message={operationsCopy(locale, 'provider.invalid')}
              />
            )}
            {save.state.status === 'error' && (
              <OperationResultPanel
                state="error"
                message={operationsErrorCopy(locale, save.state.error)}
              />
            )}
            {verify.state.status === 'error' && (
              <OperationResultPanel
                state="error"
                message={operationsErrorCopy(locale, verify.state.error)}
              />
            )}
            {(credentialSave.state.status === 'error'
              || credentialDelete.state.status === 'error'
              || credentialVerify.state.status === 'error') && (
              <OperationResultPanel
                state="error"
                message={operationsCopy(locale, 'provider.credential.operationFailed')}
              />
            )}
            {successMessage && (
              <OperationResultPanel state="success" message={successMessage} />
            )}

            <div className="flex flex-wrap justify-end gap-2">
              <Button
                variant="outline"
                size="md"
                type="button"
                onClick={() => void runVerify()}
                disabled={!access.canVerifyConfig || verify.state.status === 'loading'}
                title={!access.canVerifyConfig ? operationsCopy(locale, 'common.noPermission') : undefined}
                icon={verify.state.status === 'loading'
                  ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                  : <ShieldCheck className="h-3.5 w-3.5" />}
              >
                {verify.state.status === 'loading'
                  ? operationsCopy(locale, 'provider.verifying')
                  : operationsCopy(locale, 'provider.verify')}
              </Button>
              <Button
                variant="primary"
                size="md"
                type="button"
                onClick={() => void submit()}
                disabled={
                  disabled
                  || !providerDraftIsValid(draft)
                  || save.state.status === 'loading'
                }
                title={!access.canUpdateConfig ? operationsCopy(locale, 'common.noPermission') : undefined}
                icon={save.state.status === 'loading'
                  ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                  : <Save className="h-3.5 w-3.5" />}
              >
                {save.state.status === 'loading'
                  ? operationsCopy(locale, 'common.saving')
                  : operationsCopy(locale, 'common.save')}
              </Button>
            </div>
          </div>
        )}
      </PanelQueryBoundary>
    </OperationsPanel>
  );
}
