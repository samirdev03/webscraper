package com.example.tender.repository;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface TenderRepository {
    void save(List<Tender> tenders, Path targetFile) throws IOException;
    List<Tender> load(Path sourceFile) throws IOException;
}
