package io.tooko.core.boost.aop;

import io.tooko.core.utils.AopUtils;
import io.tooko.core.utils.ThreadLocalUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MethodInterceptorHolder {

    public static final ParameterNameDiscoverer NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    private String id;

    private Method method;

    private Object target;

    private Map<String, Object> args;

    public static MethodInterceptorHolder current() {
        return ThreadLocalUtils.get(MethodInterceptorHolder.class.getName());
    }

    public static MethodInterceptorHolder clear() {
        return ThreadLocalUtils.getAndRemove(MethodInterceptorHolder.class.getName());
    }

    public static MethodInterceptorHolder setCurrent(MethodInterceptorHolder holder) {
        return ThreadLocalUtils.put(MethodInterceptorHolder.class.getName(), holder);
    }

    public static MethodInterceptorHolder create(MethodInvocation invocation) {
        // String.valueOf(invocation.getMethod().hashCode()).getBytes()
        String id = "";
        String[] argNames = NAME_DISCOVERER.getParameterNames(invocation.getMethod());
        Object[] args = invocation.getArguments();
        Map<String, Object> argMap = new LinkedHashMap<>();
        for (int i = 0, len = args.length; i < len; i++) {
            argMap.put((argNames == null || argNames[i] == null) ? "arg" + i : argNames[i], args[i]);
        }

        return new MethodInterceptorHolder(id, invocation.getMethod(), invocation.getThis(), argMap);
    }

    public MethodInterceptorHolder(String id, Method method, Object target, Map<String, Object> args) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(id);
        Objects.requireNonNull(method);
        Objects.requireNonNull(target);
        Objects.requireNonNull(args);
        this.id = id;
        this.method = method;
        this.target = target;
        this.args = args;
    }

    public MethodInterceptorHolder set() {
        MethodInterceptorHolder.setCurrent(this);
        return this;
    }

    public <T extends Annotation> T findMethodAnnotation(Class<T> annClass) {
        return AopUtils.findMethodAnnotation(annClass, method, annClass);
    }

    public <T extends Annotation> T findClassAnnotation(Class<T> annClass) {
        return AopUtils.findAnnotation(target.getClass(), annClass);
    }

    public <T extends Annotation> T findAnnotation(Class<T> annClass) {
        return AopUtils.findAnnotation(target.getClass(), method, annClass);
    }

    public MethodInterceptorContext createParamContext() {
        return createParamContext(null);
    }

    public MethodInterceptorContext createParamContext(Object invokeResult) {
        return new MethodInterceptorContext() {
            private static final long serialVersionUID = -4102787561601219273L;

            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public <T> Optional<T> getParameter(String name) {
                if (args == null) {
                    return Optional.empty();
                }
                return Optional.of((T) args.get(name));
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annClass) {
                return findAnnotation(annClass);
            }

            @Override
            public Map<String, Object> getParams() {
                return getArgs();
            }

            @Override
            public Object getInvokeResult() {
                return invokeResult;
            }
        };
    }

    public String getId() {
        return id;
    }

    public Method getMethod() {
        return method;
    }

    public Object getTarget() {
        return target;
    }

    public Map<String, Object> getArgs() {
        return args;
    }
}
