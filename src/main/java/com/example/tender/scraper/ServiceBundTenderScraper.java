package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceBundTenderScraper extends AbstractJsoupTenderScraper {
    private static final String BASE_URL = "https://www.service.bund.de/Content/DE/Ausschreibungen/Suche/Formular.html?nn=4641514&pageNo=%d";

    @Override
    public String sourceName() { return "service.bund.de"; }

    @Override
    public List<Tender> scrape() throws IOException { return scrapePages(1, 2); }

    List<Tender> scrapePages(int fromPage, int toPage) throws IOException {
        List<Tender> tenders = new ArrayList<>();
        for (int page = fromPage; page <= toPage; page++) {
            String html = fetchDocument(BASE_URL.formatted(page));
            tenders.addAll(extractFromListing(html,
                    "result",
                    "headline|title|h2|h3",
                    "description|text|p",
                    "publisher|organization|subheadline",
                    "frist|deadline|date",
                    "reference|identifier|meta"));
        }
        return tenders;
    }
}
