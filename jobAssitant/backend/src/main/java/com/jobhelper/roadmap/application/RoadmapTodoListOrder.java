package com.jobhelper.roadmap.application;

import java.util.Comparator;
import java.util.List;

import com.jobhelper.roadmap.infrastructure.RoadmapTodoEntity;

/**
 * Roadmap todolist display order:
 * incomplete first; dated due soonest first; undated newest created first.
 */
final class RoadmapTodoListOrder {
    static final Comparator<RoadmapTodoEntity> COMPARATOR = Comparator
            .comparing(RoadmapTodoEntity::isDone)
            .thenComparing(todo -> todo.getDueAt() == null)
            .thenComparing(RoadmapTodoEntity::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RoadmapTodoEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private RoadmapTodoListOrder() {
    }

    static List<RoadmapTodoEntity> sorted(List<RoadmapTodoEntity> todos) {
        return todos.stream().sorted(COMPARATOR).toList();
    }
}
