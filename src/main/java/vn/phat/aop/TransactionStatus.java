package vn.phat.aop;

/**
 * Represents the state of an active transaction.
 */
public class TransactionStatus {

    private final String id;
    private final String operationName;
    private boolean completed = false;
    private boolean rolledBack = false;

    public TransactionStatus(String id, String operationName) {
        this.id = id;
        this.operationName = operationName;
    }

    public String getId() {
        return id;
    }

    public String getOperationName() {
        return operationName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public void markRolledBack() {
        this.rolledBack = true;
        this.completed = true;
    }
}
