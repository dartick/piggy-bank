package org.xiaoheshan.piggy.bank.dal.dao.user;

import org.xiaoheshan.piggy.bank.dal.dao.user.model.TestUserDO;

public interface TestUserDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TestUserDO record);

    int insertSelective(TestUserDO record);

    TestUserDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TestUserDO record);

    int updateByPrimaryKey(TestUserDO record);
}