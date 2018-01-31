package org.xiaoheshan.common.model;

/**
 * @author _Chf
 * @since 01-31-2018
 */
public class PageQueryDO {

    /** 分页查询页码，从1开始 */
    private Integer pageIndex;

    /** 分页查询页大小 */
    private Integer pageSize;

    /** 数据库查询偏移量 */
    private Integer offset = 0;

    /** 数据库查询返回行数 */
    private Integer rows = 10;

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

    public Integer getOffset() {
        if (pageIndex > 0 && pageSize > 0) {
            return (pageIndex - 1) * pageSize;
        }
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getRows() {
        if (pageSize > 0) {
            return pageSize;
        }
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }
}
