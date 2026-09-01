#!/usr/bin/env bash
set -u

case_id="${1:-}"
assertion_ids="${SPECNAV_VERIFICATION_ASSERTION_IDS:-}"
result_file="${SPECNAV_VERIFICATION_ASSERTION_RESULT_FILE:-}"
artifact_root="${SPECNAV_REPORT_ARTIFACT_ROOT:-}"
run_id="${SPECNAV_REPORT_ARTIFACT_RUN_ID:-}"

fail() {
  printf '%s\n' "$*" >&2
  exit 2
}

[[ -n "$case_id" ]] || fail "case id is required"
[[ -n "$assertion_ids" ]] || fail "SPECNAV_VERIFICATION_ASSERTION_IDS is required"
[[ -n "$result_file" ]] || fail "SPECNAV_VERIFICATION_ASSERTION_RESULT_FILE is required"
[[ -n "$artifact_root" ]] || fail "SPECNAV_REPORT_ARTIFACT_ROOT is required"
[[ -n "$run_id" ]] || fail "SPECNAV_REPORT_ARTIFACT_RUN_ID is required"
[[ "$assertion_ids" != *,* ]] || fail "exactly one assertion id is supported"

expected=""
test_selector=""
case "$case_id" in
  CASE-002-identifier-authority)
    assertion_id="CASE-002-identifier-authority-ASSERT"
    expected="标识保持字符串和明确所有权；缺失或歧义值为空或失败关闭，不借用其他标识或文本推断。"
    test_selector="XianyuProductPersistenceServiceTest#shouldMatchOnlyTheExactShopUserName+shouldRejectMissingOrAmbiguousOwnedPublishItemBeforeWriting+shouldPersistNullItemIdWhenTheExactShopEntryHasNoItemId,XianyuProductSkuPersistenceServiceTest#shouldPersistRawBeforeNormalizedSkus+shouldPreserveExistingXianyuSkuIdWhenTheLatestPayloadOmitsIt,XianyuProductSkuPayloadParserTest#shouldParseDocumentedSkuPayload"
    ;;
  CASE-003-immediate-order)
    assertion_id="CASE-003-immediate-order-ASSERT"
    expected="同一店铺和外部订单号只关联一个订单与明细，pay_amount 不变，未 READY 前不能分配或排期。"
    test_selector="RentalChannelOrderReconciliationServiceTest#createsInternalOrderImmediatelyWhenModelAndRemarkAreMissing+linksExistingSourceOrderAsUpdatedInsteadOfCreated+statefulRetryCreatesExactlyOneOrderAndOneItem,RentalOrderPreparationPolicyTest#requiresCompleteReadySnapshotBeforeAssignment"
    ;;
  CASE-004-exact-model-mapping)
    assertion_id="CASE-004-exact-model-mapping-ASSERT"
    expected="只有精确启用关系可补全型号；多型号任何缺失或不匹配都保持待配置，绝不回退商品默认型号。"
    test_selector="RentalChannelProductRuleServiceTest#createsSingleModelRuleFromExactShopAndItem+createsMultiRuleOnlyFromSynchronizedSkuOwnership+rejectsSkuFromAnotherProductBeforeRuleMutation+rejectsUnsynchronizedItemWithoutIdentifierFallback,RentalChannelOrderReconciliationServiceTest#multiModelRequiresExactXgjSkuAndNeverFallsBackToProductModel+multiModelUsesOnlyTheExactEnabledSkuMapping+derivesXianyuSkuOnlyWhenTheSynchronizedXgjProductAlsoMatches"
    ;;
  CASE-005-config-skipped)
    assertion_id="CASE-005-config-skipped-ASSERT"
    expected="仅目标店铺的新订单跳过备注、复核、内部订单和排期，同时保留原始载荷与 pay_amount；历史履约不逆转。"
    test_selector="RentalChannelProductRuleServiceTest#skippedRuleClearsModelConfigurationAndStillReturnsImpact,RentalChannelOrderReconciliationServiceTest#skipsConfiguredProductWithoutRemarkReviewOrInternalOrder+configurationSkippedLookupIsExactAndDisabledRulesDoNotMatch+skipRuleNeverClearsAnExistingInternalOrderModel,XianyuOrderPersistenceServiceImplTest#shouldSkipRemarkParsingAndInternalOrderPreparationForConfiguredProduct+shouldNotParseOrRecordOlderSnapshotForConfiguredSkippedOrder"
    ;;
  CASE-006-later-valid-update)
    assertion_id="CASE-006-later-valid-update-ASSERT"
    expected="订单和明细 ID 不变；有效计划和型号可补全，无效结果留历史但不清空上次有效计划。"
    test_selector="RentalChannelOrderReconciliationServiceTest#reusesOneInternalOrderAndBecomesReadyAfterSingleModelAndRemarkArrive+configuredModelWithoutRemarkWaitsForRemarkAndPersistsModelImmediately,XianyuOrderPersistenceServiceImplTest#shouldRetainLastEffectivePlanWhenLaterRemarkIsIncomplete+shouldRecordEverySuccessfulRemarkUpdateAgainstTheSameOrder+shouldReplacePendingRentalPeriodWhenLaterDetailContainsCompleteRemark,RentalOrderPreparationPolicyTest#preservesReadyStateWhenLatestRemarkFailsAfterAnEffectivePlan"
    ;;
  CASE-007-fulfillment-protection)
    assertion_id="CASE-007-fulfillment-protection-ASSERT"
    expected="允许的计划变更经过锁与冲突检查；早退不提前释放；受保护事实保持不变并进入明确人工复核。"
    test_selector="RentalFulfillmentUpdateGuardTest#extendsAssignedScheduleWithoutConflict+preservesAssignedPlanWhenExtensionConflicts+preservesDispatchedPlanWhenExtensionConflicts+earlyReturnOnlyUpdatesExpectedSendBackDate+returnedAndInspectedAssignmentIsImmutable+settledOrderIsImmutableWithoutLockingAssignments+assignedModelMustMatchConfigurationItemAndPhysicalDevice+activeDeviceLockRequiresReview+locksDevicesAssignmentsAndSchedulesInStableOrder,RentalRemarkPlanChangeClassifierTest"
    ;;
  CASE-008-historical-reconciliation)
    assertion_id="CASE-008-historical-reconciliation-ASSERT"
    expected="游标和租约安全，计数准确，失败可恢复，重复执行不重复建单，冲突保留历史且不删除记录。"
    test_selector="RentalHistoricalOrderBackfillServiceIntegrationTest,RentalChannelOrderReconciliationWorkerTest#itemScopeUsesAscendingCursorUntilEveryBatchIsProcessed+oneOrderFailureDoesNotBlockLaterCandidates+trackedRunPersistsAllOutcomeCounters+trackedRunKeepsCompletedBatchCountersWhenLaterQueryFails,RentalChannelReconciliationRunServiceTest#activeRuleRunBlocksAnotherMutationWithinCurrentTenant+transitionsPersistCountersAndTerminalStatuses"
    ;;
  *)
    fail "unsupported command case: $case_id"
    ;;
