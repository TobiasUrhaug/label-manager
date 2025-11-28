package org.omt.LabelManager.label;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping("/labels")
    public String labels(Model model) {
        var labels = labelService.getAllLabels();

        model.addAttribute("user", "Alex The Manager");
        model.addAttribute("labels", labels);

        return "labels";
    }

    @PostMapping("/labels")
    public String createLabel(@RequestParam String label) {
        System.out.println("Creating label: " + label);
        labelService.createLabel(label);
        return "redirect:/labels";
    }

    @PostMapping("/labels/{id}/delete")
    public String deleteLabel(@PathVariable Long id) {
        labelService.delete(id);
        return "redirect:/labels";
    }

}
