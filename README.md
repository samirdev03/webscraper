# Tender Scraper

Java-Anwendung zum Einsammeln von Ausschreibungen aus vier Vergabeportalen und zum Speichern/Laden der Daten als JSON.

## Features
- `Tender`-Modell mit `title`, `description`, `publisher`, `deadlineDate`, `externalId`, `internalUuid` und `score`
- Vier dedizierte Scraper-Klassen für TED, Öffentliche Vergabe, DTVP und service.bund.de
- Aggregations-Service mit Deduplizierung und fehlertolerantem Durchlauf über mehrere Quellen
- JSON-Repository zum Persistieren und Wiederladen der Tender-Liste ohne externe Bibliotheken
- Kleine ausführbare Testklassen für Parser-Extraktion und JSON-Roundtrip

## Build & Run
```bash
mvn -q -DskipTests compile
mvn -q exec:java -Dexec.mainClass=com.example.tender.Application
```

## Tests
```bash
mvn -q -DskipTests test-compile
java -cp target/test-classes:target/classes com.example.tender.scraper.AbstractJsoupTenderScraperTest
java -cp target/test-classes:target/classes com.example.tender.repository.JsonTenderRepositoryTest
java -cp target/test-classes:target/classes com.example.tender.adapter.EvergabeAdapterTest
```

Die Anwendung schreibt die aggregierten Ergebnisse nach `output/tenders.json`.

## Hinweis
Die Zielseiten blockieren in vielen Umgebungen automatisierte HTTP-Zugriffe oder sind aus Sandbox-Umgebungen nicht erreichbar. Deshalb ist die Scraper-Logik pro Portal in eigene Klassen gekapselt, sodass Selektoren oder die Fetch-Strategie später leicht gegen Selenium oder Playwright ausgetauscht werden können, ohne Datenmodell, Aggregation oder Repository neu aufzubauen.
