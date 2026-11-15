package com.example.notes.sync;

import com.example.notes.data.Note;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NoteMerger {

    private NoteMerger() { }

    public static List<Note> merge(List<Note> local, List<Note> remote, long now, long tombstoneTtlMillis) {
        Map<String, Note> byId = new LinkedHashMap<>();
        for (Note note : remote) {
            putNewest(byId, note);
        }
        for (Note note : local) {
            putNewest(byId, note);
        }

        List<Note> result = new ArrayList<>();
        for (Note note : byId.values()) {
            boolean expiredTombstone = note.deleted && now - note.updatedAt > tombstoneTtlMillis;
            if (!expiredTombstone) {
                result.add(note);
            }
        }
        return result;
    }

    private static void putNewest(Map<String, Note> byId, Note candidate) {
        Note existing = byId.get(candidate.id);
        if (existing == null || candidate.updatedAt > existing.updatedAt) {
            byId.put(candidate.id, candidate);
        }
    }
}
