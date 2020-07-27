package com.bruis.learnnetty.binary;

/**
 * @author LuoHaiYang
 */
public class BinaryTest {
    public static void main(String[] args) {

        int a = -16;

        /**
         * 由于二进制运算都是用补码来进行的，因为换算为补码后可以让最高位来参与运算。
         */
        System.out.println(Integer.toBinaryString(a));
        System.out.println(Integer.toUnsignedString(a));
        System.out.println(a << 2);
    }
}
