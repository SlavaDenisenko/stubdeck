package com.stubdeck.service.changes;

import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.ChangeAction;
import com.stubdeck.model.ChangelogEntry;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.service.context.user.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChangeLogService {

    private final WireMockProperties properties;
    private final UserContext userContext;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(ChangeAction action, WireMockMapping mapping) {
        String line = String.join(" | ",
                LocalDateTime.now().format(FORMATTER),
                action.name(),
                mapping.getId() != null ? mapping.getId() : "unknown",
                mapping.getFileName() != null ? mapping.getFileName() : "unknown",
                mapping.getRequest() != null
                        ? mapping.getRequest().getMethod() + " " + mapping.getRequest().getEffectiveUrl()
                        : "unknown",
                userContext.getCurrentUsername()
        );

        try {
            Files.writeString(
                    Path.of(properties.getChangelog().getPath()),
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warn("Failed to write changelog: {}", e.getMessage());
        }
    }

    public List<ChangelogEntry> readChangelog() {
        return readChangelog(500);
    }

    public List<ChangelogEntry> readChangelog(int limit) {
        Path file = Path.of(properties.getChangelog().getPath());
        if (!Files.exists(file)) return List.of();
        try {
            List<String> allLines = Files.readAllLines(file);
            // Read only the tail so large files don't allocate needlessly
            int from = Math.max(0, allLines.size() - limit);
            List<ChangelogEntry> entries = allLines.subList(from, allLines.size()).stream()
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.reverse(entries);
            return entries;
        } catch (IOException e) {
            log.warn("Failed to read changelog: {}", e.getMessage());
            return List.of();
        }
    }

    private ChangelogEntry parseLine(String line) {
        String[] parts = line.split(" \\| ", -1);
        if (parts.length < 6) return null;
        return new ChangelogEntry(
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
        );
    }
}
