package com.example.notes.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.notes.R;
import com.example.notes.data.Note;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import timber.log.Timber;

public class EditNoteActivity extends AppCompatActivity {

    private static final String EXTRA_NOTE_ID = "note_id";

    private NotesViewModel viewModel;
    private EditText titleInput;
    private EditText contentInput;
    private TextView metaView;

    private String noteId;
    private String loadedTitle = "";
    private String loadedContent = "";

    public static Intent intent(Context context, @Nullable String noteId) {
        Intent intent = new Intent(context, EditNoteActivity.class);
        if (noteId != null) {
            intent.putExtra(EXTRA_NOTE_ID, noteId);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        titleInput = findViewById(R.id.note_title_input);
        contentInput = findViewById(R.id.note_content_input);
        metaView = findViewById(R.id.note_meta);

        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        setTitle(noteId == null ? R.string.title_new_note : R.string.title_edit_note);

        if (noteId != null) {
            loadNote(savedInstanceState != null);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (save(false)) {
                    finish();
                }
            }
        });
    }


    private void loadNote(boolean restoredFromState) {
        LiveData<Note> source = viewModel.getNote(noteId);
        source.observe(this, new Observer<Note>() {
            @Override
            public void onChanged(Note note) {
                if (note == null) {
                    return;
                }
                loadedTitle = note.title == null ? "" : note.title;
                loadedContent = note.content == null ? "" : note.content;
                if (!restoredFromState) {
                    titleInput.setText(loadedTitle);
                    contentInput.setText(loadedContent);
                }
                metaView.setText(getString(R.string.last_edited, DateUtils.formatDateTime(
                        EditNoteActivity.this, note.updatedAt,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH)));
                metaView.setVisibility(android.view.View.VISIBLE);
                source.removeObserver(this);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_note, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_delete).setVisible(noteId != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            if (save(true)) {
                finish();
            }
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private boolean save(boolean explicit) {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            if (explicit) {
                Toast.makeText(this, R.string.error_empty_note, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
        if (title.equals(loadedTitle) && content.equals(loadedContent)) {
            Timber.d("Note not changed, nothing to save");
            return true;
        }
        viewModel.saveNote(noteId, title, content);
        return true;
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteNote(noteId);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
