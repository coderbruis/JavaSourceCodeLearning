package com.learnjava.lambda;

import lombok.AllArgsConstructor;
import lombok.Data;
import sun.java2d.pipe.SpanShapeRenderer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LambdaComparatorDemo {

    public static String[] arrays = {"Milan", "london", "San Francisco", "Tokyo", "New Delhi"};

    public static List<Employee> employees;

    static {
        Employee e1 = new Employee(1,23,"M","Rick","Beethovan", "2021-04-01");
        Employee e2 = new Employee(2,13,"F","Martina","Hengis", "2021-04-02");
        Employee e3 = new Employee(3,43,"M","Ricky","Martin","2021-04-03" );
        Employee e4 = new Employee(4,26,"M","Jon","Lowman", "2021-04-04");
        Employee e5 = new Employee(5,19,"F","Cristine","Maria", "2021-04-05");
        Employee e6 = new Employee(6,15,"M","David","Feezor", "2021-04-06");
        Employee e7 = new Employee(7,68,"F","Melissa","Roy", "2021-04-07");
        Employee e8 = new Employee(8,79,"M","Alex","Gussin", "2021-04-08");
        Employee e9 = new Employee(9,15,"F","Neetu","Singh", "2021-04-09");
        Employee e10 = new Employee(10,45,"M","Naveen","Jain", "2021-04-10");
        employees = Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    public static void main(String[] args) {
//        test01();
//        test02();
        test03();
    }

    /**
     * List字符串排序
     */
    public static void test01() {
        List<String> cities = Arrays.asList(arrays);

        System.out.println(cities);

        // CASE_INSENSITIVE_ORDER 是一个排序器Comparator接口，意思是不区分大小写进行排序
        cities.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println(cities);

        // 自然排序
        cities.sort(Comparator.naturalOrder());
        System.out.println(cities);

        // 可以将排序器放在Stream管道流中
        Stream.of(arrays)
                .sorted(Comparator.naturalOrder())
                .forEach(System.out::println);
    }

    /**
     * 对整数数组进行排序
     */
    public static void test02() {
        List<Integer> numbers = Arrays.asList(6, 2, 4, 3, 1, 9);
        System.out.println(numbers);

        // 自然排序（升序）
        numbers.sort(Comparator.naturalOrder());
        System.out.println(numbers);

        // 降序
        numbers.sort(Comparator.reverseOrder());
        System.out.println(numbers);
    }

    /**
     * 对对象进行排序
     */
    public static void test03() {
        // 根据employee的年龄进行自然排序
        employees.sort(Comparator.comparing(Employee::getAge));
        employees.forEach(System.out::println);

        System.out.println();

        // 根据employee的年龄进行降序排序
        employees.sort(Comparator.comparing(Employee::getAge).reversed());
        employees.forEach(System.out::println);

        System.out.println();

        // 先对性别排序，然后对年龄进行排序
        employees.sort(
                Comparator.comparing(Employee::getGender)
                .thenComparing(Employee::getAge)
                        // 性别，年龄都进行倒序排序
                .reversed()
        );
        employees.forEach(System.out::println);

        // 自定义排序器
        employees.sort((em1, em2) -> {
            if (em1.getAge().equals(em2.getAge())) {
                return 0;
            }
            return em1.getAge() - em2.getAge() > 0 ? -1 : 1;
        });
        employees.forEach(System.out::println);
    }






















    @Data
    @AllArgsConstructor
    public static class Employee {

        private Integer id;
        // 年龄
        private Integer age;
        // 性别
        private String gender;
        private String firstName;
        private String lastName;
        private String date;

        @Override
        public String toString() {
            return "Employee{" +
                    "id=" + id +
                    ", age=" + age +
                    ", gender='" + gender + '\'' +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", date='" + date + '\'' +
                    '}';
        }

        // 年龄大于70的谓语
        static Predicate<Employee> ageGreaterThan70 = e -> e.getAge() > 70;

        static Predicate<Employee> ageLessThan18 = e -> e.getAge() < 18;
    }
}
