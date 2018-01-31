package org.xiaoheshan.common.rest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author _Chf
 * @since 01-30-2018
 */
public class RestPageResult<T> implements Serializable {

    private static final long serialVersionUID = 4595415231573277173L;
    /** 返回码 */
    String                    code             = RestRetCodeEnum.SUCCESS.getCode();
    /** 列表数据 */
    List<T> data;
    /** 总数 */
    long                      count            = 0;
    /** 错误信息 */
    String                    msg;

    public static <I> RestPageResultBuilder<I> builder() {
        return new RestPageResultBuilder<>();
    }

    public static class RestPageResultBuilder<E> {
        private RestPageResult<E> result;

        public RestPageResultBuilder() {
            result = new RestPageResult<>();
        }

        public RestPageResult<E> build() {
            return result;
        }

        public RestPageResultBuilder<E> success(List<E> data, long count) {
            result.code = RestRetCodeEnum.SUCCESS.getCode();
            result.msg = RestRetCodeEnum.SUCCESS.getDesc();
            result.data = data;
            result.count = count;
            return this;
        }

        public RestPageResultBuilder<E> success() {
            result.code = RestRetCodeEnum.SUCCESS.getCode();
            result.msg = RestRetCodeEnum.SUCCESS.getCode();
            return this;
        }

        public RestPageResultBuilder<E> failed(String code, String msg) {
            result.code = code;
            result.msg = msg;
            return this;
        }

        public RestPageResultBuilder<E> failed(RestRetCodeEnum re) {
            result.code = re.getCode();
            result.msg = re.getDesc();
            return this;
        }
    }

    @Override
    public String toString() {
        Class<?> contentType = data == null ? null : data.getClass();
        String str = "Result{" + "code='" + code + '\'' + ", msg='" + msg + '\'';
        if (contentType == null) {
            return str + ", data=null}";
        } else if (Collection.class.isAssignableFrom(contentType)) {
            return str + ", data.size()=" + ((Collection<?>) data).size() + "}";
        } else {
            return str + ", data='" + data + "'}";
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

}
