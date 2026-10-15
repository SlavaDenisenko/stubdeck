package com.stubdeck.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, HttpSession session, Model model) {
        Exception lastError = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (lastError != null) {
            model.addAttribute("errorMessage", simplifyLdapError(lastError.getMessage()));
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        } else if (request.getParameter("error") != null) {
            return "redirect:/login";
        }
        return "login";
    }

    private String simplifyLdapError(String message) {
        if (message == null) return "Authentication failed. Please try again.";
        if (message.contains("000004DC")) return "LDAP bind required — check service account configuration.";
        if (message.contains("52e")) return "Invalid username or password.";
        if (message.contains("533")) return "Account is disabled.";
        if (message.contains("701")) return "Account has expired.";
        if (message.contains("775")) return "Account is locked.";
        return "Authentication failed. Please try again.";
    }
}
