package org.omt.labelmanager.label.api;

import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Controller
@RequestMapping("/labels")
public class LabelController {

    private final LabelCRUDHandler labelCRUDHandler;
    private final ReleaseCRUDHandler releaseCRUDHandler;

    public LabelController(
            LabelCRUDHandler labelCRUDHandler,
            ReleaseCRUDHandler releaseCRUDHandler
    ) {
        this.labelCRUDHandler = labelCRUDHandler;
        this.releaseCRUDHandler = releaseCRUDHandler;
    }

    @GetMapping("/{id}")
    public String labelView(@PathVariable Long id, Model model) {
        String labelName =
                labelCRUDHandler
                        .findById(id)
                        .map(Label::name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Release> releases = releaseCRUDHandler.getReleasesForLabel(id);

        model.addAttribute("name", labelName);
        model.addAttribute("id", id);
        model.addAttribute("releases", releases);

        return "labels/label";
    }

    @PostMapping
    public String createLabel(@RequestParam String labelName) {
        labelCRUDHandler.createLabel(labelName);
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelCRUDHandler.delete(id);
        return "redirect:/dashboard";
    }

}
