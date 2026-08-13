package org.omt.labelmanager.web.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.omt.labelmanager.identity.api.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.api.user.UserCommandApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    private final UserCommandApi userCommandApi;

    public RegisterController(UserCommandApi userCommandApi) {
        this.userCommandApi = userCommandApi;
    }

    @PostMapping("/register")
    public ResponseEntity<ProblemDetail> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email '{}'", request.email());
        try {
            userCommandApi.registerUser(request.email(), request.password(), request.displayName());
            log.info("User registered successfully: {}", request.email());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            ProblemDetail.forStatusAndDetail(
                                    HttpStatus.CONFLICT,
                                    "An account with this email already exists."));
        }
    }

    record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String displayName) {}
}
