package org.omt.labelmanager.test;

import org.omt.labelmanager.identity.user.AppUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockAppUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAppUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockAppUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        AppUserDetails principal = new AppUserDetails(
                annotation.id(),
                annotation.email(),
                "password",
                annotation.displayName()
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
