package xyz.liut.bingwallpaper.http;

/**
 * Create by liut
 * 2020/11/2
 */
public class Response<T> {

    private boolean successful;

    private int responseCode;

    private T body;

    private String errorMessage;

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "Response{" +
                "responseCode=" + responseCode +
                ", body=" + body +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }


}
