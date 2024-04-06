package vn.phat.beans;

import vn.phat.annotation.Bean;

@Bean
public class ThirdBean implements ActionInterface {

    public void action(){
        System.out.println("This is "+ this.getName());
    }
}
