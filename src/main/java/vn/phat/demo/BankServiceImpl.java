package vn.phat.demo;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.annotation.Transactional;

/**
 * Concrete implementation of {@link BankService}.
 *
 * <p>Depends on {@link AccountService} via field injection to show that the
 * container correctly injects the <em>proxied</em> bean (not the raw impl).
 */
@Bean("bankService")
public class BankServiceImpl implements BankService {

    @Autowired
    private AccountService accountService;

    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void internalTransfer(String from, String to, double amount) {
        System.out.printf("  [BankService] Internal transfer: %.2f from %s → %s%n",
                amount, from, to);
        accountService.transfer(from, to, amount);
    }

    /**
     * REQUIRES_NEW: always suspends any outer transaction and opens a brand
     * new one for the wire operation.
     */
    @Override
    @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
    public void externalWire(String fromAccount, double amount, String destinationBank) {
        System.out.printf("  [BankService] External wire: %.2f from %s to bank '%s'%n",
                amount, fromAccount, destinationBank);
        accountService.withdraw(fromAccount, amount);
        System.out.printf("  [BankService] Wire sent to %s (amount: %.2f)%n",
                destinationBank, amount);
    }

    @Override
    @Transactional
    public void demonstrateRollback() {
        System.out.println("  [BankService] --- Attempting a transfer that will FAIL ---");
        try {
            accountService.failingTransfer("ACC-001", "ACC-002", 100.0);
        } catch (RuntimeException e) {
            System.out.printf("  [BankService] Caught exception: %s%n", e.getMessage());
            System.out.println("  [BankService] Transaction was rolled back. Balances unchanged.");
        }
    }
}
