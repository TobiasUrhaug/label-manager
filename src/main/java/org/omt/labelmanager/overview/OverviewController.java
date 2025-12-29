package org.omt.labelmanager.overview;

import org.omt.labelmanager.label.LabelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class OverviewController {

    private final LabelService labelService;

    public OverviewController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping("/overview")
    public String overview(Model model) {
        var labels = labelService.getAllLabels();

        model.addAttribute("user", "Alex The Manager");
        model.addAttribute("labels", labels);

        return "overview";
    }

}
