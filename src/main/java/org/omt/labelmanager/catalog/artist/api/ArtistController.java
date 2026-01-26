package org.omt.labelmanager.catalog.artist.api;

import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.artist.ArtistCRUDHandler;
import org.omt.labelmanager.identity.user.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/artists")
public class ArtistController {

    private static final Logger log = LoggerFactory.getLogger(ArtistController.class);

    private final ArtistCRUDHandler artistCRUDHandler;

    public ArtistController(ArtistCRUDHandler artistCRUDHandler) {
        this.artistCRUDHandler = artistCRUDHandler;
    }

    @GetMapping("/{id}")
    public String artistView(@PathVariable Long id, Model model) {
        Artist artist =
                artistCRUDHandler
                        .findById(id)
                        .orElseThrow(() -> {
                            log.warn("Artist with id {} not found", id);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND);
                        });

        model.addAttribute("id", id);
        model.addAttribute("artistName", artist.artistName());
        model.addAttribute("realName", artist.realName());
        model.addAttribute("email", artist.email());
        model.addAttribute("address", artist.address());

        return "artists/artist";
    }

    @PostMapping
    public String createArtist(
            @AuthenticationPrincipal AppUserDetails user,
            CreateArtistForm form
    ) {
        artistCRUDHandler.createArtist(
                form.getArtistName(),
                form.toRealName(),
                form.getEmail(),
                form.toAddress(),
                user.getId()
        );
        return "redirect:/dashboard";
    }

    @PutMapping("/{id}")
    public String updateArtist(@PathVariable Long id, UpdateArtistForm form) {
        artistCRUDHandler.updateArtist(
                id,
                form.getArtistName(),
                form.toRealName(),
                form.getEmail(),
                form.toAddress()
        );
        return "redirect:/artists/" + id;
    }

    @DeleteMapping("/{id}")
    public String deleteArtist(@PathVariable Long id) {
        artistCRUDHandler.delete(id);
        return "redirect:/dashboard";
    }
}
