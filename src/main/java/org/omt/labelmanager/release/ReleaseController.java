package org.omt.labelmanager.release;

import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        System.out.println("CREATING NEW RELEASE");
        Label label =
                labelService
                        .findById(labelId)
                        .orElseThrow(() -> new RuntimeException("Label not found"));

        LocalDate date = LocalDate.parse(releaseDate);

        releaseService.createRelease(releaseName, date, label);

        return "redirect:/labels/" + labelId;
    }
}
