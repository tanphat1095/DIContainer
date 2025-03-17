package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;

@Bean
public class SecondBean implements ActionInterface {

    private final ThirdBean thirdBean;

    @Autowired
    public SecondBean(ThirdBean thirdBean) {
        this.thirdBean = thirdBean;
    }

    public void action(){
        System.out.println("This is "+ this.getName());
        action(thirdBean);
    }

}
