package org.omt.labelmanager.label.api;

import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Controller
@RequestMapping("/labels")
public class LabelController {

    private final LabelService labelService;
    private final ReleaseService releaseService;

    public LabelController(
            LabelService labelService,
            ReleaseService releaseService
    ) {
        this.labelService = labelService;
        this.releaseService = releaseService;
    }

    @GetMapping("/{id}")
    public String labelView(@PathVariable Long id, Model model) {
        String labelName =
                labelService
                        .findById(id)
                        .map(LabelEntity::getName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Release> releases = releaseService.getReleasesForLabel(id);

        model.addAttribute("name", labelName);
        model.addAttribute("id", id);
        model.addAttribute("releases", releases);

        return "labels/label";
    }

    @PostMapping
    public String createLabel(@RequestParam String label) {
        System.out.println("Creating label: " + label);
        labelService.createLabel(label);
        return "redirect:/labels";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelService.delete(id);
        return "redirect:/labels";
    }

}
