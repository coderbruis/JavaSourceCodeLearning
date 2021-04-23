package com.learnjava.lambda;

/**
 * @author bruis
 * lambda表达式
 */
public class LambdaDemo01 {

    /**
     * 打印内部类
     */
    interface Printer {
        void print(String content);
//        void print(String content, String operator);
    }

    public static void printSomething(String content, Printer printer) {
        printer.print(content);
    }

    public static void main(String[] args) {
//        Printer printer = (String content) -> {
//            System.out.println(content);
//        };

//        去掉参数类型，只有一个参数时可以去掉括号
//        Printer printer = (content) -> {
//            System.out.println(content);
//        };

//        只有一个参数提
//        Printer printer = val -> System.out.println(val);

        Printer printer = System.out::println;
        printSomething("hello lambda", printer);
    }
}
