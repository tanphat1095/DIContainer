package vn.phat.beans;

public interface ActionInterface {

    void action();
    default String getName(){
        return this.getClass().getSimpleName();
    }

    default void action(ActionInterface bean){
        System.out.println(String.format("%s call %s ", this.getName(), bean.getName()));
        bean.action();
    }

}
