package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

public class XianyuExpressCandidateRespVO {

    /** 承运商编码（快递100 comCode，与闲管家快递公司 code 基本一致） */
    private String code;

    /** 承运商名称 */
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
