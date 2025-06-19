package com.example.learning.common;

public class ConsoleColor {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static void println(String message, String color) {
        System.out.println(color + message + RESET);
    }

    public static void print(String message, String color) {
        System.out.print(color + message + RESET);
    }

    // Optional: convenience methods
    public static void info(String message) {
        println(message, BLUE);
    }

    public static void success(String message) {
        println("✅ " + message, GREEN);
    }

    public static void warn(String message) {
        println("⚠️  " + message, YELLOW);
    }

    public static void error(String message) {
        println("❌ " + message, RED);
    }
    
}
