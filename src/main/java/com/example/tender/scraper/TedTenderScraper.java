package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TedTenderScraper extends AbstractJsoupTenderScraper {
    private static final String BASE_URL = "https://ted.europa.eu/de/search/result?ojs-number=54%%2F2026&search-scope=ALL&scope=ALL&onlyLatestVersions=false&sortColumn=publication-number&sortOrder=DESC&page=%d";

    @Override
    public String sourceName() { return "TED"; }

    @Override
    public List<Tender> scrape() throws IOException { return scrapePages(1, 2); }

    List<Tender> scrapePages(int fromPage, int toPage) throws IOException {
        List<Tender> tenders = new ArrayList<>();
        for (int page = fromPage; page <= toPage; page++) {
            String html = fetchDocument(BASE_URL.formatted(page));
            tenders.addAll(extractFromListing(html,
                    "search-result",
                    "result-title|h2|h3",
                    "result-description|description|p",
                    "organisation-name|publisher",
                    "deadline-date|deadline|date",
                    "notice-id|identifier"));
        }
        return tenders;
    }
}
