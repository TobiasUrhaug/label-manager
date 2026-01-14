package org.omt.labelmanager.release.api;

import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseService;
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
        LocalDate date = LocalDate.parse(releaseDate);

        releaseService.createRelease(releaseName, date, labelId);

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
