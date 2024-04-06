package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;


@Bean
public class FirstBean implements ActionInterface {

    @Autowired
    private SecondBean secondBean;

    public void action(){
        System.out.println("This is "+ this.getName());
        action(secondBean);
    }
}
