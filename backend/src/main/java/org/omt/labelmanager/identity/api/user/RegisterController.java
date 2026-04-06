package org.omt.labelmanager.identity.api.user;

import org.omt.labelmanager.identity.application.UserCRUDHandler;
import org.omt.labelmanager.identity.domain.user.EmailAlreadyExistsException;
import org.omt.labelmanager.infrastructure.security.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    private final UserCRUDHandler userCRUDHandler;

    public RegisterController(UserCRUDHandler userCRUDHandler) {
        this.userCRUDHandler = userCRUDHandler;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for email '{}'", request.email());
        try {
            userCRUDHandler.registerUser(request.email(), request.password(), request.displayName());
            log.info("User registered successfully: {}", request.email());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("An account with this email already exists."));
        }
    }

    record RegisterRequest(String email, String password, String displayName) {}
}
