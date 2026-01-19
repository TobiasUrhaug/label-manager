package org.omt.labelmanager.release.api;

import java.time.LocalDate;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private final ReleaseCRUDHandler releaseCRUDHandler;

    public ReleaseController(ReleaseCRUDHandler releaseCRUDHandler) {
        this.releaseCRUDHandler = releaseCRUDHandler;
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
    public String releaseView(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            Model model) {
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
