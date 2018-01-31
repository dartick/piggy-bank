package org.xiaoheshan.piggy.bank.dal.dao.user.model;

import java.util.Date;

/**
 * 用户表DO
 * 
 * @author chf
 * @since 01-31-2018
 */
@lombok.Data
public class TestUserDO {
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