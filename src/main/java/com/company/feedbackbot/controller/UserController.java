package com.company.feedbackbot.controller;


import com.company.feedbackbot.domain.AppUser;
import com.company.feedbackbot.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/new")
    public String newUserForm(Model model) {
        return "admin/create";
    }

    @GetMapping("/create")
    public String createUserGet() {
        return "redirect:/admin/users/new";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (username == null || username.isBlank() || password == null || password.length() < 3) {
            model.addAttribute("error", "Invalid username or password (min 3 chars)");
            return "admin/create";
        }

        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "User already exists");
            return "admin/create";
        }

        AppUser user = AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("ADMIN")
                .build();

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "User created");
        return "redirect:/admin";
    }
}
