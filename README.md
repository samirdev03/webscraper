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
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.tender.Application
```

## Tests
```bash
java -cp out com.example.tender.scraper.AbstractJsoupTenderScraperTest
java -cp out com.example.tender.repository.JsonTenderRepositoryTest
```

Die Anwendung schreibt die aggregierten Ergebnisse nach `output/tenders.json`.

## Hinweis
Die Zielseiten blockieren in vielen Umgebungen automatisierte HTTP-Zugriffe oder sind aus Sandbox-Umgebungen nicht erreichbar. Deshalb ist die Scraper-Logik pro Portal in eigene Klassen gekapselt, sodass Selektoren oder die Fetch-Strategie später leicht gegen Selenium oder Playwright ausgetauscht werden können, ohne Datenmodell, Aggregation oder Repository neu aufzubauen.
