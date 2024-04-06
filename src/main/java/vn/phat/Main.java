package vn.phat;


import vn.phat.annotation.PackageScan;
import vn.phat.beans.FirstBean;
import vn.phat.container.BeanFactory;
import vn.phat.loader.Application;

@PackageScan
public class Main {
    public static void main(String...args) {
        BeanFactory beanFactory = Application.run(Main.class);
        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        firstBean.action();
    }
}
