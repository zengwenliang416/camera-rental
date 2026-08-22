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
    ErrorCode XIANYU_CONFIG_INVALID = new ErrorCode(1_040_001_014, "闲管家配置无效：{}");
    ErrorCode XIANYU_ORDER_REPARSE_BUSY = new ErrorCode(1_040_001_015, "订单同步正在运行，请稍后重试备注解析");
    ErrorCode XIANYU_WRITE_DISABLED = new ErrorCode(1_040_001_020, "闲管家写操作未启用");
    ErrorCode XIANYU_SHIP_IDEMPOTENT_KEY_REUSED = new ErrorCode(1_040_001_021, "发货幂等键已绑定其它请求");
    ErrorCode XIANYU_SHIP_REMOTE_ERROR = new ErrorCode(1_040_001_022, "闲管家发货失败：{}");
    ErrorCode XIANYU_SHIP_ORDER_NOT_PENDING = new ErrorCode(1_040_001_023, "闲鱼订单不是待发货状态");
    ErrorCode XIANYU_SHIP_ORDER_NOT_CONVERTED = new ErrorCode(1_040_001_024, "闲鱼订单尚未转换为租赁订单");
    ErrorCode XIANYU_SHIP_DEVICE_NOT_SHIPPABLE = new ErrorCode(1_040_001_025, "设备当前不可发货：{}");
    ErrorCode XIANYU_SHIP_OCR_FAILED = new ErrorCode(1_040_001_026, "未识别到有效快递单号");

    // ========== 设备分配 / 设备二维码 1-040-002-000 ==========
    ErrorCode RENTAL_DEVICE_ASSIGN_FAILED = new ErrorCode(1_040_002_000, "设备分配失败：{}");
    ErrorCode RENTAL_DEVICE_NOT_EXISTS = new ErrorCode(1_040_002_001, "租赁设备不存在");
    ErrorCode RENTAL_DEVICE_QR_INVALID = new ErrorCode(1_040_002_002, "设备二维码无效：{}");
    ErrorCode RENTAL_DEVICE_QR_MODEL_MISMATCH = new ErrorCode(1_040_002_003, "设备二维码型号与设备档案不一致");
    ErrorCode RENTAL_DEVICE_DISPATCH_FAILED = new ErrorCode(1_040_002_004, "设备出库失败：{}");
    ErrorCode RENTAL_DEVICE_RETURN_FAILED = new ErrorCode(1_040_002_005, "设备回仓失败：{}");
    ErrorCode RENTAL_DEVICE_INBOUND_FAILED = new ErrorCode(1_040_002_006, "采购入库生成设备失败：{}");
    ErrorCode RENTAL_DEVICE_CODE_INVALID = new ErrorCode(1_040_002_007,
            "机器编码格式应类似 P4-01");
    ErrorCode RENTAL_DEVICE_LOCK_NOT_EXISTS = new ErrorCode(1_040_002_008, "设备锁定记录不存在");
    ErrorCode RENTAL_DEVICE_LOCK_INVALID = new ErrorCode(1_040_002_009, "设备锁定操作无效：{}");
    ErrorCode RENTAL_DEVICE_LOCK_CONFLICT = new ErrorCode(1_040_002_010, "设备存在活动锁定：{}");
    ErrorCode RENTAL_DEVICE_UNASSIGN_FAILED = new ErrorCode(1_040_002_011, "撤销设备分配失败：{}");
    ErrorCode RENTAL_ORDER_NOT_EXISTS = new ErrorCode(1_040_002_012, "租赁订单不存在");
    ErrorCode RENTAL_ORDER_CANCEL_FAILED = new ErrorCode(1_040_002_013, "租赁订单取消失败：{}");

    // ========== 人工复核 1-040-003-000 ==========
    ErrorCode RENTAL_MANUAL_REVIEW_NOT_EXISTS = new ErrorCode(1_040_003_000, "人工复核记录不存在");
    ErrorCode RENTAL_MANUAL_REVIEW_STATUS_INVALID = new ErrorCode(1_040_003_001,
            "人工复核记录当前状态不允许处理：{}");
    ErrorCode RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED = new ErrorCode(1_040_003_002,
            "人工复核前置条件仍未解决：{}");

    // ========== 客户退回登记 1-040-004-000 ==========
    ErrorCode RETURN_REGISTRATION_NOT_AVAILABLE = new ErrorCode(1_040_004_000, "退回登记链接不可用");
    ErrorCode RETURN_REGISTRATION_ORDER_INVALID = new ErrorCode(1_040_004_001, "退回登记订单不匹配");
    ErrorCode RETURN_REGISTRATION_STATUS_INVALID = new ErrorCode(1_040_004_002, "当前登记状态不允许此操作");
    ErrorCode RETURN_REGISTRATION_ATTACHMENT_INVALID = new ErrorCode(1_040_004_003, "退回登记附件无效：{}");
    ErrorCode RETURN_REGISTRATION_SUBMISSION_INVALID = new ErrorCode(1_040_004_004, "退回登记信息无效：{}");
    ErrorCode RETURN_REGISTRATION_VERIFICATION_FAILED = new ErrorCode(1_040_004_005,
            "订单信息或机器编码不匹配");

}
