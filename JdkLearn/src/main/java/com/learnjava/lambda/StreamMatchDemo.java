package com.learnjava.lambda;

import java.util.Optional;

/**
 * @author bruis
 */
public class StreamMatchDemo {

    public static void main(String[] args) {
//        test01();
        test02();
    }

    /**
     * 判断是否有年龄大于70的员工
     */
    public static void test01() {
        boolean isExistAgeThan70 = LambdaComparatorDemo.employees
                .stream()
                // 使用了Employee的谓语语句（这种写法方便复用）
                .anyMatch(LambdaComparatorDemo.Employee.ageGreaterThan70);
//                .anyMatch(e -> e.getAge() > 70);
        System.out.println(isExistAgeThan70);

        boolean isExistAgeLessThan18 = LambdaComparatorDemo.employees
                .stream()
                .noneMatch(LambdaComparatorDemo.Employee.ageLessThan18);

        System.out.println(isExistAgeLessThan18);
    }

    /**
     * 元素查找与Optional
     */
    public static void test02() {

        Optional<LambdaComparatorDemo.Employee> employeeOptional = LambdaComparatorDemo.employees
                .stream()
                .filter(e -> e.getAge() > 400)
                .findFirst();

        // Optional#get 会报空
//        System.out.println(employeeOptional.get());
        LambdaComparatorDemo.Employee employee = employeeOptional.orElse(null);
        System.out.println(employee == null);
    }

}
