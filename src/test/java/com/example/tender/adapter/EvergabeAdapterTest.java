package com.example.tender.adapter;

import com.example.tender.model.Tender;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class EvergabeAdapterTest {
    public static void main(String[] args) {
        EvergabeAdapter adapter = new EvergabeAdapter();

        Map<String, String> rawTender = new HashMap<>();
        rawTender.put("title", "Sanierung Verwaltungsgebäude");
        rawTender.put("description", "Erneuerung der Dachabdichtung und Fassadenarbeiten.");
        rawTender.put("publisher", "Stadt Leipzig");
        rawTender.put("deadline", "Angebotsfrist: 21.04.2026");
        rawTender.put("externalId", "Vergabenummer: EVB-2026-001");
        rawTender.put("href", "https://www.evergabe.de/auftraege/auftrag-details/1234567/sanierung-verwaltungsgebaeude");
        rawTender.put("containerText", "Sanierung Verwaltungsgebäude Vergabenummer: EVB-2026-001 Angebotsfrist: 21.04.2026");
        rawTender.put("index", "0");

        Tender tender = adapter.toTender(rawTender);
        assertCondition(tender != null, "Tender should not be null");
        assertCondition("Sanierung Verwaltungsgebäude".equals(tender.getTitle()), "Unexpected title");
        assertCondition("Stadt Leipzig".equals(tender.getPublisher()), "Unexpected publisher");
        assertCondition("EVB-2026-001".equals(tender.getExternalId()), "Unexpected external id");
        assertCondition(LocalDate.of(2026, 4, 21).equals(tender.getDeadlineDate()), "Unexpected deadline");

        Map<String, String> fallbackTender = new HashMap<>();
        fallbackTender.put("title", "Malerarbeiten Schule");
        fallbackTender.put("description", "");
        fallbackTender.put("publisher", "");
        fallbackTender.put("deadline", "Frist: 2026-05-30");
        fallbackTender.put("externalId", "");
        fallbackTender.put("href", "https://www.evergabe.de/auftraege/auftrag-details/7654321/malerarbeiten-schule");
        fallbackTender.put("containerText", "Malerarbeiten Schule Frist: 2026-05-30");
        fallbackTender.put("index", "1");

        Tender fallback = adapter.toTender(fallbackTender);
        assertCondition("malerarbeiten-schule".equals(fallback.getExternalId()), "Expected slug fallback id");
        assertCondition(LocalDate.of(2026, 5, 30).equals(fallback.getDeadlineDate()), "Unexpected ISO deadline");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
