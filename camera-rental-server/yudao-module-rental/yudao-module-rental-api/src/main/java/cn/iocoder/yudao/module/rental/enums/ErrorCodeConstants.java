package cn.iocoder.yudao.module.rental.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 租赁模块错误码，使用 1-040-000-000 段。
 */
public interface ErrorCodeConstants {

    // ========== 闲管家集成 1-040-001-000 ==========
    ErrorCode XIANYU_INTEGRATION_DISABLED = new ErrorCode(1_040_001_000, "闲管家集成未启用");
    ErrorCode XIANYU_CREDENTIALS_MISSING = new ErrorCode(1_040_001_001, "闲管家运行时凭据未配置");
    ErrorCode XIANYU_SHOP_NOT_EXISTS = new ErrorCode(1_040_001_002, "闲鱼店铺不存在");
    ErrorCode XIANYU_SHOP_AUTHORIZE_MISSING = new ErrorCode(1_040_001_003, "闲鱼店铺缺少授权ID，请先同步授权店铺");
    ErrorCode XIANYU_ORDER_SYNC_FAILED = new ErrorCode(1_040_001_004, "闲鱼订单同步失败：{}");
    ErrorCode XIANYU_REMOTE_ERROR = new ErrorCode(1_040_001_005, "闲管家接口调用失败");
    ErrorCode XIANYU_SHOP_AUTHORIZATION_INVALID = new ErrorCode(1_040_001_006, "闲鱼店铺授权无效或已过期，请先同步授权店铺");
    ErrorCode XIANYU_AFTER_SALE_SYNC_FAILED = new ErrorCode(1_040_001_007, "闲鱼售后同步失败：{}");
    ErrorCode XIANYU_ALERT_NOT_EXISTS = new ErrorCode(1_040_001_008, "闲鱼告警不存在");
    ErrorCode XIANYU_RAW_PAYLOAD_NOT_EXISTS = new ErrorCode(1_040_001_009, "闲管家原始载荷不存在");
    ErrorCode XIANYU_PUSH_EVENT_NOT_EXISTS = new ErrorCode(1_040_001_010, "闲管家推送事件不存在");
    ErrorCode XIANYU_RAW_PAYLOAD_REPLAY_UNSUPPORTED = new ErrorCode(1_040_001_011,
            "闲管家原始载荷类型不支持重放：{}");
    ErrorCode XIANYU_PRODUCT_SYNC_FAILED = new ErrorCode(1_040_001_012, "闲鱼商品同步失败：{}");
    ErrorCode XIANYU_ORDER_NOT_EXISTS = new ErrorCode(1_040_001_013, "闲鱼渠道订单不存在");

    // ========== 设备分配 1-040-002-000 ==========
    ErrorCode RENTAL_DEVICE_ASSIGN_FAILED = new ErrorCode(1_040_002_000, "设备分配失败：{}");

    // ========== 人工复核 1-040-003-000 ==========
    ErrorCode RENTAL_MANUAL_REVIEW_NOT_EXISTS = new ErrorCode(1_040_003_000, "人工复核记录不存在");
    ErrorCode RENTAL_MANUAL_REVIEW_STATUS_INVALID = new ErrorCode(1_040_003_001,
            "人工复核记录当前状态不允许处理：{}");
    ErrorCode RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED = new ErrorCode(1_040_003_002,
            "人工复核前置条件仍未解决：{}");

}
