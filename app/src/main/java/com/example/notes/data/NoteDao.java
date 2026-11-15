package com.example.notes.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<Note>> observeActive();

    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> observeById(String id);

    @Query("SELECT * FROM notes")
    List<Note> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Note note);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Note> notes);

    @Query("UPDATE notes SET deleted = 1, title = '', content = '', updatedAt = :now WHERE id = :id")
    void markDeleted(String id, long now);

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    void deleteByIds(List<String> ids);
}
