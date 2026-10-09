package com.fixora.validation;

import java.lang.annotation.*;

/**
 * Injects the id of the acting user.
 *
 * DEVELOPMENT ONLY: the value is read from the `X-User-Id` request header, which
 * replaces the session/token that a real authentication layer would provide.
 * Services still re-load the user from the database and verify ownership, so a
 * forged id cannot read another user's private data — but it is not a security
 * boundary and is documented as such.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {

    /** When true (default) a missing header raises 403 instead of passing null. */
    boolean required() default true;

    /** When true (default) only an ADMIN user may call the endpoint. */
    boolean adminOnly() default false;
}
