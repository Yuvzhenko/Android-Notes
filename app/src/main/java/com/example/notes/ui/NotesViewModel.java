package com.example.notes.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.notes.data.Note;
import com.example.notes.data.NoteRepository;
import com.example.notes.sync.SyncStatus;

import java.util.List;

public class NotesViewModel extends AndroidViewModel {

    private final NoteRepository repository;

    public NotesViewModel(@NonNull Application application) {
        super(application);
        repository = NoteRepository.getInstance(application);
    }

    public LiveData<List<Note>> getNotes() {
        return repository.observeNotes();
    }

    public LiveData<Note> getNote(String id) {
        return repository.observeNote(id);
    }

    public LiveData<SyncStatus> getSyncStatus() {
        return repository.getSyncStatus();
    }

    public void saveNote(String id, String title, String content) {
        repository.save(id, title, content);
    }

    public void deleteNote(String id) {
        repository.delete(id);
    }

    public void sync(String accessToken) {
        repository.sync(accessToken);
    }

    public void reportAuthFailure() {
        repository.reportAuthFailure();
    }

    public void clearSyncStatus() {
        repository.clearSyncStatus();
    }
}
