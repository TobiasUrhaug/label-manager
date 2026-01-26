package org.omt.labelmanager.identity.user.api;

import org.omt.labelmanager.identity.domain.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.application.UserCRUDHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserCRUDHandler userCRUDHandler;

    public AuthController(UserCRUDHandler userCRUDHandler) {
        this.userCRUDHandler = userCRUDHandler;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegistrationForm form, Model model) {
        log.info("Registration attempt for email '{}'", form.getEmail());

        try {
            userCRUDHandler.registerUser(
                    form.getEmail(),
                    form.getPassword(),
                    form.getDisplayName()
            );
            log.info("User registered successfully: {}", form.getEmail());
            return "redirect:/login?registered";
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registration failed: {}", e.getMessage());
            model.addAttribute("error", "An account with this email already exists.");
            model.addAttribute("registrationForm", form);
            return "auth/register";
        }
    }
}
