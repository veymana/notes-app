package com.example.notes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class NoteController {

    public record NoteRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 10000) String content,
            List<@NotBlank @Size(max = 40) String> tags) {}

    public record NoteResponse(Long id, String title, String content, List<String> tags, Instant createdAt) {
        static NoteResponse from(Note n) {
            return new NoteResponse(n.getId(), n.getTitle(), n.getContent(), List.copyOf(n.getTags()), n.getCreatedAt());
        }
    }

    private final NoteRepository repo;

    public NoteController(NoteRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/notes")
    public List<NoteResponse> list(@RequestParam(required = false) String tag) {
        List<Note> notes = (tag == null || tag.isBlank())
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByTag(tag.trim().toLowerCase(Locale.ROOT));
        return notes.stream().map(NoteResponse::from).toList();
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteRequest req) {
        Note note = new Note();
        note.setTitle(req.title().trim());
        note.setContent(req.content());
        if (req.tags() != null) {
            note.setTags(req.tags().stream()
                    .map(t -> t.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(java.util.TreeSet::new)));
        }
        return NoteResponse.from(repo.save(note));
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
        }
        repo.deleteById(id);
    }

    @GetMapping("/tags")
    public List<String> tags() {
        return repo.findAllTags();
    }
}
