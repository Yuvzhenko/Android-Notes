#!/bin/sh
# Створює локальний репозиторій з логічною історією комітів.
# Використовує ваші git user.name / user.email. Після виконання скрипт можна видалити.
set -e
git init -b main

git add .gitignore build.gradle settings.gradle gradle.properties gradle app/build.gradle
git commit -m "chore: add Gradle project skeleton and .gitignore"

git add app/src/main/java/com/example/notes/data/Note.java \
        app/src/main/java/com/example/notes/data/NoteDao.java \
        app/src/main/java/com/example/notes/data/NoteDatabase.java
git commit -m "feat(data): add Room entity, DAO and database for notes"

git add app/src/main/java/com/example/notes/sync/NoteMerger.java \
        app/src/main/java/com/example/notes/sync/NoteJson.java \
        app/src/main/java/com/example/notes/sync/RemoteFile.java \
        app/src/main/java/com/example/notes/sync/SyncStatus.java
git commit -m "feat(sync): add note merging and JSON serialization"

git add app/src/main/java/com/example/notes/sync/DriveNotesStore.java \
        app/src/main/java/com/example/notes/sync/DriveApiException.java
git commit -m "feat(sync): add Google Drive appDataFolder client"

git add app/src/main/java/com/example/notes/data/NoteRepository.java \
        app/src/main/java/com/example/notes/ui/NotesViewModel.java
git commit -m "feat: add repository with Drive sync and view model"

git add app/src/main/java/com/example/notes/NotesApp.java \
        app/src/main/java/com/example/notes/logging
git commit -m "feat(logging): configure Timber for debug and release builds"

git add app/src/main/res/values app/src/main/res/values-uk app/src/main/res/xml
git commit -m "feat(i18n): add English and Ukrainian resources and locale config"

git add app/src/main/AndroidManifest.xml \
        app/src/main/res/layout app/src/main/res/menu app/src/main/res/drawable \
        app/src/main/res/mipmap-anydpi-v26 \
        app/src/main/java/com/example/notes/ui/MainActivity.java \
        app/src/main/java/com/example/notes/ui/EditNoteActivity.java \
        app/src/main/java/com/example/notes/ui/NotesAdapter.java
git commit -m "feat(ui): add notes list, editor, language switch and Drive authorization"

git add app/src/test
git commit -m "test: add unit tests for note merging and JSON serialization"

git add README.md
git commit -m "docs: add README with setup instructions"

echo "Готово. Далі: git remote add origin <URL> && git push -u origin main"
