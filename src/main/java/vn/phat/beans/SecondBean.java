package vn.phat.beans;

import vn.phat.annotation.Bean;

@Bean
public class SecondBean {
    public void getName(){
        System.out.println("Hello bean2");
    }
}
