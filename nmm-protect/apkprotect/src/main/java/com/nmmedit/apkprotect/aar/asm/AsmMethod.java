package com.nmmedit.apkprotect.aar.asm;

public class AsmMethod {
    private final String name;
    private final String descriptor;

    public AsmMethod(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AsmMethod myMethod = (AsmMethod) o;

        if (!name.equals(myMethod.name)) return false;
        return descriptor.equals(myMethod.descriptor);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + descriptor.hashCode();
        return result;
    }
}
