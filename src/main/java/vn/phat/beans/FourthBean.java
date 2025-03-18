package vn.phat.beans;

import vn.phat.annotation.Bean;

@Bean
public class FourthBean {
    private String name = "FourthBean";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
