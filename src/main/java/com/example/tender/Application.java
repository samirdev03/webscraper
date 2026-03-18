package com.example.tender;

import com.example.tender.model.Tender;
import com.example.tender.repository.JsonTenderRepository;
import com.example.tender.repository.TenderRepository;
import com.example.tender.scraper.DtvpTenderScraper;
import com.example.tender.scraper.OeffentlicheVergabeTenderScraper;
import com.example.tender.scraper.ServiceBundTenderScraper;
import com.example.tender.scraper.TedTenderScraper;
import com.example.tender.service.TenderAggregationService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Application {
    public static void main(String[] args) throws IOException {
        TenderAggregationService aggregationService = new TenderAggregationService(List.of(
                new TedTenderScraper(),
                new OeffentlicheVergabeTenderScraper(),
                new DtvpTenderScraper(),
                new ServiceBundTenderScraper()
        ));

        List<Tender> tenders = aggregationService.scrapeAll();
        Path output = Path.of("output", "tenders.json");

        TenderRepository repository = new JsonTenderRepository();
        repository.save(tenders, output);
        List<Tender> loaded = repository.load(output);

        System.out.printf("%d Ausschreibungen gespeichert und erneut geladen: %s%n", loaded.size(), output.toAbsolutePath());
    }
}
