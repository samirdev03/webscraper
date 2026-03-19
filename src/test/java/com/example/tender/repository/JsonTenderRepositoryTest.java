package com.example.tender.repository;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class JsonTenderRepositoryTest {
    public static void main(String[] args) throws IOException {
        TenderRepository repository = new JsonTenderRepository();
        List<Tender> tenders = List.of(new Tender(
                "Cloud Migration",
                "Migration bestehender Systeme in eine neue Infrastruktur",
                "Bundesbehörde A",
                LocalDate.of(2026, 6, 15),
                "EXT-1",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                42.5
        ));

        Path directory = Files.createTempDirectory("tender-repo-test");
        Path file = directory.resolve("tenders.json");
        repository.save(tenders, file);
        List<Tender> loaded = repository.load(file);

        if (!tenders.equals(loaded)) {
            throw new IllegalStateException("JSON roundtrip failed");
        }
    }
}
