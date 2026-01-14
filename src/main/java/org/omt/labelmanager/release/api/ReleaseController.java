package org.omt.labelmanager.release.api;

import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private final ReleaseCRUDHandler releaseCRUDHandler;
    private final LabelCRUDHandler labelCRUDHandler;

    public ReleaseController(ReleaseCRUDHandler releaseCRUDHandler, LabelCRUDHandler labelCRUDHandler) {
        this.releaseCRUDHandler = releaseCRUDHandler;
        this.labelCRUDHandler = labelCRUDHandler;
    }

    @PostMapping
    public String createRelease(
            @PathVariable Long labelId,
            @RequestParam String releaseName,
            @RequestParam String releaseDate
    ) {
        LocalDate date = LocalDate.parse(releaseDate);

        releaseCRUDHandler.createRelease(releaseName, date, labelId);

        return "redirect:/labels/" + labelId;
    }

    @GetMapping("/{releaseId}")
    public String releaseView(@PathVariable Long labelId, @PathVariable Long releaseId, Model model) {
        Release release = releaseCRUDHandler
                .findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("name", release.name());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.releaseDate());

        return "/releases/release";
    }
}
