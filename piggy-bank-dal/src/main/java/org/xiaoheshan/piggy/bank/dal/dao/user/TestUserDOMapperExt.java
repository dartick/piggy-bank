package org.xiaoheshan.piggy.bank.dal.dao.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.xiaoheshan.piggy.bank.dal.dao.user.model.TestUserDO;
import org.xiaoheshan.piggy.bank.dal.dao.user.model.TestUserQueryDO;

import java.util.List;

/**
 * 用户Mapper
 * 
 * @author chf
 * @since 01-30-2018
 */
@Mapper
public interface TestUserDOMapperExt extends TestUserDOMapper {

    /**
     * 分页查询用户
     */
    List<TestUserDO> listUserDOPage(TestUserQueryDO queryDO);

    /**
     * 分页查询总数
     */
    Long countUserDO(TestUserQueryDO queryDO);
}