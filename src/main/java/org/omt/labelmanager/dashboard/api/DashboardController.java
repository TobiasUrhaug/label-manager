package org.omt.labelmanager.dashboard.api;

import org.omt.labelmanager.label.LabelCRUDHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LabelCRUDHandler labelCRUDHandler;

    public DashboardController(LabelCRUDHandler labelCRUDHandler) {
        this.labelCRUDHandler = labelCRUDHandler;
    }

    @GetMapping("/dashboard")
    public String overview(Model model) {
        var labels = labelCRUDHandler.getAllLabels();

        model.addAttribute("user", "Alex The Manager");
        model.addAttribute("labels", labels);

        return "dashboard";
    }

}
