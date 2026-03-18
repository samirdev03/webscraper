package com.example.tender.repository;

import com.example.tender.model.Tender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonTenderRepository implements TenderRepository {
    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);

    @Override
    public void save(List<Tender> tenders, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < tenders.size(); i++) {
            Tender tender = tenders.get(i);
            builder.append("  {\n")
                    .append("    \"title\": \"").append(escape(tender.getTitle())).append("\",\n")
                    .append("    \"description\": \"").append(escape(tender.getDescription())).append("\",\n")
                    .append("    \"publisher\": \"").append(escape(tender.getPublisher())).append("\",\n")
                    .append("    \"deadlineDate\": ").append(toJsonString(tender.getDeadlineDate() == null ? null : tender.getDeadlineDate().toString())).append(",\n")
                    .append("    \"externalId\": \"").append(escape(tender.getExternalId())).append("\",\n")
                    .append("    \"internalUuid\": \"").append(tender.getInternalUuid()).append("\",\n")
                    .append("    \"score\": ").append(tender.getScore()).append("\n")
                    .append("  }");
            if (i + 1 < tenders.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(']');
        Files.writeString(targetFile, builder.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public List<Tender> load(Path sourceFile) throws IOException {
        String json = Files.readString(sourceFile, StandardCharsets.UTF_8);
        List<Tender> tenders = new ArrayList<>();
        Matcher matcher = OBJECT_PATTERN.matcher(json);
        while (matcher.find()) {
            String object = matcher.group(1);
            Tender tender = new Tender();
            tender.setTitle(readString(object, "title"));
            tender.setDescription(readString(object, "description"));
            tender.setPublisher(readString(object, "publisher"));
            String date = readNullableString(object, "deadlineDate");
            tender.setDeadlineDate(date == null || date.isBlank() ? null : LocalDate.parse(date));
            tender.setExternalId(readString(object, "externalId"));
            tender.setInternalUuid(UUID.fromString(readString(object, "internalUuid")));
            tender.setScore(Double.parseDouble(readNumber(object, "score")));
            tenders.add(tender);
        }
        return tenders;
    }

    private String readString(String object, String key) {
        String value = readNullableString(object, key);
        return value == null ? "" : value;
    }

    private String readNullableString(String object, String key) {
        Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(null|\\\"(.*?)\\\")", Pattern.DOTALL).matcher(object);
        if (!matcher.find()) {
            return null;
        }
        if ("null".equals(matcher.group(1))) {
            return null;
        }
        return unescape(matcher.group(2));
    }

    private String readNumber(String object, String key) {
        Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*([0-9.]+)").matcher(object);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing number for key " + key);
        }
        return matcher.group(1);
    }

    private String toJsonString(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
