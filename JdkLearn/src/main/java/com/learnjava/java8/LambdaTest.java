package com.learnjava.java8;

public class LambdaTest {
    public static void main(String[] args) {
        LambdaTest lambdaTest = new LambdaTest();
        //类型声明
        MathOperation addition = (int a, int b) -> a + b;

        //不用类型声明
        MathOperation subtraction = (a, b) -> a - b;

        //大括号中的返回语句
        MathOperation multiplication = (int a, int b) -> {return a * b;};

        //没有大括号的返回语句
        MathOperation division = (int a, int b) -> a / b;


        int num = 1;
        Converter<Integer, String> s = (param) -> System.out.println(String.valueOf(param + num));

        s.convert(2);

        System.out.println(addition.operation(1, 2));

    }



    interface MathOperation {
        int operation(int a, int b);
    }

    interface GreetingService {
        void sayMessage(String message);
    }

    interface Converter<T1, T2> {
        void convert(int i);
    }

    private int operate(int a, int b, MathOperation mathOperation) {
        return mathOperation.operation(a, b);
    }
}
