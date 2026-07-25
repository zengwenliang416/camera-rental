package cn.iocoder.yudao.module.rental.integration.xianyu.client;

/**
 * Safe error boundary for XianGuanJia transport. Messages never include raw
 * payloads, authorization headers, signatures, or secrets.
 */
public class XianyuClientException extends RuntimeException {

    private final Kind kind;
    private final Integer httpStatus;
    private final Integer remoteCode;

    public XianyuClientException(Kind kind, String message) {
        this(kind, message, null, null, null);
    }

    public XianyuClientException(Kind kind, String message, Throwable cause) {
        this(kind, message, null, null, cause);
    }

    public XianyuClientException(Kind kind, String message, Integer httpStatus, Integer remoteCode) {
        this(kind, message, httpStatus, remoteCode, null);
    }

    private XianyuClientException(Kind kind, String message, Integer httpStatus, Integer remoteCode, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
        this.remoteCode = remoteCode;
    }

    public Kind getKind() {
        return kind;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Integer getRemoteCode() {
        return remoteCode;
    }

    public enum Kind {
        INTEGRATION_DISABLED,
        MISSING_CREDENTIALS,
        MALFORMED_REQUEST,
        TRANSPORT,
        HTTP_STATUS,
        MALFORMED_RESPONSE,
        REMOTE_RESPONSE
    }

}
