package vn.phat.util;

public class NameConverter {
    private NameConverter(){}
    public static String convertCLassToBeanName(Class<?> clazz){
        if(clazz == null) return null;
        String classSimpleName = clazz.getSimpleName();
        if(classSimpleName.length() == 1) return classSimpleName.toLowerCase();
        StringBuilder sb = new StringBuilder(classSimpleName);
        char firstChar = classSimpleName.charAt(0);
        sb.setCharAt(0, Character.toLowerCase(firstChar));
        return sb.toString();
    }
}
