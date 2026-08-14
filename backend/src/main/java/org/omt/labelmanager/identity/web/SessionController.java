package org.omt.labelmanager.identity.web;

import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private record SessionResponse(String username) {}

    @GetMapping("/api/session")
    public SessionResponse session(Principal principal) {
        return new SessionResponse(principal.getName());
    }
}
