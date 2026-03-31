package vn.phat;

import vn.phat.annotation.PackageScan;
import vn.phat.beans.FirstBean;
import vn.phat.container.BeanFactory;
import vn.phat.demo.AccountService;
import vn.phat.demo.AccountServiceImpl;
import vn.phat.demo.BankService;
import vn.phat.loader.Application;

/**
 * Entry point – demonstrates:
 *  1. Existing DI wiring (FirstBean chain)
 *  2. AOP / @Transactional:
 *     a) Successful deposit / withdraw
 *     b) Successful transfer (method joining existing tx)
 *     c) External wire with REQUIRES_NEW propagation
 *     d) Failing transfer → automatic rollback
 */
@PackageScan("vn.phat")
public class Main {

    // ── ANSI helpers ──────────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    public static void main(String... args) {
        BeanFactory beanFactory = Application.run(Main.class);

        // ─── 1. Original DI demo ──────────────────────────────────────────
        printSection("1. Dependency Injection demo (existing bean chain)");
        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        firstBean.action();

        // ─── 2. AOP / @Transactional demos ───────────────────────────────
        AccountService accountService = beanFactory.getBean(AccountService.class);
        BankService    bankService    = beanFactory.getBean(BankService.class);

        // ── 2a. Simple deposit ─────────────────────────────────────────────
        printSection("2a. @Transactional – simple deposit (COMMIT)");
        accountService.deposit("ACC-001", 250.0);

        // ── 2b. Simple withdraw ────────────────────────────────────────────
        printSection("2b. @Transactional – simple withdraw (COMMIT)");
        accountService.withdraw("ACC-002", 100.0);

        // ── 2c. Check balance ──────────────────────────────────────────────
        printSection("2c. @Transactional – check balance (COMMIT)");
        accountService.getBalance("ACC-001");
        accountService.getBalance("ACC-002");

        // ── 2d. Internal transfer (multi-step, same transaction) ───────────
        printSection("2d. @Transactional – internal transfer via BankService (COMMIT)");
        bankService.internalTransfer("ACC-001", "ACC-003", 300.0);

        // ── 2e. External wire with REQUIRES_NEW ────────────────────────────
        printSection("2e. @Transactional(REQUIRES_NEW) – external wire");
        bankService.externalWire("ACC-001", 50.0, "OVERSEAS_BANK");

        // ── 2f. Failing transfer → rollback ───────────────────────────────
        printSection("2f. @Transactional – failing transfer (ROLLBACK expected)");
        System.out.println(YELLOW + "  Balances BEFORE failing transfer:" + RESET);
        if (accountService != null) accountService.printAllBalances();

        try {
            accountService.failingTransfer("ACC-001", "ACC-002", 50.0);
        } catch (RuntimeException e) {
            System.out.println(YELLOW + "  Exception propagated to caller: " + e.getMessage() + RESET);
        }

        System.out.println(YELLOW + "  Balances AFTER rollback (should be same as before):" + RESET);
        if (accountService != null) accountService.printAllBalances();

        // ── 2g. Final state ────────────────────────────────────────────────
        printSection("2g. Final account balances");
        accountService.getBalance("ACC-001");
        accountService.getBalance("ACC-002");
        accountService.getBalance("ACC-003");
    }

    private static void printSection(String title) {
        System.out.printf("%n%s%s╔═══════════════════════════════════════════════════════╗%s%n", BOLD, CYAN, RESET);
        System.out.printf("%s%s║  %-54s║%s%n", BOLD, CYAN, title, RESET);
        System.out.printf("%s%s╚═══════════════════════════════════════════════════════╝%s%n", BOLD, CYAN, RESET);
    }
}
