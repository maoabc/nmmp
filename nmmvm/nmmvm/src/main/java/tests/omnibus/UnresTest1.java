package tests.omnibus;

/*
 * Test failure to resolve class members.
 */
class UnresTest1 {
    public static void run() {
        System.out.println("UnresTest1...");

        UnresStuff stuff = new UnresStuff();
        try {
            int x = stuff.instField;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }
        try {       // hit the same one a second time
            int x = stuff.instField;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }
        try {
            stuff.instField = 5;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }

        try {
            double d = stuff.wideInstField;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }
        try {
            stuff.wideInstField = 0.0;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }

        try {
            int y = UnresStuff.staticField;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }
        try {
            UnresStuff.staticField = 17;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }

        try {
            double d = UnresStuff.wideStaticField;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }
        try {
            UnresStuff.wideStaticField = 1.0;
//            MyAssert.myassert(false);
        } catch (NoSuchFieldError nsfe) {
            // good
        }

        try {
            stuff.virtualMethod();
//            MyAssert.myassert(false);
        } catch (NoSuchMethodError nsfe) {
            // good
        }
        try {
            UnresStuff.staticMethod();
//            MyAssert.myassert(false);
        } catch (NoSuchMethodError nsfe) {
            // good
        }
    }
}
