package xyz.liut.bingwallpaper.v3.sync;

/**
 * 壁纸同步结果。
 * <p>
 * 用于向调用方表达同步是否成功、可展示或可记录的消息，以及失败时的原始异常。
 */
public class SyncResult {

    private final boolean success;
    private final String message;
    private final Exception exception;

    private SyncResult(boolean success, String message, Exception exception) {
        this.success = success;
        this.message = message;
        this.exception = exception;
    }

    /**
     * 创建成功结果。
     *
     * @param message 成功消息
     * @return 同步成功结果
     */
    public static SyncResult success(String message) {
        return new SyncResult(true, message, null);
    }

    /**
     * 创建失败结果。
     *
     * @param message   失败消息
     * @param exception 原始异常
     * @return 同步失败结果
     */
    public static SyncResult failure(String message, Exception exception) {
        return new SyncResult(false, message, exception);
    }

    /**
     * 是否同步成功。
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取同步消息。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取失败异常；成功时为 null。
     */
    public Exception getException() {
        return exception;
    }
}
