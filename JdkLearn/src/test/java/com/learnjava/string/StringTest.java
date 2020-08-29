package com.learnjava.string;

import org.junit.Test;

/**
 * @author LuoHaiYang
 */
public class StringTest {

    @Test
    public void testSplitWithSplit() {
        String splitStr1 = "what,is,,,,split";
        String[] strs1 = splitStr1.split(",");
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
    }

    @Test
    public void testSplitWithOutSplit() {
        String splitStr1 = "what,is,,,,";
        String[] strs1 = splitStr1.split(",");
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
    }

    @Test
    public void testSplitLimit() {
        String splitStr1 = "what,is,,,,";
        String[] strs1 = splitStr1.split(",", -1);
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
    }

    @Test
    public void testSplitLimitGreaterThanZero() {
        String splitStr1 = "what,is,,,,";
        String[] strs1 = splitStr1.split(",", 3);
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
    }
}
