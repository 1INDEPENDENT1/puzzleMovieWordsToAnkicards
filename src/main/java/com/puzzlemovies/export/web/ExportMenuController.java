package com.puzzlemovies.export.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExportMenuController {
    private final SessionUserResolver sessionUserResolver;

    public ExportMenuController(SessionUserResolver sessionUserResolver) {
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping("/exports/menu")
    public String menu(HttpSession session) {
        return sessionUserResolver.resolve(session)
                .map(user -> "menu")
                .orElse("redirect:/login");
    }
}
