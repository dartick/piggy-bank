package org.xiaoheshan.piggy.bank.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xiaoheshan.common.model.PageQueryParam;
import org.xiaoheshan.common.rest.RestPageResult;
import org.xiaoheshan.common.util.BeanCopier;
import org.xiaoheshan.piggy.bank.dal.dao.user.TestUserDOMapperExt;
import org.xiaoheshan.piggy.bank.dal.dao.user.model.TestUserDO;
import org.xiaoheshan.piggy.bank.dal.dao.user.model.TestUserQueryDO;
import org.xiaoheshan.piggy.bank.web.constant.SystemConstant;
import org.xiaoheshan.piggy.bank.web.vo.TestUserVO;

import java.util.List;

/**
 * @author _Chf
 * @since 01-30-2018
 */
@RestController
@RequestMapping(SystemConstant.REST_API_V1 + "user")
@Api("用户模块")
public class TestUserController {

    @Autowired
    private TestUserDOMapperExt testUserDOMapperExt;

    @PostMapping("/list")
    @ApiOperation("分页查询用户")
    public RestPageResult<TestUserVO> list(@ApiParam("分页查询参数") @RequestBody PageQueryParam queryParam) {
        TestUserQueryDO queryDO = BeanCopier.instantiateAndCopy(TestUserQueryDO.class, queryParam);
        List<TestUserDO> testUserDOS = testUserDOMapperExt.listUserDOPage(queryDO);
        List<TestUserVO> testUserVOS = BeanCopier.copyToList(testUserDOS, TestUserVO.class);
        Long count = testUserDOMapperExt.countUserDO(queryDO);
        return RestPageResult.<TestUserVO>builder().success(testUserVOS, count).build();
    }

}
