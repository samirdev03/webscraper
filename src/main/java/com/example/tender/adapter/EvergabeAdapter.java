package com.example.tender.adapter;

import com.example.tender.model.Tender;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.WaitUntilState;
import com.microsoft.playwright.Playwright;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvergabeAdapter implements SourceManagerAdapter {
    static final String SEARCH_URL = "https://www.evergabe.de/auftraege/auftrag-suchen?search%5Bsort_order%5D=best&search%5Bper_page%5D=10&search_craft_code_ids%5B%5D=&search%5Bcpv_code_numbers%5D=&search%5Bcraft_code_ids%5D%5B%5D=&search%5Bcraft_code_ids%5D%5B%5D=231&search%5Bcraft_code_ids%5D%5B%5D=236&search%5Bcraft_code_ids%5D%5B%5D=223&search%5Bcraft_code_ids%5D%5B%5D=222&search%5Bcraft_code_ids%5D%5B%5D=229&search%5Bcraft_code_ids%5D%5B%5D=232&search%5Bcraft_code_ids%5D%5B%5D=228&search%5Bcraft_code_ids%5D%5B%5D=230&search%5Bcraft_code_ids%5D%5B%5D=234&search%5Bcraft_code_ids%5D%5B%5D=227&search%5Bcraft_code_ids%5D%5B%5D=233&search%5Bregion_type%5D=perimeter&search_cities=&search%5Bfederal_states%5D%5B%5D=&search%5Bzip_codes_name%5D=&search%5Bzip_codes%5D=&search%5Bperformance_period_start_date%5D=&search%5Bperformance_period_end_date%5D=&search%5Bperformance_period_minimum%5D=0&search%5Bperformance_period_min_mode%5D=weeks&search%5Bperformance_period_maximum%5D=0&search%5Bperformance_period_max_mode%5D=weeks&search%5Borigin_mode%5D%5B%5D=&search%5Bquery%5D=&search%5Bquery_operator%5D=and&search%5Bexclude_query%5D=&search%5Bprocedure_range_mode%5D%5B%5D=&search%5Bregulation%5D%5B%5D=&search%5Bcontracting_authority_mode%5D=all&search%5Bcontracting_authorities_include%5D%5B%5D=&search%5Bcontracting_authorities_exclude%5D%5B%5D=&search%5Bdeadline_mode%5D=none&search%5Bdeadline_days%5D=1&search%5Bdeadline_date%5D=&search%5Bpublish_start%5D=all&search%5Bcustom_publish_start%5D=&search%5Bsustainability%5D%5B%5D=&search%5Bprocedure_stadiums%5D%5B%5D=&search%5Buse_additional_procedure_stadiums%5D=0&search%5Binclude_viewed%5D=0&search%5Binclude_viewed%5D=1&search%5Binclude_remembered%5D=0&search%5Binclude_remembered%5D=1&search%5Binclude_archived%5D=0&search%5Binclude_discarded%5D=0";
    private static final Pattern DEADLINE_PATTERN = Pattern.compile("(?i)(?:frist|angebotsfrist|teilnahmefrist|ablauf|endet am)\\s*:?\\s*([0-3]?\\d\\.[0-1]?\\d\\.(?:20)?\\d{2}|(?:20)?\\d{2}-[0-1]?\\d-[0-3]?\\d)");
    private static final Pattern ID_PATTERN = Pattern.compile("(?i)(?:vergabe[- ]?nr\\.?|vergabenummer|referenznummer|aktenzeichen|id)\\s*:?\\s*([A-Z0-9._/-]+)");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d.M.uuuu", Locale.GERMAN),
            DateTimeFormatter.ofPattern("d.M.uu", Locale.GERMAN),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    public ArrayList<Tender> getTenders() {
        Map<String, Tender> tendersById = new LinkedHashMap<>();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(SEARCH_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            dismissConsentIfPresent(page);
            page.waitForTimeout(1500);

            String lastUrl = null;
            while (true) {
                page.waitForLoadState();
                page.waitForTimeout(1000);
                for (Map<String, String> rawTender : extractRawTenders(page)) {
                    Tender tender = toTender(rawTender);
                    if (tender == null) {
                        continue;
                    }
                    tendersById.putIfAbsent(tender.getExternalId(), tender);
                }

                Locator nextLocator = page.locator("a[rel='next'], .pagination a[aria-label*='Weiter'], .pagination a[aria-label*='Next'], .pagination a.next_page, nav[aria-label*='Pagination'] a[aria-label*='Weiter']").first();
                if (nextLocator.count() == 0 || !nextLocator.isVisible() || nextLocator.getAttribute("href") == null) {
                    break;
                }
                String nextHref = nextLocator.getAttribute("href");
                String currentUrl = page.url();
                if (nextHref.equals(lastUrl) || currentUrl.equals(lastUrl)) {
                    break;
                }
                lastUrl = currentUrl;
                nextLocator.click();
                page.waitForLoadState();
            }
            browser.close();
        } catch (Exception e) {
            throw new IllegalStateException("Evergabe konnte nicht gescraped werden", e);
        }
        return new ArrayList<>(tendersById.values());
    }

    @SuppressWarnings("unchecked")
    List<Map<String, String>> extractRawTenders(Page page) {
        Object result = page.evaluate("""
                () => {
                  const text = node => (node?.textContent || '').replace(/\s+/g, ' ').trim();
                  const bySelectors = (root, selectors) => {
                    for (const selector of selectors) {
                      const element = root.querySelector(selector);
                      const value = text(element);
                      if (value) return value;
                    }
                    return '';
                  };
                  const cards = Array.from(document.querySelectorAll('article, li, .search-result, .result, .card, .project, .notice, .listing-item'))
                    .filter(card => {
                      const cardText = text(card).toLowerCase();
                      if (!cardText) return false;
                      const hasLink = card.querySelector('a[href*="/auftraege/"]');
                      return !!hasLink || cardText.includes('vergabe') || cardText.includes('angebotsfrist');
                    });
                  return cards.map((card, index) => {
                    const link = card.querySelector('a[href*="/auftraege/"]');
                    const href = link ? link.href : '';
                    return {
                      title: bySelectors(card, ['h1', 'h2', 'h3', '[data-testid="title"]', '.title', '.heading', '.headline', 'a[href*="/auftraege/"]']),
                      description: bySelectors(card, ['p', '.description', '.excerpt', '.summary', '.content']),
                      publisher: bySelectors(card, ['.issuer', '.publisher', '.contracting-authority', '.company', '.organization', '[data-testid="buyer"]']),
                      deadline: bySelectors(card, ['time', '.deadline', '.date', '.meta', '.details', '.badges', '.info']),
                      externalId: bySelectors(card, ['.identifier', '.reference', '.number', '.meta', '.details', '[data-testid="identifier"]']),
                      href,
                      containerText: text(card),
                      index: String(index)
                    };
                  });
                }
                """);
        return (List<Map<String, String>>) result;
    }

    Tender toTender(Map<String, String> rawTender) {
        String title = clean(rawTender.get("title"));
        if (title.isBlank()) {
            return null;
        }
        String description = firstNonBlank(clean(rawTender.get("description")), clean(rawTender.get("containerText")));
        String publisher = clean(rawTender.get("publisher"));
        String containerText = clean(rawTender.get("containerText"));
        LocalDate deadlineDate = parseDate(firstNonBlank(clean(rawTender.get("deadline")), containerText));
        String href = clean(rawTender.get("href"));
        String externalId = extractExternalId(firstNonBlank(clean(rawTender.get("externalId")), containerText), href, rawTender.get("index"));
        return new Tender(
                title,
                description,
                publisher,
                deadlineDate,
                externalId,
                UUID.randomUUID(),
                score(title, description, publisher)
        );
    }

    private void dismissConsentIfPresent(Page page) {
        String[] selectors = {
                "button:has-text('Akzeptieren')",
                "button:has-text('Alle akzeptieren')",
                "button:has-text('Einverstanden')",
                "#onetrust-accept-btn-handler"
        };
        for (String selector : selectors) {
            Locator locator = page.locator(selector).first();
            if (locator.count() > 0 && locator.isVisible()) {
                locator.click();
                page.waitForTimeout(500);
                return;
            }
        }
    }

    private String extractExternalId(String rawValue, String href, String fallbackIndex) {
        Matcher matcher = ID_PATTERN.matcher(rawValue);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (!href.isBlank()) {
            String normalized = href.replaceAll("/$", "");
            int slash = normalized.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < normalized.length()) {
                return normalized.substring(slash + 1);
            }
            return normalized;
        }
        return "evergabe-" + fallbackIndex + "-" + Integer.toHexString(rawValue.hashCode());
    }

    private LocalDate parseDate(String value) {
        Matcher matcher = DEADLINE_PATTERN.matcher(value);
        String candidate = matcher.find() ? matcher.group(1) : value;
        candidate = candidate.replaceAll("(?<=\\d\\.\\d\\.)(\\d{2})$", "20$1").trim();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(candidate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private double score(String title, String description, String publisher) {
        double score = 10.0;
        if (!publisher.isBlank()) score += 5.0;
        if (!description.isBlank()) score += 3.0;
        if (title.toLowerCase(Locale.ROOT).contains("sanierung") || title.toLowerCase(Locale.ROOT).contains("bau")) score += 2.0;
        return score;
    }

    private String clean(String value) {
        return value == null ? "" : value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
