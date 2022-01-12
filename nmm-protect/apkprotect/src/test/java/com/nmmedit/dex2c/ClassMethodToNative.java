package com.nmmedit.dex2c;


import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.reference.MethodReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 把原本class里除了构造方法和抽象方法及native方法以外所有方法转换为native方法
 */

public class ClassMethodToNative extends BaseTypeReference implements ClassDef {
    private final ClassDef classDef;
    private final ClassAndMethodFilter filter;

    public ClassMethodToNative(ClassDef classDef, ClassAndMethodFilter filter) {
        this.classDef = classDef;
        this.filter = filter;
    }

    @Nonnull
    @Override
    public String getType() {
        return classDef.getType();
    }

    @Override
    public int getAccessFlags() {
        return classDef.getAccessFlags();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return classDef.getSuperclass();
    }

    @Nonnull
    @Override
    public List<String> getInterfaces() {
        return classDef.getInterfaces();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return null;
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return classDef.getAnnotations();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return classDef.getStaticFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return classDef.getInstanceFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getFields() {
        return classDef.getFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        final Iterator<? extends Method> iterator = classDef.getDirectMethods().iterator();
        return new Iterable<Method>() {

            @Nonnull
            @Override
            public Iterator<Method> iterator() {
                return new MethodIter(iterator);
            }
        };
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        final Iterator<? extends Method> iterator = classDef.getVirtualMethods().iterator();
        return new Iterable<Method>() {

            @Nonnull
            @Override
            public Iterator<Method> iterator() {
                return new MethodIter(iterator);
            }
        };
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        final Iterator<? extends Method> iterator = classDef.getMethods().iterator();
        return new Iterable<Method>() {

            @Nonnull
            @Override
            public Iterator<Method> iterator() {
                return new MethodIter(iterator);
            }
        };
    }

    //方法转换为native方法
    private Method methodToNative(final Method method) {
        if (filter.acceptMethod(method)) {

            return new Method() {

                @Override
                public void validateReference() throws InvalidReferenceException {
                    method.validateReference();
                }

                @Nonnull
                @Override
                public String getDefiningClass() {
                    return method.getDefiningClass();
                }

                @Nonnull
                @Override
                public String getName() {
                    return method.getName();
                }

                @Nonnull
                @Override
                public List<? extends CharSequence> getParameterTypes() {
                    return method.getParameterTypes();
                }

                @Nonnull
                @Override
                public List<? extends MethodParameter> getParameters() {
                    //调试信息 关于参数名之类的,把它们设置为空,不然指令为空时也会写入无用代码段,可能导致dex文件格式有问题
                    return Collections.emptyList();
                }

                @Nonnull
                @Override
                public String getReturnType() {
                    return method.getReturnType();
                }

                @Override
                public int compareTo(@Nonnull MethodReference o) {
                    return method.compareTo(o);
                }

                @Override
                public int getAccessFlags() {
                    //把方法访问标识添加native
                    int flags = method.getAccessFlags();
                    return flags | AccessFlags.NATIVE.getValue();
                }

                @Nonnull
                @Override
                public Set<? extends Annotation> getAnnotations() {
                    return method.getAnnotations();
                }

                @Nonnull
                @Override
                public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
                    return method.getHiddenApiRestrictions();
                }

                @Nullable
                @Override
                public MethodImplementation getImplementation() {
                    //方法代码设置为空
                    return null;
                }
            };
        } else {
            return method;
        }
    }

    private class MethodIter implements Iterator<Method> {
        private final Iterator<? extends Method> iter;

        public MethodIter(Iterator<? extends Method> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Method next() {
            Method next = iter.next();
            if (next == null) {
                return null;
            }
            return methodToNative(next);
        }
    }
}