import vn.phat.container.BeanFactoryImpl;
import vn.phat.annotation.Bean;
import vn.phat.exception.AmbiguousBeanException;
import vn.phat.exception.BeanResolutionException;

interface Service {}

@Bean
class ServiceImplA implements Service {
}

@Bean
class ServiceImplB implements Service {
}

public class TestAmbiguousBeans {
    public static void main(String[] args) {
        System.out.println("Testing ambiguous bean detection...");

        BeanFactoryImpl factory = new BeanFactoryImpl();
        factory.registerBean(ServiceImplA.class, null);
        factory.registerBean(ServiceImplB.class, null);

        try {
            Service service = factory.getBean(Service.class);
            System.out.println("ERROR: Should have thrown AmbiguousBeanException!");
        } catch (AmbiguousBeanException e) {
            System.out.println("SUCCESS: Caught AmbiguousBeanException as expected");
            System.out.println("  Message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Caught unexpected exception: " + e.getClass().getName());
            e.printStackTrace();
        }

        // Test null type throws IllegalArgumentException
        try {
            factory.getBean((Class<?>) null);
            System.out.println("ERROR: Should have thrown IllegalArgumentException for null type!");
        } catch (IllegalArgumentException e) {
            System.out.println("SUCCESS: Caught IllegalArgumentException for null type");
            System.out.println("  Message: " + e.getMessage());
        }

        // Test null bean name throws IllegalArgumentException
        try {
            factory.getBean((String) null);
            System.out.println("ERROR: Should have thrown IllegalArgumentException for null name!");
        } catch (IllegalArgumentException e) {
            System.out.println("SUCCESS: Caught IllegalArgumentException for null name");
            System.out.println("  Message: " + e.getMessage());
        }

        // Test no bean found
        try {
            interface NotRegistered {}
            factory.getBean(NotRegistered.class);
            System.out.println("ERROR: Should have thrown BeanResolutionException for non-existent bean!");
        } catch (BeanResolutionException e) {
            System.out.println("SUCCESS: Caught BeanResolutionException for non-existent bean");
            System.out.println("  Message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Note: Got exception: " + e.getClass().getName());
        }
    }
}
