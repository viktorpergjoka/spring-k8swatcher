package io.informerwatch.spring.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class K8WatchProcessor {

    @Around("@annotation(K8Watch)")
    public Object process(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = joinPoint.proceed();

        return obj;
    }
}
