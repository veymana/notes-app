package com.example.notes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class NotesApiApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--x=1"};

            NotesApiApplication.main(args);

            spring.verify(() -> SpringApplication.run(NotesApiApplication.class, args));
        }
    }

    @Test
    void constructorIsCovered() {
        new NotesApiApplication();
    }
}

