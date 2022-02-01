package com.nmmedit.jna;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface TestProxy {
    TestProxy INSTANCE = (TestProxy) Proxy.newProxyInstance(TestProxy.class.getClassLoader(), new Class[]{TestProxy.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("hello ");
            return null;
        }
    });

    void pass_str(String msg);
}
