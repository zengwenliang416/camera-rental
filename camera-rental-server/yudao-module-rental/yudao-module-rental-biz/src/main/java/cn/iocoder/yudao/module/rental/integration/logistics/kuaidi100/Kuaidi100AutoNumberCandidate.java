package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

/**
 * 快递100 智能识别候选承运商。
 *
 * @param comCode 快递100 承运商编码（与闲管家快递公司 code 基本一致，如 shunfeng）
 * @param name    承运商名称（如 顺丰速运）
 */
public record Kuaidi100AutoNumberCandidate(String comCode, String name) {
}
