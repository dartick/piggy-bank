package org.xiaoheshan.common.model;

/**
 * @author _Chf
 * @since 01-30-2018
 */
public class PageQueryParam {

    /** 分页查询页码，从1开始 */
    private Integer pageIndex = 1;

    /** 分页查询页大小 */
    private Integer pageSize = 10;

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
