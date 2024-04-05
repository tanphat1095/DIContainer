package vn.phat;


import vn.phat.container.BeanFactory;
import vn.phat.loader.Application;

//@PackageScan("vn.phat.beans")
public class Main {
    public static void main(String...args){
        BeanFactory beanFactory = Application.run(Main.class);
        beanFactory.getDeclaredBeans().forEach(item->{
            System.out.println(beanFactory.getBean(item).getClass());
        });
    }
}
