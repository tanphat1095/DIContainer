package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;


@Bean
public class FirstBean implements ActionInterface {

    private SecondBean secondBean;

    @Autowired
    public void setSecondBean(SecondBean secondBean) {
        this.secondBean = secondBean;
    }

    public SecondBean getSecondBean() {
        return secondBean;
    }

    public void action(){
        System.out.println("This is "+ this.getName());
        action(secondBean);
    }
}
