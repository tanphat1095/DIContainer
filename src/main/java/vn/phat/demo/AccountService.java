package vn.phat.demo;

/**
 * Service interface for account operations.
 * Methods here are declared so the JDK Proxy can wrap the implementation.
 */
public interface AccountService {

    void deposit(String accountId, double amount);

    void withdraw(String accountId, double amount);

    double getBalance(String accountId);

    /**
     * Transfer money between two accounts – runs in its own transaction.
     * Internally calls deposit + withdraw (which join the same transaction).
     */
    void transfer(String fromAccount, String toAccount, double amount);

    /**
     * Intentionally fails to demonstrate rollback.
     */
    void failingTransfer(String fromAccount, String toAccount, double amount);

    /**
     * Prints all account balances.
     */
    void printAllBalances();
}
