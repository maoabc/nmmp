package tests.common;

public class MyAssert {
    public static void myassert(boolean cond) {
        if (!cond) {
            throw new RuntimeException("assert");
        }
    }
}
