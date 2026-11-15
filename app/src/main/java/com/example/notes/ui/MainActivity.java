package com.example.notes.ui;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.R;
import com.example.notes.sync.SyncStatus;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final String DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata";

    private NotesViewModel viewModel;
    private View rootView;
    private View emptyView;
    private LinearProgressIndicator progressIndicator;
    private boolean syncing;

    private final ActivityResultLauncher<IntentSenderRequest> authorizationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    Timber.w("Google Drive authorization was cancelled");
                    viewModel.reportAuthFailure();
                    return;
                }
                try {
                    AuthorizationResult authorization = Identity.getAuthorizationClient(this)
                            .getAuthorizationResultFromIntent(result.getData());
                    onAuthorized(authorization.getAccessToken());
                } catch (ApiException e) {
                    Timber.e(e, "Cannot read authorization result");
                    viewModel.reportAuthFailure();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((MaterialToolbar) findViewById(R.id.toolbar));
        rootView = findViewById(R.id.root);
        emptyView = findViewById(R.id.empty_view);
        progressIndicator = findViewById(R.id.sync_progress);

        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);

        NotesAdapter adapter = new NotesAdapter(note -> startActivity(EditNoteActivity.intent(this, note.id)));
        RecyclerView list = findViewById(R.id.notes_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> startActivity(EditNoteActivity.intent(this, null)));

        viewModel.getNotes().observe(this, notes -> {
            adapter.submitList(notes);
            emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
            if (getSupportActionBar() != null) {
                // Множина залежить від мови: «1 нотатка», «2 нотатки», «5 нотаток»
                getSupportActionBar().setSubtitle(
                        getResources().getQuantityString(R.plurals.notes_count, notes.size(), notes.size()));
            }
        });
        viewModel.getSyncStatus().observe(this, this::onSyncStatus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_sync).setEnabled(!syncing);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            startSync();
            return true;
        }
        if (id == R.id.action_language) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSync() {
        Timber.i("Sync requested by user");
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_APPDATA_SCOPE)))
                .build();
        Identity.getAuthorizationClient(this)
                .authorize(request)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        // Потрібен вибір акаунта / згода користувача
                        PendingIntent pendingIntent = result.getPendingIntent();
                        if (pendingIntent == null) {
                            viewModel.reportAuthFailure();
                            return;
                        }
                        authorizationLauncher.launch(
                                new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
                    } else {
                        onAuthorized(result.getAccessToken());
                    }
                })
                .addOnFailureListener(e -> {
                    Timber.e(e, "Authorization request failed");
                    viewModel.reportAuthFailure();
                });
    }

    private void onAuthorized(@Nullable String accessToken) {
        if (accessToken == null) {
            Timber.w("Authorization succeeded but access token is missing");
            viewModel.reportAuthFailure();
            return;
        }
        viewModel.sync(accessToken);
    }

    private void onSyncStatus(SyncStatus status) {
        boolean syncingNow = status == SyncStatus.SYNCING;
        progressIndicator.setVisibility(syncingNow ? View.VISIBLE : View.INVISIBLE);
        if (syncing != syncingNow) {
            syncing = syncingNow;
            invalidateOptionsMenu();
        }

        int message;
        switch (status) {
            case SUCCESS:
                message = R.string.sync_success;
                break;
            case ERROR_AUTH:
                message = R.string.sync_error_auth;
                break;
            case ERROR_NETWORK:
                message = R.string.sync_error_network;
                break;
            case ERROR_UNKNOWN:
                message = R.string.sync_error_unknown;
                break;
            default:
                return;
        }
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        viewModel.clearSyncStatus();
    }


    private void showLanguageDialog() {
        String[] tags = {"", "en", "uk"};
        String[] labels = {
                getString(R.string.language_system),
                getString(R.string.language_english),
                getString(R.string.language_ukrainian)
        };

        int checked = 0;
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (!current.isEmpty()) {
            Locale locale = current.get(0);
            for (int i = 0; i < tags.length; i++) {
                if (locale != null && tags[i].equals(locale.getLanguage())) {
                    checked = i;
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_language_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    Timber.i("Language changed to '%s'", tags[which]);
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[which]));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
