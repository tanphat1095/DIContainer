package vn.phat.aop;

import vn.phat.annotation.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK {@link InvocationHandler} that intercepts calls to methods annotated
 * with {@link Transactional} and wraps them in a managed transaction.
 *
 * <p>Look-up order for the annotation:
 * <ol>
 *   <li>The concrete method on the target (implementation class)</li>
 *   <li>The same method signature on the declaring interface</li>
 *   <li>The class-level annotation on the target (applies to all methods)</li>
 * </ol>
 */
public class TransactionalInterceptor implements InvocationHandler {

    private final Object target;
    private final TransactionManager transactionManager;

    public TransactionalInterceptor(Object target, TransactionManager transactionManager) {
        this.target = target;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Transactional tx = resolveTransactional(method);

        if (tx == null) {
            // No @Transactional – pass through directly
            return method.invoke(target, args);
        }

        // ── AOP advice: BEFORE ──────────────────────────────────────────
        String operationName = target.getClass().getSimpleName() + "." + method.getName() + "()";

        // For REQUIRED: track pre-existing tx so we know if we joined or started
        TransactionStatus preExisting = null;
        if (tx.propagation() == Transactional.Propagation.REQUIRED
                && transactionManager instanceof SimpleTransactionManager stm) {
            preExisting = stm.getCurrent();
        }

        // Handle REQUIRES_NEW: suspend existing tx if any
        TransactionStatus suspended = null;
        if (tx.propagation() == Transactional.Propagation.REQUIRES_NEW
                && transactionManager instanceof SimpleTransactionManager stm) {
            suspended = stm.getCurrent();
            if (suspended != null) {
                System.out.printf("  [AOP] Suspending transaction [%s] for REQUIRES_NEW%n",
                        suspended.getId());
            }
        }

        TransactionStatus status = transactionManager.begin(operationName);
        // We own the tx only if begin() returned a new status (not the pre-existing one)
        boolean startedTransaction = (status != preExisting);

        try {
            // ── Proceed with the real method ───────────────────────────
            Object result = method.invoke(target, args);

            // ── AOP advice: AFTER RETURNING ────────────────────────────
            if (startedTransaction) {
                transactionManager.commit(status);
            }
            return result;

        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();

            boolean shouldRollback = (cause instanceof RuntimeException)
                    || (tx.rollbackOnCheckedException());

            if (shouldRollback) {
                transactionManager.rollback(status);
            } else if (startedTransaction) {
                transactionManager.commit(status);
            }

            throw cause; // re-throw original exception
        } finally {
            // Restore suspended transaction (REQUIRES_NEW)
            if (suspended != null && transactionManager instanceof SimpleTransactionManager stm) {
                System.out.printf("  [AOP] Resuming suspended transaction [%s]%n",
                        suspended.getId());
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Transactional resolveTransactional(Method proxyMethod) {
        // 1) Try the concrete method on the implementation class
        try {
            Method implMethod = target.getClass().getMethod(
                    proxyMethod.getName(), proxyMethod.getParameterTypes());
            Transactional ann = implMethod.getAnnotation(Transactional.class);
            if (ann != null) return ann;
        } catch (NoSuchMethodException ignored) { /* fall through */ }

        // 2) Try the interface method
        Transactional ann = proxyMethod.getAnnotation(Transactional.class);
        if (ann != null) return ann;

        // 3) Class-level annotation on the implementation
        return target.getClass().getAnnotation(Transactional.class);
    }
}
