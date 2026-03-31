package vn.phat.demo;

import vn.phat.annotation.Bean;
import vn.phat.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of {@link AccountService}.
 *
 * <p>Each method is individually annotated with {@code @Transactional} to
 * demonstrate method-level AOP interception. The in-memory map acts as a
 * fake "database".
 *
 * <p>Note: The class must implement an interface so that the JDK dynamic
 * proxy created by {@link vn.phat.aop.AopProxyFactory} can wrap it.
 */
@Bean("accountService")
public class AccountServiceImpl implements AccountService {

    // ── Fake in-memory "database" ─────────────────────────────────────────
    private final Map<String, Double> accounts = new ConcurrentHashMap<>();

    // Seed some data in the constructor
    public AccountServiceImpl() {
        accounts.put("ACC-001", 1_000.0);
        accounts.put("ACC-002", 500.0);
        accounts.put("ACC-003", 200.0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Operations – each annotated so the proxy intercepts them
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deposit(String accountId, double amount) {
        System.out.printf("  [AccountService] Depositing %.2f to %s%n", amount, accountId);
        accounts.merge(accountId, amount, Double::sum);
        System.out.printf("  [AccountService] New balance of %s: %.2f%n",
                accountId, accounts.get(accountId));
    }

    @Override
    @Transactional
    public void withdraw(String accountId, double amount) {
        double balance = accounts.getOrDefault(accountId, 0.0);
        if (balance < amount) {
            throw new IllegalStateException(
                    String.format("Insufficient funds in %s (balance=%.2f, requested=%.2f)",
                            accountId, balance, amount));
        }
        System.out.printf("  [AccountService] Withdrawing %.2f from %s%n", amount, accountId);
        accounts.put(accountId, balance - amount);
        System.out.printf("  [AccountService] New balance of %s: %.2f%n",
                accountId, accounts.get(accountId));
    }

    @Override
    @Transactional(propagation = Transactional.Propagation.REQUIRED)
    public double getBalance(String accountId) {
        double balance = accounts.getOrDefault(accountId, 0.0);
        System.out.printf("  [AccountService] Balance of %s: %.2f%n", accountId, balance);
        return balance;
    }

    @Override
    @Transactional
    public void transfer(String fromAccount, String toAccount, double amount) {
        System.out.printf("  [AccountService] Transferring %.2f from %s to %s%n",
                amount, fromAccount, toAccount);
        // These calls go through the SAME proxy – in a real Spring context they
        // would be self-invocation problems; here we call the raw impl directly
        // to show the "join existing transaction" behaviour at service level.
        withdraw(fromAccount, amount);
        deposit(toAccount, amount);
        System.out.printf("  [AccountService] Transfer completed successfully.%n");
    }

    @Override
    @Transactional(rollbackOnCheckedException = true)
    public void failingTransfer(String fromAccount, String toAccount, double amount) {
        System.out.printf("  [AccountService] Starting failing transfer of %.2f from %s to %s%n",
                amount, fromAccount, toAccount);
        withdraw(fromAccount, amount);
        System.out.println("  [AccountService] About to throw an error to trigger rollback...");
        throw new RuntimeException("Simulated network failure during transfer!");
    }

    // ── Utility ──────────────────────────────────────────────────────────

    public void printAllBalances() {
        System.out.println("\n  ╔══════════════════════════════════╗");
        System.out.println("  ║       Current Account Balances   ║");
        System.out.println("  ╠══════════════════════════════════╣");
        accounts.forEach((id, bal) ->
                System.out.printf("  ║  %-10s  →  %12.2f     ║%n", id, bal));
        System.out.println("  ╚══════════════════════════════════╝\n");
    }
}
