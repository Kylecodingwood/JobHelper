package com.jobhelper.roadmap.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.roadmap.infrastructure.RoadmapFolderEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoRepository;

@Service
public class RoadmapRolloverService {

    private static final Pattern SHORT_DATE = Pattern.compile("^(\\d{1,2})/(\\d{1,2})$");
    private static final Pattern ISO_DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

    private final RoadmapFolderRepository folderRepository;
    private final RoadmapTodoRepository todoRepository;

    public RoadmapRolloverService(RoadmapFolderRepository folderRepository, RoadmapTodoRepository todoRepository) {
        this.folderRepository = folderRepository;
        this.todoRepository = todoRepository;
    }

    @Transactional
    public Map<String, Object> rollover() {
        LocalDate today = LocalDate.now();

        List<RoadmapFolderEntity> allFolders = folderRepository.findAllByOrderBySortOrderAscUpdatedAtAsc();

        // Only todolist folders with parseable date names
        Map<UUID, LocalDate> folderDates = new LinkedHashMap<>();
        for (RoadmapFolderEntity f : allFolders) {
            if (!"todolist".equals(f.getKind())) continue;
            LocalDate d = parseDate(f.getName());
            if (d != null) folderDates.put(f.getFolderId(), d);
        }

        if (folderDates.isEmpty()) return result(0, null);

        // Target: earliest date >= today; fallback to latest date folder
        UUID targetId = folderDates.entrySet().stream()
                .filter(e -> !e.getValue().isBefore(today))
                .min(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(
                        folderDates.entrySet().stream()
                                .max(Comparator.comparing(Map.Entry::getValue))
                                .map(Map.Entry::getKey)
                                .orElse(null)
                );

        if (targetId == null) return result(0, null);

        String targetName = allFolders.stream()
                .filter(f -> f.getFolderId().equals(targetId))
                .map(RoadmapFolderEntity::getName)
                .findFirst().orElse("");

        int moved = 0;
        Instant now = Instant.now();

        for (Map.Entry<UUID, LocalDate> entry : folderDates.entrySet()) {
            UUID folderId = entry.getKey();
            LocalDate date = entry.getValue();

            if (!date.isBefore(today)) continue; // skip current/future
            if (folderId.equals(targetId)) continue;

            List<RoadmapTodoEntity> unchecked = todoRepository.findByFolderIdAndDoneFalse(folderId);
            if (unchecked.isEmpty()) continue;

            int maxOrder = todoRepository.findByFolderId(targetId).stream()
                    .mapToInt(RoadmapTodoEntity::getSortOrder).max().orElse(-1);

            for (RoadmapTodoEntity todo : unchecked) {
                todo.setFolderId(targetId);
                todo.setSortOrder(++maxOrder);
                todo.setUpdatedAt(now);
                todoRepository.save(todo);
                moved++;
            }
        }

        return result(moved, moved > 0 ? targetName : null);
    }

    private Map<String, Object> result(int moved, String targetName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("moved", moved);
        m.put("targetFolderName", targetName);
        return m;
    }

    LocalDate parseDate(String name) {
        if (name == null) return null;
        Matcher m = SHORT_DATE.matcher(name.trim());
        if (m.matches()) {
            try {
                int month = Integer.parseInt(m.group(1));
                int day = Integer.parseInt(m.group(2));
                LocalDate now = LocalDate.now();
                LocalDate d = LocalDate.of(now.getYear(), month, day);
                if (d.isAfter(now.plusDays(60))) d = d.minusYears(1);
                return d;
            } catch (Exception e) {
                return null;
            }
        }
        Matcher m2 = ISO_DATE.matcher(name.trim());
        if (m2.matches()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(m2.group(1)),
                        Integer.parseInt(m2.group(2)),
                        Integer.parseInt(m2.group(3)));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
