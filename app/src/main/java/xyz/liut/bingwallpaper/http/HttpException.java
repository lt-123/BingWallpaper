package xyz.liut.bingwallpaper.http;

/**
 * Create by liut on 20-11-12
 */
public class HttpException extends Exception {

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

}
