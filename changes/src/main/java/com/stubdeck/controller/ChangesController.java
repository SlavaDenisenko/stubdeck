package com.stubdeck.controller;

import com.stubdeck.service.PendingChangeTracker;
import com.stubdeck.service.changes.ChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChangesController {

    private final ChangeLogService changeLogService;
    private final PendingChangeTracker tracker;

    @GetMapping("/changes")
    public String changes(Model model) {
        model.addAttribute("pending", tracker.getAllOrdered());
        model.addAttribute("changelog", changeLogService.readChangelog());
        return "changes";
    }
}
