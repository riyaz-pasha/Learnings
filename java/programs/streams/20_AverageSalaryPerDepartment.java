package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class AverageSalaryPerDepartment {

    record Employee(String name, String dept, Integer salary) {
    }

    public static void main(String[] args) {

        // Employee class: name, dept, salary
        List<Employee> emps = Arrays.asList(
                new Employee("Alice", "IT", 80000),
                new Employee("Bob", "HR", 50000),
                new Employee("Charlie", "IT", 90000),
                new Employee("Dave", "HR", 60000));

        Map<String, Double> deptAvgSalary = emps.stream()
                .collect(Collectors.groupingBy(
                        Employee::dept,
                        Collectors.averagingInt(Employee::salary)));
        System.out.println(deptAvgSalary);
        // IT -> 85000.0
        // HR -> 55000.0

    }

}
