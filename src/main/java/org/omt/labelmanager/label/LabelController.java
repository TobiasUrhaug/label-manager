package org.omt.labelmanager.label;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


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
        var labelName =
                labelService
                        .findById(id)
                        .map(Label::getName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

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
