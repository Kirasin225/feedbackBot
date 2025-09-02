package com.company.feedbackbot.controller;

import com.company.feedbackbot.domain.Feedback;
import com.company.feedbackbot.domain.Role;
import com.company.feedbackbot.domain.Sentiment;
import com.company.feedbackbot.repo.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final FeedbackRepository repo;

    @GetMapping
    public String index(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "minCriticality", required = false) Integer minCriticality,
            @RequestParam(name = "maxCriticality", required = false) Integer maxCriticality,
            Model model) {

        if (minCriticality != null) {
            minCriticality = Math.max(1, Math.min(5, minCriticality));
        }
        if (maxCriticality != null) {
            maxCriticality = Math.max(1, Math.min(5, maxCriticality));
        }
        if (minCriticality != null && maxCriticality != null && minCriticality > maxCriticality) {
            int tmp = minCriticality;
            minCriticality = maxCriticality;
            maxCriticality = tmp;
        }

        Page<Feedback> p = repo.findAll(PageRequest.of(page, size));

        final Integer minCrit = minCriticality;
        final Integer maxCrit = maxCriticality;
        List<Feedback> filtered = p.getContent().stream()
                .filter(f -> {
                    boolean ok = true;
                    if (branch != null && !branch.isBlank()) {
                        String b = f.getStaffProfile() != null && f.getStaffProfile().getBranch() != null
                                ? f.getStaffProfile().getBranch() : "";
                        ok &= b.equalsIgnoreCase(branch);
                    }
                    if (role != null) {
                        ok &= f.getStaffProfile() != null
                                && f.getStaffProfile().getRole() != null
                                && f.getStaffProfile().getRole() == role;
                    }
                    if (minCrit != null) {
                        ok &= f.getCriticality() >= minCrit;
                    }
                    if (maxCrit != null) {
                        ok &= f.getCriticality() <= maxCrit;
                    }
                    return ok;
                })
                .collect(Collectors.toList());

        Page<Feedback> result = new PageImpl<>(filtered, PageRequest.of(page, size), filtered.size());

        model.addAttribute("page", result);
        model.addAttribute("sentiments", Sentiment.values());

        model.addAttribute("roles", Role.values());

        model.addAttribute("branch", branch);
        model.addAttribute("role", role);
        model.addAttribute("minCriticality", minCriticality);
        model.addAttribute("maxCriticality", maxCriticality);

        return "admin/index";
    }
}