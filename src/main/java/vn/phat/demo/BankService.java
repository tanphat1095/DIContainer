package vn.phat.demo;

/**
 * Higher-level banking service that orchestrates account operations.
 * Demonstrates REQUIRES_NEW propagation.
 */
public interface BankService {

    /**
     * Transfer between accounts inside the bank.
     * Delegates to {@link AccountService#transfer}.
     */
    void internalTransfer(String from, String to, double amount);

    /**
     * External wire – always opens a NEW transaction regardless of context.
     */
    void externalWire(String fromAccount, double amount, String destinationBank);

    /**
     * Demonstrates a failed transaction and subsequent successful one,
     * verifying balances are intact after rollback.
     */
    void demonstrateRollback();
}
