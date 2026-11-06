package com.lhyf.dataasset.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handler (or controller) as admin-only. Enforced centrally in
 * {@link AuthInterceptor} before the handler runs, so forgetting an inline
 * check can no longer expose a write endpoint.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {

    /** Message returned in the 403 JSON body. */
    String value() default "只有管理员可以执行该操作";
}
