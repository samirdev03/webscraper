package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OeffentlicheVergabeTenderScraper extends AbstractJsoupTenderScraper {
    private static final String BASE_URL = "https://oeffentlichevergabe.de/ui/de/search/?page=%d";

    @Override
    public String sourceName() { return "Öffentliche Vergabe"; }

    @Override
    public List<Tender> scrape() throws IOException { return scrapePages(1, 2); }

    List<Tender> scrapePages(int fromPage, int toPage) throws IOException {
        List<Tender> tenders = new ArrayList<>();
        for (int page = fromPage; page <= toPage; page++) {
            String html = fetchDocument(BASE_URL.formatted(page));
            tenders.addAll(extractFromListing(html,
                    "search-result",
                    "teaser-title|h2|h3",
                    "teaser-text|description|p",
                    "issuer|organization|subtitle",
                    "meta__date|deadline|date",
                    "meta__number|identifier"));
        }
        return tenders;
    }
}
