package org.xiaoheshan.piggy.bank.web.vo;

import java.util.Date;

/**
 * @author _Chf
 * @since 01-30-2018
 */
@lombok.Data
public class TestUserVO {
    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String name;

    /** 用户密码 */
    private String password;

    /** 手机号码 */
    private String mobile;

    /** 创建时间 */
    private Date gmtCreated;

    /** 修改时间 */
    private Date gmtModified;

}
