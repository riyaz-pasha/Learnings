package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class MaxSalaryPerDepartment {

    record Employee(String name, String dept, Integer salary) {
    }

    public static void main(String[] args) {

        // Employee class: name, dept, salary
        List<Employee> emps = Arrays.asList(
                new Employee("Alice", "IT", 80000),
                new Employee("Bob", "HR", 50000),
                new Employee("Charlie", "IT", 90000),
                new Employee("Dave", "HR", 60000));

        Map<String, Optional<Employee>> maxSalaryByDept = emps.stream()
                .collect(Collectors.groupingBy(
                        Employee::dept,
                        Collectors.maxBy(
                                Comparator.comparingInt(Employee::salary))));

        // IT -> Optional[Charlie(90000)]
        // HR -> Optional[Dave(60000)]

        // To get actual employee (not Optional):
        Map<String, Employee> result = emps.stream()
                .collect(Collectors.toMap(
                        Employee::dept,
                        e -> e,
                        (e1, e2) -> e1.salary() > e2.salary() ? e1 : e2));
    }

}
