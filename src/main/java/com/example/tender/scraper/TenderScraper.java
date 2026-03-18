package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.util.List;

public interface TenderScraper {
    String sourceName();
    List<Tender> scrape() throws IOException;
}
