package com.example.notes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findAllByOrderByCreatedAtDesc();

    @Query("select n from Note n where :tag member of n.tags order by n.createdAt desc")
    List<Note> findByTag(@Param("tag") String tag);

    @Query("select distinct t from Note n join n.tags t order by t")
    List<String> findAllTags();
}
