package com.example.notes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NoteRepositoryTest {

    @Autowired
    NoteRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        repo.saveAndFlush(newNote("first", "a", "b"));
        repo.saveAndFlush(newNote("second", "b", "c"));
    }

    private static Note newNote(String title, String... tags) {
        Note n = new Note();
        n.setTitle(title);
        n.setTags(new TreeSet<>(Set.of(tags)));
        return n;
    }

    @Test
    void findAllByOrderByCreatedAtDescReturnsAll() {
        assertThat(repo.findAllByOrderByCreatedAtDesc()).hasSize(2);
    }

    @Test
    void findByTagFilters() {
        assertThat(repo.findByTag("b")).hasSize(2);
        assertThat(repo.findByTag("a")).extracting(Note::getTitle).containsExactly("first");
        assertThat(repo.findByTag("zzz")).isEmpty();
    }

    @Test
    void findAllTagsReturnsDistinctSorted() {
        assertThat(repo.findAllTags()).containsExactly("a", "b", "c");
    }
}

