package net.pullolo.magicitems.utils;

public class Utils {
    public static String upperFirstLetter(String s){
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String prettify(String s){
        s = s.toLowerCase().replaceAll("_", " ");
        String a = "";
        for (String str : s.split(" ")){
            a += upperFirstLetter(str) + " ";
        }
        return a.substring(0, a.length()-1);
    }
}
