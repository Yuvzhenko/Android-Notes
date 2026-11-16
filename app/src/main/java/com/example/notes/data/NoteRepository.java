package com.example.notes.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.notes.sync.DriveApiException;
import com.example.notes.sync.DriveNotesStore;
import com.example.notes.sync.NoteJson;
import com.example.notes.sync.NoteMerger;
import com.example.notes.sync.RemoteFile;
import com.example.notes.sync.SyncStatus;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class NoteRepository {

    private static final long TOMBSTONE_TTL_MS = 30L * 24 * 60 * 60 * 1000;

    private static volatile NoteRepository instance;

    private final NoteDatabase database;
    private final NoteDao dao;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean syncing = new AtomicBoolean(false);
    private final MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>(SyncStatus.IDLE);

    private NoteRepository(Context context) {
        database = NoteDatabase.getInstance(context);
        dao = database.noteDao();
    }

    public static NoteRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NoteRepository.class) {
                if (instance == null) {
                    instance = new NoteRepository(context);
                }
            }
        }
        return instance;
    }

    public LiveData<List<Note>> observeNotes() {
        return dao.observeActive();
    }

    public LiveData<Note> observeNote(String id) {
        return dao.observeById(id);
    }

    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatus;
    }

    public void save(String id, String title, String content) {
        diskExecutor.execute(() -> {
            String noteId = id != null ? id : UUID.randomUUID().toString();
            dao.upsert(new Note(noteId, title, content, System.currentTimeMillis(), false));
            Timber.i("Note saved: %s", noteId);
        });
    }

    public void delete(String id) {
        diskExecutor.execute(() -> {
            dao.markDeleted(id, System.currentTimeMillis());
            Timber.i("Note deleted: %s", id);
        });
    }

    public void clearSyncStatus() {
        syncStatus.postValue(SyncStatus.IDLE);
    }

    public void reportAuthFailure() {
        syncStatus.postValue(SyncStatus.ERROR_AUTH);
    }

    public void sync(String accessToken) {
        if (!syncing.compareAndSet(false, true)) {
            Timber.w("Sync is already running, request ignored");
            return;
        }
        syncStatus.postValue(SyncStatus.SYNCING);
        syncExecutor.execute(() -> {
            SyncStatus result;
            try {
                syncBlocking(accessToken);
                result = SyncStatus.SUCCESS;
            } catch (DriveApiException e) {
                Timber.e(e, "Drive API failure");
                result = e.isAuthError() ? SyncStatus.ERROR_AUTH : SyncStatus.ERROR_NETWORK;
            } catch (IOException e) {
                Timber.e(e, "Network failure during sync");
                result = SyncStatus.ERROR_NETWORK;
            } catch (JSONException | RuntimeException e) {
                Timber.e(e, "Unexpected sync failure");
                result = SyncStatus.ERROR_UNKNOWN;
            } finally {
                syncing.set(false);
            }
            syncStatus.postValue(result);
        });
    }

    private void syncBlocking(String accessToken) throws IOException, JSONException {
        Timber.i("Sync started");
        DriveNotesStore store = new DriveNotesStore(accessToken);

        RemoteFile remote = store.find();
        List<Note> remoteNotes = remote == null ? new ArrayList<>() : NoteJson.fromJson(remote.content);
        Timber.d("Downloaded %d notes from Drive", remoteNotes.size());

        List<Note> merged = new ArrayList<>();
        database.runInTransaction(() -> {
            List<Note> local = dao.getAll();
            List<Note> result = NoteMerger.merge(local, remoteNotes, System.currentTimeMillis(), TOMBSTONE_TTL_MS);

            Set<String> keptIds = new HashSet<>();
            for (Note note : result) {
                keptIds.add(note.id);
            }
            List<String> toRemove = new ArrayList<>();
            for (Note note : local) {
                if (!keptIds.contains(note.id)) {
                    toRemove.add(note.id);
                }
            }
            dao.upsertAll(result);
            if (!toRemove.isEmpty()) {
                dao.deleteByIds(toRemove);
            }
            merged.addAll(result);
        });

        store.save(remote == null ? null : remote.id, NoteJson.toJson(merged));
        Timber.i("Sync finished: %d notes in total", merged.size());
    }
}
