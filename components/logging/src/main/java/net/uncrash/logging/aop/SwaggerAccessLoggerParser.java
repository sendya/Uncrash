package net.uncrash.logging.aop;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.uncrash.core.boost.aop.MethodInterceptorHolder;
import net.uncrash.core.utils.StringUtils;
import net.uncrash.logging.api.LoggerDefine;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

public class SwaggerAccessLoggerParser implements AccessLoggerParser {

    @Override
    public boolean support(Class clazz, Method method) {
        Api api = AnnotationUtils.findAnnotation(clazz, Api.class);
        ApiOperation operation = AnnotationUtils.findAnnotation(method, ApiOperation.class);
        return api != null || operation != null;
    }

    @Override
    public LoggerDefine parse(MethodInterceptorHolder holder) {
        Api api = holder.findAnnotation(Api.class);
        ApiOperation operation = holder.findAnnotation(ApiOperation.class);
        String action = "";
        if (api != null) {
            action = action.concat(api.value());
        }
        if (operation != null) {
            action = StringUtils.isEmpty(action) ? operation.value() : action + "-" + operation.value();
        }
        return new LoggerDefine(action, "");
    }

}
