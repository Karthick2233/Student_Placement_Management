package com.college.placement.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.college.placement.service..*(..)) "
            + "&& !within(com.college.placement.service.AuthService)")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.debug("START {}", method);
        Object result = pjp.proceed();
        log.debug("END   {} ({} ms)", method, System.currentTimeMillis() - start);
        return result;
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        log.warn("ERROR in {} -> {}: {}", jp.getSignature().toShortString(),
                ex.getClass().getSimpleName(), ex.getMessage());
    }
}
