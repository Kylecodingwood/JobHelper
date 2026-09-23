package com.jobhelper.roadmap.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jobhelper.roadmap.infrastructure.RoadmapTodoEntity;

class RoadmapTodoListOrderTest {

    @Test
    void incompleteBeforeDoneThenDueThenNewestUndated() {
        Instant now = Instant.parse("2026-09-23T10:00:00Z");
        RoadmapTodoEntity doneSoon = todo("done-soon", true, now.plusSeconds(3600), now.minusSeconds(8000));
        RoadmapTodoEntity openLater = todo("open-later", false, now.plusSeconds(86400), now.minusSeconds(100));
        RoadmapTodoEntity openSoon = todo("open-soon", false, now.plusSeconds(3600), now.minusSeconds(5000));
        RoadmapTodoEntity openOlderUndated = todo("open-old", false, null, now.minusSeconds(4000));
        RoadmapTodoEntity openNewerUndated = todo("open-new", false, null, now.minusSeconds(10));
        RoadmapTodoEntity doneUndated = todo("done-undated", true, null, now.minusSeconds(1));

        List<RoadmapTodoEntity> ordered = RoadmapTodoListOrder.sorted(List.of(
                doneUndated, openOlderUndated, doneSoon, openNewerUndated, openLater, openSoon));

        assertEquals(
                List.of("open-soon", "open-later", "open-new", "open-old", "done-soon", "done-undated"),
                ordered.stream().map(RoadmapTodoEntity::getName).toList());
    }

    @Test
    void sameDueKeepsNewerCreatedFirst() {
        Instant due = Instant.parse("2026-09-24T00:00:00Z");
        Instant older = Instant.parse("2026-09-20T00:00:00Z");
        Instant newer = Instant.parse("2026-09-22T00:00:00Z");
        RoadmapTodoEntity olderTodo = todo("older", false, due, older);
        RoadmapTodoEntity newerTodo = todo("newer", false, due, newer);

        List<RoadmapTodoEntity> ordered = RoadmapTodoListOrder.sorted(List.of(olderTodo, newerTodo));

        assertEquals(List.of("newer", "older"), ordered.stream().map(RoadmapTodoEntity::getName).toList());
    }

    private RoadmapTodoEntity todo(String name, boolean done, Instant dueAt, Instant createdAt) {
        RoadmapTodoEntity entity = new RoadmapTodoEntity();
        entity.setTodoId(UUID.randomUUID());
        entity.setFolderId(UUID.randomUUID());
        entity.setName(name);
        entity.setDone(done);
        entity.setDueAt(dueAt);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        entity.setSortOrder(0);
        return entity;
    }
}
