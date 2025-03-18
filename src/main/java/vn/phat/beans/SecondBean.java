package vn.phat.beans;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;

@Bean
public class SecondBean implements ActionInterface {

    private final ThirdBean thirdBean;

    @Autowired
    private FourthBean fourthBean;

    @Autowired
    public SecondBean(ThirdBean thirdBean, FourthBean fourthBean) {
        this.thirdBean = thirdBean;
        this.fourthBean = fourthBean;
    }

    public void action(){
        System.out.println("This is "+ this.getName());
        action(thirdBean);
    }

    public FourthBean getFourthBean() {
        return fourthBean;
    }
}
