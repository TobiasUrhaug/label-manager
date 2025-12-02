package org.omt.labelmanager.label;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


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

    @GetMapping("/labels/{id}")
    public String labelView(@PathVariable Long id, Model model) {
        var label = labelService.getLabelById(id);
        var labelName = label.map(Label::getName).orElse("Unknown");

        model.addAttribute("name", labelName);

        return "label";
    }

    @PostMapping("/labels")
    public String createLabel(@RequestParam String label) {
        System.out.println("Creating label: " + label);
        labelService.createLabel(label);
        return "redirect:/labels";
    }

    @DeleteMapping("/labels/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelService.delete(id);
        return "redirect:/labels";
    }

}
