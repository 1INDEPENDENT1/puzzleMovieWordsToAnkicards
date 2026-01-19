package util;

public class AppException extends RuntimeException {
    public enum ErrorCode {
        AUTH,
        NETWORK,
        IO,
        PARSE,
        INPUT
    }

    private final ErrorCode code;

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public static AppException auth(String message) {
        return new AppException(ErrorCode.AUTH, prefix(ErrorCode.AUTH, message));
    }

    public static AppException network(String message, Throwable cause) {
        return new AppException(ErrorCode.NETWORK, prefix(ErrorCode.NETWORK, message), cause);
    }

    public static AppException network(String message) {
        return new AppException(ErrorCode.NETWORK, prefix(ErrorCode.NETWORK, message));
    }

    public static AppException io(String message, Throwable cause) {
        return new AppException(ErrorCode.IO, prefix(ErrorCode.IO, message), cause);
    }

    public static AppException parse(String message) {
        return new AppException(ErrorCode.PARSE, prefix(ErrorCode.PARSE, message));
    }

    public static AppException input(String message) {
        return new AppException(ErrorCode.INPUT, prefix(ErrorCode.INPUT, message));
    }

    private static String prefix(ErrorCode code, String message) {
        String safe = message == null ? "" : message.trim();
        return code.name() + ": " + safe;
    }
}
