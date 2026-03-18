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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
            String title = firstNonBlank(
                    extractText(item, titleClassOrTag),
                    extractHeadline(item),
                    extractText(item, "title"));
            if (title.isBlank()) {
                continue;
            }

            String description = firstNonBlank(
                    extractText(item, descriptionClassOrTag),
                    extractText(item, "p"),
                    fallbackDescription(item, title));
            String publisher = firstNonBlank(
                    extractText(item, publisherClassOrTag),
                    extractLabeledValue(item, "Auftraggeber|Vergabestelle|Beschaffer|Organisation|Herausgeber|Publisher"));
            String rawDeadline = firstNonBlank(
                    extractText(item, deadlineClassOrTag),
                    extractLabeledValue(item, "Frist|Angebotsfrist|Teilnahmefrist|Deadline|Schlusstermin|Abgabefrist"),
                    normalize(item));
            String externalId = firstNonBlank(
                    extractText(item, externalIdClassOrTag),
                    extractAttribute(item, "data-id"),
                    extractAttribute(item, "data-item-id"),
                    extractLabeledValue(item, "Vergabenummer|Referenznummer|Notice-ID|ID|Aktenzeichen|Identifier"),
                    title);

            tenders.add(Tender.create(
                    title,
                    description,
                    publisher,
                    parseDate(rawDeadline),
                    externalId,
                    calculateScore(title, description, publisher)
            ));
        }
        return tenders;
    }

    private List<String> splitItems(String html, String itemClass) {
        Set<String> items = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(
                "<(\\w+)([^>]*\\bclass\\s*=\\s*([\"'])[^\"']*" + Pattern.quote(itemClass) + "[^\"']*\\3[^>]*)>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            int itemStart = matcher.start();
            int contentStart = matcher.end();
            int itemEnd = findMatchingClosingTag(html, tagName, contentStart);
            if (itemEnd > itemStart) {
                items.add(html.substring(itemStart, itemEnd));
            }
        }
        if (!items.isEmpty()) {
            return new ArrayList<>(items);
        }

        Matcher fallbackMatcher = Pattern.compile(
                "<(article|li|div|section)[^>]*>(.*?)</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (fallbackMatcher.find()) {
            String item = fallbackMatcher.group(0);
            String normalized = normalize(item);
            if (normalized.contains("Frist") || normalized.contains("Vergabenummer") || normalized.contains("Referenznummer")) {
                items.add(item);
            }
        }
        return new ArrayList<>(items);
    }

    private int findMatchingClosingTag(String html, String tagName, int searchStart) {
        Pattern nestedTagPattern = Pattern.compile("</?" + Pattern.quote(tagName) + "\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = nestedTagPattern.matcher(html);
        matcher.region(searchStart, html.length());
        int depth = 1;
        while (matcher.find()) {
            String token = matcher.group();
            boolean closingTag = token.startsWith("</");
            boolean selfClosing = token.endsWith("/>");
            if (!closingTag && !selfClosing) {
                depth++;
            } else if (closingTag) {
                depth--;
                if (depth == 0) {
                    return matcher.end();
                }
            }
        }
        return html.length();
    }

    protected String extractText(String html, String classOrTag) {
        if (classOrTag == null || classOrTag.isBlank()) {
            return "";
        }
        for (String candidate : classOrTag.split("\\|")) {
            String trimmed = candidate.trim();
            if (trimmed.startsWith("data-")) {
                String byAttribute = extractAttribute(html, trimmed);
                if (!byAttribute.isBlank()) {
                    return normalize(byAttribute);
                }
                continue;
            }
            if (isHtmlTag(trimmed)) {
                String byTag = extractByTag(html, trimmed);
                if (!byTag.isBlank()) {
                    return normalize(byTag);
                }
                continue;
            }
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


    private boolean isHtmlTag(String candidate) {
        return switch (candidate.toLowerCase(Locale.ROOT)) {
            case "a", "article", "div", "h1", "h2", "h3", "h4", "h5", "li", "meta", "p", "section", "span", "time", "title" -> true;
            default -> false;
        };
    }

    private String extractByClass(String html, String className) {
        Matcher matcher = Pattern.compile(
                "<(\\w+)[^>]*\\bclass\\s*=\\s*([\"'])[^\"']*" + Pattern.quote(className) + "[^\"']*\\2[^>]*>(.*?)</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? matcher.group(3) : "";
    }

    private String extractByTag(String html, String tag) {
        Matcher matcher = Pattern.compile(
                "<" + Pattern.quote(tag) + "\\b[^>]*>(.*?)</" + Pattern.quote(tag) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractAttribute(String html, String attributeName) {
        Matcher matcher = Pattern.compile(
                "\\b" + Pattern.quote(attributeName) + "\\s*=\\s*([\"'])(.*?)\\1",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? matcher.group(2) : "";
    }

    private String extractHeadline(String html) {
        Matcher matcher = Pattern.compile(
                "<(h1|h2|h3|h4|a)\\b[^>]*>(.*?)</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            String headline = normalize(matcher.group(2));
            if (!headline.isBlank()) {
                return headline;
            }
        }
        return "";
    }

    private String extractLabeledValue(String html, String labels) {
        for (String label : labels.split("\\|")) {
            String trimmedLabel = label.trim();
            Matcher htmlMatcher = Pattern.compile(
                    Pattern.quote(trimmedLabel) + "\\s*:?\\s*([^<]{3,120})",
                    Pattern.CASE_INSENSITIVE).matcher(html);
            if (htmlMatcher.find()) {
                return cleanupLabeledValue(normalize(htmlMatcher.group(1)));
            }
        }

        String normalized = normalize(html);
        for (String label : labels.split("\\|")) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(label.trim()) + "\\s*:?\\s*([^|]{3,120})",
                    Pattern.CASE_INSENSITIVE).matcher(normalized);
            if (matcher.find()) {
                return cleanupLabeledValue(matcher.group(1));
            }
        }
        return "";
    }

    private String cleanupLabeledValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("\\s*(Angebotsfrist|Teilnahmefrist|Deadline|Schlusstermin|Abgabefrist)\\s*:.*$", "").trim();
        cleaned = cleaned.replaceAll("\\s*(Vergabenummer|Referenznummer|Notice-ID|Aktenzeichen|Identifier|ID)\\s*:.*$", "").trim();
        return cleaned;
    }

    private String fallbackDescription(String html, String title) {
        String normalized = normalize(html);
        if (normalized.startsWith(title)) {
            normalized = normalized.substring(title.length()).trim();
        }
        return normalized.length() > 240 ? normalized.substring(0, 240).trim() : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String raw) {
        return raw.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#160;", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
