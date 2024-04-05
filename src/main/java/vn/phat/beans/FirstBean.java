package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;

@Bean
public class FirstBean {

    @Autowired
    private SecondBean secondBean;

    public void sayHi(){
        System.out.println("Hello bean 1");
        secondBean.getName();
    }
}
