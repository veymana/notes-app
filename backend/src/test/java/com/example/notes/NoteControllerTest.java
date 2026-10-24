package com.example.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock
    NoteRepository repo;

    @InjectMocks
    NoteController controller;

    private static Note note(String title, String content, String... tags) {
        Note n = new Note();
        n.setTitle(title);
        n.setContent(content);
        n.setTags(new java.util.TreeSet<>(List.of(tags)));
        return n;
    }

    @Test
    void listWithNullTagReturnsAll() {
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(note("t", "c", "x")));

        List<NoteController.NoteResponse> result = controller.list(null);

        assertThat(result).hasSize(1);
        NoteController.NoteResponse r = result.get(0);
        assertThat(r.title()).isEqualTo("t");
        assertThat(r.content()).isEqualTo("c");
        assertThat(r.tags()).containsExactly("x");
        assertThat(r.createdAt()).isNotNull();
        assertThat(r.id()).isNull();
        verify(repo, never()).findByTag(any());
    }

    @Test
    void listWithBlankTagReturnsAll() {
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(controller.list("   ")).isEmpty();
        verify(repo, never()).findByTag(any());
    }

    @Test
    void listWithTagNormalisesAndFilters() {
        when(repo.findByTag("java")).thenReturn(List.of(note("t", "c", "java")));

        List<NoteController.NoteResponse> result = controller.list("  JAVA ");

        assertThat(result).hasSize(1);
        verify(repo).findByTag("java");
    }

    @Test
    void createWithTagsTrimsAndLowercases() {
        when(repo.save(any(Note.class))).thenAnswer(i -> i.getArgument(0));

        NoteController.NoteResponse r = controller.create(
                new NoteController.NoteRequest("  Title  ", "body", List.of(" B ", "A", "b")));

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Title");
        assertThat(captor.getValue().getContent()).isEqualTo("body");
        assertThat(r.title()).isEqualTo("Title");
        assertThat(r.tags()).containsExactly("a", "b");
    }

    @Test
    void createWithoutTagsKeepsDefaultEmptyTags() {
        when(repo.save(any(Note.class))).thenAnswer(i -> i.getArgument(0));

        NoteController.NoteResponse r = controller.create(new NoteController.NoteRequest("T", null, null));

        assertThat(r.tags()).isEmpty();
        assertThat(r.content()).isNull();
    }

    @Test
    void deleteExistingRemovesNote() {
        when(repo.existsById(1L)).thenReturn(true);

        controller.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void deleteMissingThrowsNotFound() {
        when(repo.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> controller.delete(2L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(repo, never()).deleteById(any());
    }

    @Test
    void tagsReturnsRepositoryTags() {
        when(repo.findAllTags()).thenReturn(List.of("a", "b"));

        assertThat(controller.tags()).containsExactly("a", "b");
    }
}

