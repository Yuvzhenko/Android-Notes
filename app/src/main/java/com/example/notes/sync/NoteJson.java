package com.example.notes.sync;

import com.example.notes.data.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class NoteJson {

    private static final int FORMAT_VERSION = 1;

    private NoteJson() { }

    public static String toJson(List<Note> notes) throws JSONException {
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            JSONObject item = new JSONObject();
            item.put("id", note.id);
            item.put("title", note.title);
            item.put("content", note.content);
            item.put("updatedAt", note.updatedAt);
            item.put("deleted", note.deleted);
            array.put(item);
        }
        JSONObject root = new JSONObject();
        root.put("version", FORMAT_VERSION);
        root.put("notes", array);
        return root.toString();
    }

    public static List<Note> fromJson(String json) throws JSONException {
        List<Note> notes = new ArrayList<>();
        JSONArray array = new JSONObject(json).optJSONArray("notes");
        if (array == null) {
            return notes;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            notes.add(new Note(
                    item.getString("id"),
                    item.optString("title", ""),
                    item.optString("content", ""),
                    item.optLong("updatedAt", 0L),
                    item.optBoolean("deleted", false)));
        }
        return notes;
    }
}
