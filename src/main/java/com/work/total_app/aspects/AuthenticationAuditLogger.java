package com.work.total_app.aspects;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Log4j2
public class AuthenticationAuditLogger {
    
    /**
     * Log toate apelurile către serviciile de autentificare
     */
    @Before("execution(* com.work.total_app.services.authentication.*.*(..)) || " +
            "execution(* com.work.total_app.services.security.*.*(..))")
    public void logMethodCall(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Sanitize passwords din log-uri
        Object[] sanitizedArgs = Arrays.stream(args)
            .map(arg -> {
                if (arg == null) return "null";
                String argStr = arg.toString();
                // Nu loga parole sau token-uri
                if (argStr.contains("password") || argStr.length() > 100) {
                    return "[REDACTED]";
                }
                return argStr;
            })
            .toArray();
        
        log.info("AUTH_CALL: {}.{} with args: {}", className, methodName, Arrays.toString(sanitizedArgs));
    }
    
    /**
     * Log succesele
     */
    @AfterReturning(
        pointcut = "execution(* com.work.total_app.services.authentication.*.login*(..)) || " +
                   "execution(* com.work.total_app.services.authentication.*.register*(..))",
        returning = "result")
    public void logAuthSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.info("AUTH_SUCCESS: {} completed successfully", methodName);
    }
    
    /**
     * Log eșecurile
     */
    @AfterThrowing(
        pointcut = "execution(* com.work.total_app.services.authentication.*.*(..)) || " +
                   "execution(* com.work.total_app.services.security.*.*(..))",
        throwing = "error")
    public void logAuthFailure(JoinPoint joinPoint, Throwable error) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.warn("AUTH_FAILURE: {}.{} failed with error: {} - {}", 
            className, methodName, error.getClass().getSimpleName(), error.getMessage());
    }
}

