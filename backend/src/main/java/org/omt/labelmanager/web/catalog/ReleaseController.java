package org.omt.labelmanager.web.catalog;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.shared.Format;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels/{labelId}/releases")
public class ReleaseController {

    private final ReleaseCommandApi releaseCommandApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final ArtistQueryApi artistQueryApi;

    public ReleaseController(
            ReleaseCommandApi releaseCommandApi,
            ReleaseQueryApi releaseQueryApi,
            ArtistQueryApi artistQueryApi) {
        this.releaseCommandApi = releaseCommandApi;
        this.releaseQueryApi = releaseQueryApi;
        this.artistQueryApi = artistQueryApi;
    }

    /** Catalog owns the answer; this only turns "no" into a 404. */
    private void requireRelease(Long labelId, Long releaseId) {
        if (!releaseQueryApi.belongsToLabel(releaseId, labelId)) {
            throw new EntityNotFoundException(
                    "Release " + releaseId + " does not belong to label " + labelId);
        }
    }

    record TrackRequest(List<Long> artistIds, String name, String duration, List<Long> remixerIds) {
        TrackInput toTrackInput(int position) {
            return new TrackInput(
                    artistIds != null ? artistIds : List.of(),
                    name,
                    TrackDuration.parse(duration),
                    position,
                    remixerIds != null ? remixerIds : List.of());
        }
    }

    record CreateReleaseRequest(
            @NotBlank String releaseName,
            @NotBlank String releaseDate,
            List<Long> artistIds,
            List<TrackRequest> tracks,
            List<String> formats) {
        List<TrackInput> toTrackInputs() {
            if (tracks == null) {
                return List.of();
            }
            return IntStream.range(0, tracks.size())
                    .mapToObj(i -> tracks.get(i).toTrackInput(i + 1))
                    .toList();
        }

        Set<Format> toFormats() {
            if (formats == null) {
                return Set.of();
            }
            return formats.stream().map(Format::valueOf).collect(Collectors.toSet());
        }
    }

    record UpdateReleaseRequest(
            @NotBlank String releaseName,
            @NotBlank String releaseDate,
            List<Long> artistIds,
            List<TrackRequest> tracks,
            List<String> formats) {
        List<TrackInput> toTrackInputs() {
            if (tracks == null) {
                return List.of();
            }
            return IntStream.range(0, tracks.size())
                    .mapToObj(i -> tracks.get(i).toTrackInput(i + 1))
                    .toList();
        }

        Set<Format> toFormats() {
            if (formats == null) {
                return Set.of();
            }
            return formats.stream().map(Format::valueOf).collect(Collectors.toSet());
        }
    }

    /**
     * A release with the parts that belong to it: its tracks, and the artist names behind the ids
     * it holds.
     *
     * <p>Its costs, production runs, distributors and sales are separate collections — see {@code
     * /api/labels/{labelId}/costs?releaseId=}, {@code .../releases/{releaseId}/production-runs},
     * {@code /api/labels/{labelId}/distributors} and {@code
     * /api/labels/{labelId}/sales?releaseId=}. Twelve fields drawn from five bounded contexts was
     * the page model §3 calls the direct cause of F3 and of F5's N+1.
     */
    record ReleaseDetailResponse(
            Long releaseId,
            Long labelId,
            String name,
            LocalDate releaseDate,
            List<Artist> artists,
            List<TrackView> tracks,
            Set<Format> formats) {}

    /** The label's releases. Replaces the list that {@code GET /api/labels/{labelId}} bundled. */
    @GetMapping
    public List<Release> releases(@PathVariable Long labelId) {
        return releaseQueryApi.getReleasesForLabel(labelId);
    }

    @GetMapping("/{releaseId}")
    public ReleaseDetailResponse release(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long labelId,
            @PathVariable Long releaseId) {
        requireRelease(labelId, releaseId);
        Release release =
                releaseQueryApi
                        .findById(releaseId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Release not found: " + releaseId));

        List<Artist> allArtists = artistQueryApi.getArtistsForUser(user.getId());
        Map<Long, Artist> artistMap =
                allArtists.stream().collect(Collectors.toMap(Artist::id, Function.identity()));

        return new ReleaseDetailResponse(
                releaseId,
                labelId,
                release.name(),
                release.releaseDate(),
                resolveArtists(release.artistIds(), artistMap),
                resolveTrackArtists(release.tracks(), artistMap),
                release.formats());
    }

    @PostMapping
    public ResponseEntity<Void> createRelease(
            @PathVariable Long labelId, @Valid @RequestBody CreateReleaseRequest request) {
        releaseCommandApi.createRelease(
                request.releaseName(),
                LocalDate.parse(request.releaseDate()),
                labelId,
                request.artistIds() != null ? request.artistIds() : List.of(),
                request.toTrackInputs(),
                request.toFormats());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{releaseId}")
    public ResponseEntity<Void> updateRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @Valid @RequestBody UpdateReleaseRequest request) {
        requireRelease(labelId, releaseId);
        releaseCommandApi.updateRelease(
                releaseId,
                request.releaseName(),
                LocalDate.parse(request.releaseDate()),
                request.artistIds() != null ? request.artistIds() : List.of(),
                request.toTrackInputs(),
                request.toFormats());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{releaseId}")
    public ResponseEntity<Void> deleteRelease(
            @PathVariable Long labelId, @PathVariable Long releaseId) {
        requireRelease(labelId, releaseId);
        releaseCommandApi.delete(releaseId);
        return ResponseEntity.noContent().build();
    }

    private List<Artist> resolveArtists(List<Long> artistIds, Map<Long, Artist> artistMap) {
        return artistIds.stream().map(artistMap::get).filter(a -> a != null).toList();
    }

    private List<TrackView> resolveTrackArtists(List<Track> tracks, Map<Long, Artist> artistMap) {
        return tracks.stream()
                .map(
                        track ->
                                new TrackView(
                                        track.id(),
                                        resolveArtists(track.artistIds(), artistMap),
                                        track.name(),
                                        track.duration(),
                                        track.position(),
                                        resolveArtists(track.remixerIds(), artistMap)))
                .toList();
    }
}
