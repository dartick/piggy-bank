package org.xiaoheshan.common.rest;

/**
 * @author _Chf
 * @since 01-30-2018
 */
public enum RestRetCodeEnum {

    /** 正常状态, 请求被正确处理,并且获得预期的结果 */
    SUCCESS("0", "成功"),
    /** 失败 */
    FAIL("-1", "失败"),
    /** 业务异常 */
    BUSINESS_ERROR("-100001", "业务异常"),
    /** 非法APP */
    ILLEGAL_APP("-200001", "非法APP，没有权限"),
    /** 身份未验证 */
    UNAUTHORIZED("-200002", "身份未验证"),
    /** 非法身份 */
    ILLEGAL_AUTHORIZATION("-200004", "非法身份"),
    /**客户端版本号过低**/
    CLIENT_VERSION_LOW("-300001", "客户端版本号过低"),
    /**不支持的请求类型**/
    NOT_SUPPORT("-300002", "不支持的请求类型"),
    /**密钥异常**/
    CRYPT_KEYS_ERROR("-300003", "密钥异常"),
    /** 非法请求参数 */
    ILLEGAL_ARGUMENT("-900001", "非法请求参数"),
    /** 内部异常 */
    SYSTEM_ERROR("-999999", "系统异常"),

    ;
    /** 图形验证码风险标识 */
    public final static int INSECURITY_IMG_FLAG    = 1000000;
    /** 用户操作冻结标识*/
    public final static int INSECURITY_FREEZE_FLAG = 2000000;

    RestRetCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
