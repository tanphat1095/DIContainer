package vn.phat.exception;

public class BeanResolutionException extends BeanException {

    public BeanResolutionException(String message) {
        super(message);
    }

    public BeanResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
