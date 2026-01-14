package org.omt.labelmanager.label.api;

import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.release.persistence.ReleaseEntity;
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
    private final ReleaseCRUDHandler releaseCRUDHandler;

    public LabelController(
            LabelService labelService,
            ReleaseCRUDHandler releaseCRUDHandler
    ) {
        this.labelService = labelService;
        this.releaseCRUDHandler = releaseCRUDHandler;
    }

    @GetMapping("/{id}")
    public String labelView(@PathVariable Long id, Model model) {
        String labelName =
                labelService
                        .findById(id)
                        .map(Label::name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<ReleaseEntity> releases = releaseCRUDHandler.getReleasesForLabel(id);

        model.addAttribute("name", labelName);
        model.addAttribute("id", id);
        model.addAttribute("releases", releases);

        return "labels/label";
    }

    @PostMapping
    public String createLabel(@RequestParam String labelName) {
        labelService.createLabel(labelName);
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelService.delete(id);
        return "redirect:/dashboard";
    }

}
