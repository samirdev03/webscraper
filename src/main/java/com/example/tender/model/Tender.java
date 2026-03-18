package com.example.tender.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Tender {
    private String title;
    private String description;
    private String publisher;
    private LocalDate deadlineDate;
    private String externalId;
    private UUID internalUuid;
    private double score;

    public Tender() {
    }

    public Tender(String title, String description, String publisher, LocalDate deadlineDate,
                  String externalId, UUID internalUuid, double score) {
        this.title = title;
        this.description = description;
        this.publisher = publisher;
        this.deadlineDate = deadlineDate;
        this.externalId = externalId;
        this.internalUuid = internalUuid;
        this.score = score;
    }

    public static Tender create(String title, String description, String publisher, LocalDate deadlineDate,
                                String externalId, double score) {
        return new Tender(title, description, publisher, deadlineDate, externalId, UUID.randomUUID(), score);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public UUID getInternalUuid() { return internalUuid; }
    public void setInternalUuid(UUID internalUuid) { this.internalUuid = internalUuid; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tender tender)) return false;
        return Double.compare(tender.score, score) == 0
                && Objects.equals(title, tender.title)
                && Objects.equals(description, tender.description)
                && Objects.equals(publisher, tender.publisher)
                && Objects.equals(deadlineDate, tender.deadlineDate)
                && Objects.equals(externalId, tender.externalId)
                && Objects.equals(internalUuid, tender.internalUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, publisher, deadlineDate, externalId, internalUuid, score);
    }
}
