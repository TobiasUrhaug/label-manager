package org.omt.labelmanager.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAppUserSecurityContextFactory.class)
public @interface WithMockAppUser {

    long id() default 1L;

    String email() default "test@example.com";

    String displayName() default "Test User";

}
