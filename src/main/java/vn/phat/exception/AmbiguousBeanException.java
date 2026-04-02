package vn.phat.exception;

public class AmbiguousBeanException extends BeanException {

    public AmbiguousBeanException(String message) {
        super(message);
    }

    public AmbiguousBeanException(String message, Throwable cause) {
        super(message, cause);
    }
}
