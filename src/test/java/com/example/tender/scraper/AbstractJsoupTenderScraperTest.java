package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class AbstractJsoupTenderScraperTest {
    public static void main(String[] args) throws IOException {
        String html = Files.readString(Path.of("src/test/resources/samples/listing.html"), StandardCharsets.UTF_8);
        TestableScraper scraper = new TestableScraper();
        List<Tender> tenders = scraper.parse(html);

        assertCondition(tenders.size() == 2, "Expected 2 tenders");
        assertCondition("Modernisierung Rechenzentrum".equals(tenders.get(0).getTitle()), "Unexpected title");
        assertCondition("Stadt Musterstadt".equals(tenders.get(0).getPublisher()), "Unexpected publisher");
        assertCondition(LocalDate.of(2026, 4, 21).equals(tenders.get(0).getDeadlineDate()), "Unexpected deadline");
        assertCondition("VG-2026-002".equals(tenders.get(1).getExternalId()), "Unexpected external id");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static class TestableScraper extends AbstractJsoupTenderScraper {
        @Override
        public String sourceName() { return "test"; }

        @Override
        public List<Tender> scrape() { throw new UnsupportedOperationException(); }

        List<Tender> parse(String html) {
            return extractFromListing(html,
                    "search-result",
                    "h2",
                    "description",
                    "publisher",
                    "deadline",
                    "identifier");
        }
    }
}
