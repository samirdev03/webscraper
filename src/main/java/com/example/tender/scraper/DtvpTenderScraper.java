package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DtvpTenderScraper extends AbstractJsoupTenderScraper {
    private static final String BASE_URL = "https://www.dtvp.de/Center/common/project/search.do?method=showExtendedSearch&fromExternal=true&page=%d";

    @Override
    public String sourceName() { return "DTVP"; }

    @Override
    public List<Tender> scrape() throws IOException { return scrapePages(1, 2); }

    List<Tender> scrapePages(int fromPage, int toPage) throws IOException {
        List<Tender> tenders = new ArrayList<>();
        for (int page = fromPage; page <= toPage; page++) {
            String html = fetchDocument(BASE_URL.formatted(page));
            tenders.addAll(extractFromListing(html,
                    "project-list-entry",
                    "project-title|h2|h3",
                    "project-description|description|p",
                    "contracting-authority|publisher",
                    "submission-deadline|deadline|date",
                    "project-id|external-id|identifier"));
        }
        return tenders;
    }
}
