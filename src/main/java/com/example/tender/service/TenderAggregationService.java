package com.example.tender.service;

import com.example.tender.model.Tender;
import com.example.tender.scraper.TenderScraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TenderAggregationService {
    private final List<TenderScraper> scrapers;

    public TenderAggregationService(List<TenderScraper> scrapers) {
        this.scrapers = List.copyOf(scrapers);
    }

    public List<Tender> scrapeAll() {
        Map<String, Tender> deduplicated = new LinkedHashMap<>();
        for (TenderScraper scraper : scrapers) {
            try {
                for (Tender tender : scraper.scrape()) {
                    deduplicated.putIfAbsent(scraper.sourceName() + ":" + tender.getExternalId(), tender);
                }
            } catch (Exception e) {
                System.err.printf("Quelle %s konnte nicht verarbeitet werden: %s%n", scraper.sourceName(), e.getMessage());
            }
        }
        List<Tender> tenders = new ArrayList<>(deduplicated.values());
        tenders.sort(Comparator.comparing(Tender::getDeadlineDate,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Tender::getTitle, String.CASE_INSENSITIVE_ORDER));
        return tenders;
    }
}
