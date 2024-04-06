package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;

@Bean
public class SecondBean implements ActionInterface {

    @Autowired
    private ThirdBean thirdBean;

    public void action(){
        System.out.println("This is "+ this.getName());
        action(thirdBean);
    }

}
