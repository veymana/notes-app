package com.example.notes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class NoteTest {

    @Test
    void defaultsAreInitialised() {
        Note note = new Note();

        assertThat(note.getId()).isNull();
        assertThat(note.getTitle()).isNull();
        assertThat(note.getContent()).isNull();
        assertThat(note.getTags()).isEmpty();
        assertThat(note.getCreatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void settersUpdateValues() {
        Note note = new Note();
        Set<String> tags = new TreeSet<>(Set.of("a", "b"));

        note.setTitle("title");
        note.setContent("content");
        note.setTags(tags);

        assertThat(note.getTitle()).isEqualTo("title");
        assertThat(note.getContent()).isEqualTo("content");
        assertThat(note.getTags()).containsExactly("a", "b");
    }
}