esac

[[ "$assertion_ids" == "$assertion_id" ]] || fail "assertion id mismatch"

repo_root="$(cd "$(dirname "$0")/../../../../.." && pwd)"
server_root="$repo_root/camera-rental-server"
maven_bin="/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn"
maven_repo="/Volumes/zwl/maven-repository"
[[ -x "$maven_bin" ]] || fail "Maven executable is unavailable: $maven_bin"
[[ -d "$server_root" ]] || fail "server repository is unavailable: $server_root"

mkdir -p "$(dirname "$result_file")" "$artifact_root/$run_id"
log_file="$artifact_root/$run_id/${case_id}.log"

if [[ "${SPECNAV_RUNNER_DRY_RUN:-}" == "1" ]]; then
  command_status=0
  printf 'DRY RUN: %s\n' "$test_selector" >"$log_file"
else
  (
    cd "$server_root"
    "$maven_bin" \
      -Dmaven.repo.local="$maven_repo" \
      -pl yudao-module-rental/yudao-module-rental-biz \
      -am \
      -DskipITs=false \
      -Dsurefire.failIfNoSpecifiedTests=false \
      -Dtest="$test_selector" \
      test
  ) 2>&1 | tee "$log_file"
  command_status=${PIPESTATUS[0]}
fi

if [[ "$command_status" -eq 0 ]]; then
  actual="$expected"
  status="passed"
else
  actual="聚焦 Maven 测试失败，退出码 ${command_status}；详见命令输出。"
  status="failed"
fi

node_bin="/Users/wenliang_zeng/.nvm/versions/node/v22.19.0/bin/node"
[[ -x "$node_bin" ]] || fail "Node.js executable is unavailable"
"$node_bin" -e '
  const fs = require("node:fs");
  const [file, assertionId, actual, expected, status] = process.argv.slice(1);
  fs.appendFileSync(file, `${JSON.stringify({
    assertion_id: assertionId,
    method: "equal",
    actual,
    expected,
    status
  })}\n`);
' "$result_file" "$assertion_id" "$actual" "$expected" "$status"

exit "$command_status"
