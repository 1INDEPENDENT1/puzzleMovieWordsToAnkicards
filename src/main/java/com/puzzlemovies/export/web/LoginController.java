package com.puzzlemovies.export.web;

import com.puzzlemovies.export.service.AuthService;
import com.puzzlemovies.export.service.AuthService.AuthenticationResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class LoginController {
    private final AuthService authService;
    private final SessionUserResolver sessionUserResolver;

    public LoginController(AuthService authService, SessionUserResolver sessionUserResolver) {
        this.authService = authService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("email") @Email String email,
                              @RequestParam("password") @NotBlank String password,
                              HttpSession session,
                              Model model) throws IOException, InterruptedException {
        AuthenticationResult result = authService.authenticate(email, password);
        if (result.isSuccess()) {
            sessionUserResolver.bind(session, result.user());
            return "redirect:/exports/menu";
        }

        model.addAttribute("error", result.errorMessage());
        return "login";
    }
}
