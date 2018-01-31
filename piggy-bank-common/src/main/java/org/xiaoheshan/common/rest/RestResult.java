package org.xiaoheshan.common.rest;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author _Chf
 * @since 01-30-2018
 */
public class RestResult<T> implements Serializable {

    private final static long serialVersionUID = 6725048659996675209L;
    // 错误code
    String                    code             = RestRetCodeEnum.SUCCESS.getCode();
    // 数据
    T                         data             = null;
    // 错误信息
    String                    msg              = null;

    public static <I> RestResultBuilder<I> builder() {
        return new RestResultBuilder<>();
    }

    public static class RestResultBuilder<E> {
        private RestResult<E> result;

        public RestResultBuilder() {
            result = new RestResult<>();
        }

        public RestResult<E> build() {
            return result;
        }

        public RestResultBuilder<E> success(E data) {
            result.code = RestRetCodeEnum.SUCCESS.getCode();
            result.msg = RestRetCodeEnum.SUCCESS.getDesc();
            result.data = data;
            return this;
        }

        public RestResultBuilder<E> success() {
            result.code = RestRetCodeEnum.SUCCESS.getCode();
            result.msg = RestRetCodeEnum.SUCCESS.getCode();
            return this;
        }

        public RestResultBuilder<E> failed(String code, String msg) {
            result.code = code;
            result.msg = msg;
            return this;
        }

        public RestResultBuilder<E> failed(RestRetCodeEnum re) {
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
