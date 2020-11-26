package xyz.liut.bingwallpaper.http;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Create by liut
 * 2020/11/2
 */
public class Response<T> {

    private int responseCode;

    private Map<String, List<String>> headers;

    private T body;

    private HttpException error;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public HttpException getError() {
        return error;
    }

    public void setError(HttpException error) {
        this.error = error;
    }

    public String getErrorMessage() {
        if (null == error)
            return "未知错误";
        return error.getMessage();
    }

    @NonNull
    @Override
    public String toString() {
        return "Response{" +
                "responseCode=" + responseCode +
                ", headers=" + headers +
                ", body=" + body +
                ", error='" + getErrorMessage() + '\'' +
                '}';
    }

}
