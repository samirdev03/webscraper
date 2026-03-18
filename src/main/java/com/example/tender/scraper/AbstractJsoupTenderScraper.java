package com.example.tender.scraper;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractJsoupTenderScraper implements TenderScraper {
    protected String fetchDocument(String url) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; TenderBot/1.0)")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode() + " für " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abruf unterbrochen", e);
        }
    }

    protected List<Tender> extractFromListing(String html, String itemClass,
                                              String titleClassOrTag, String descriptionClassOrTag,
                                              String publisherClassOrTag, String deadlineClassOrTag,
                                              String externalIdClassOrTag) {
        List<Tender> tenders = new ArrayList<>();
        for (String item : splitItems(html, itemClass)) {
            String title = extractText(item, titleClassOrTag);
            if (title.isBlank()) {
                continue;
            }
            String description = extractText(item, descriptionClassOrTag);
            String publisher = extractText(item, publisherClassOrTag);
            String rawDeadline = extractText(item, deadlineClassOrTag);
            String externalId = extractText(item, externalIdClassOrTag);
            tenders.add(Tender.create(
                    title,
                    description,
                    publisher,
                    parseDate(rawDeadline),
                    externalId.isBlank() ? title : externalId,
                    calculateScore(title, description, publisher)
            ));
        }
        return tenders;
    }

    private List<String> splitItems(String html, String itemClass) {
        List<String> items = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "<(article|li|div)[^>]*class=\"[^\"]*" + Pattern.quote(itemClass) + "[^\"]*\"[^>]*>(.*?)</\\1>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            items.add(matcher.group(0));
        }
        return items;
    }

    protected String extractText(String html, String classOrTag) {
        if (classOrTag == null || classOrTag.isBlank()) {
            return "";
        }
        String[] candidates = classOrTag.split("\\|");
        for (String candidate : candidates) {
            String trimmed = candidate.trim();
            String byClass = extractByClass(html, trimmed);
            if (!byClass.isBlank()) {
                return normalize(byClass);
            }
            String byTag = extractByTag(html, trimmed);
            if (!byTag.isBlank()) {
                return normalize(byTag);
            }
        }
        return "";
    }

    private String extractByClass(String html, String className) {
        Matcher matcher = Pattern.compile("<(\\w+)[^>]*class=\"[^\"]*" + Pattern.quote(className) + "[^\"]*\"[^>]*>(.*?)</\\1>", Pattern.DOTALL).matcher(html);
        return matcher.find() ? matcher.group(2) : "";
    }

    private String extractByTag(String html, String tag) {
        Matcher matcher = Pattern.compile("<" + Pattern.quote(tag) + "[^>]*>(.*?)</" + Pattern.quote(tag) + ">", Pattern.DOTALL).matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalize(String raw) {
        return raw.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    protected LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        Matcher isoMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(rawDate);
        if (isoMatcher.find()) {
            return LocalDate.parse(isoMatcher.group(1));
        }
        Matcher germanMatcher = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4})").matcher(rawDate);
        if (germanMatcher.find()) {
            String value = germanMatcher.group(1);
            for (DateTimeFormatter formatter : List.of(
                    DateTimeFormatter.ofPattern("d.M.uuuu", Locale.GERMAN),
                    DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMAN))) {
                try {
                    return LocalDate.parse(value, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return null;
    }

    protected double calculateScore(String title, String description, String publisher) {
        int length = title.length() + description.length() + publisher.length();
        return Math.min(100.0, 10.0 + (length / 25.0));
    }
}
