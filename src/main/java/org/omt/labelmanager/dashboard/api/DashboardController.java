package org.omt.labelmanager.dashboard.api;

import org.omt.labelmanager.label.LabelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LabelService labelService;

    public DashboardController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping("/dashboard")
    public String overview(Model model) {
        var labels = labelService.getAllLabels();

        model.addAttribute("user", "Alex The Manager");
        model.addAttribute("labels", labels);

        return "dashboard";
    }

}
