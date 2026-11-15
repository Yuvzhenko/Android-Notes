package com.example.notes.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;


@Entity(tableName = "notes")
public class Note {

    @PrimaryKey
    @NonNull
    public final String id;
    public final String title;
    public final String content;
    public final long updatedAt;
    public final boolean deleted;

    public Note(@NonNull String id, String title, String content, long updatedAt, boolean deleted) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Note)) {
            return false;
        }
        Note other = (Note) o;
        return updatedAt == other.updatedAt
                && deleted == other.deleted
                && id.equals(other.id)
                && Objects.equals(title, other.title)
                && Objects.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, updatedAt, deleted);
    }

    @NonNull
    @Override
    public String toString() {
        return "Note{id=" + id + ", updatedAt=" + updatedAt + ", deleted=" + deleted + "}";
    }
}
