package org.omt.labelmanager.release;

import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private final ReleaseService releaseService;
    private final LabelService labelService;

    public ReleaseController(ReleaseService releaseService, LabelService labelService) {
        this.releaseService = releaseService;
        this.labelService = labelService;
    }

    @PostMapping
    public String createRelease(
            @PathVariable Long labelId,
            @RequestParam String releaseName,
            @RequestParam String releaseDate
    ) {
        Label label =
                labelService
                        .findById(labelId)
                        .orElseThrow(() -> new RuntimeException("Label not found"));

        LocalDate date = LocalDate.parse(releaseDate);

        releaseService.createRelease(releaseName, date, label);

        return "redirect:/labels/" + labelId;
    }

    @GetMapping("/{releaseId}")
    public String releaseView(@PathVariable Long labelId, @PathVariable Long releaseId, Model model) {
        Release release = releaseService
                .findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));



        model.addAttribute("name", release.getName());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.getReleaseDate());

        return "/releases/release";
    }
}
