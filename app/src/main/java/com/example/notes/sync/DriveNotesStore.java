package com.example.notes.sync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class DriveNotesStore {

    static final String FILE_NAME = "notes.json";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType MULTIPART_RELATED = MediaType.get("multipart/related");

    private final OkHttpClient client;
    private final String accessToken;

    public DriveNotesStore(String accessToken) {
        this(new OkHttpClient(), accessToken);
    }

    DriveNotesStore(OkHttpClient client, String accessToken) {
        this.client = client;
        this.accessToken = accessToken;
    }

    public RemoteFile find() throws IOException {
        HttpUrl listUrl = HttpUrl.get(FILES_URL).newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "name = '" + FILE_NAME + "' and trashed = false")
                .addQueryParameter("fields", "files(id)")
                .build();
        String listing = execute(new Request.Builder().url(listUrl).get());

        String fileId;
        try {
            JSONArray files = new JSONObject(listing).optJSONArray("files");
            if (files == null || files.length() == 0) {
                Timber.i("No notes file on Drive yet");
                return null;
            }
            fileId = files.getJSONObject(0).getString("id");
        } catch (JSONException e) {
            throw new IOException("Malformed Drive response", e);
        }

        HttpUrl contentUrl = HttpUrl.get(FILES_URL + "/" + fileId).newBuilder()
                .addQueryParameter("alt", "media")
                .build();
        return new RemoteFile(fileId, execute(new Request.Builder().url(contentUrl).get()));
    }

    public void save(String fileId, String content) throws IOException {
        Request.Builder request;
        if (fileId == null) {
            String metadata = "{\"name\":\"" + FILE_NAME + "\",\"parents\":[\"appDataFolder\"]}";
            RequestBody body = new MultipartBody.Builder()
                    .setType(MULTIPART_RELATED)
                    .addPart(RequestBody.create(metadata, JSON))
                    .addPart(RequestBody.create(content, JSON))
                    .build();
            HttpUrl url = HttpUrl.get(UPLOAD_URL).newBuilder()
                    .addQueryParameter("uploadType", "multipart")
                    .build();
            request = new Request.Builder().url(url).post(body);
        } else {
            HttpUrl url = HttpUrl.get(UPLOAD_URL + "/" + fileId).newBuilder()
                    .addQueryParameter("uploadType", "media")
                    .build();
            request = new Request.Builder().url(url).patch(RequestBody.create(content, JSON));
        }
        execute(request);
    }

    private String execute(Request.Builder builder) throws IOException {
        Request request = builder.header("Authorization", "Bearer " + accessToken).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String text = body != null ? body.string() : "";
            Timber.d("Drive %s %s -> HTTP %d", request.method(), request.url().encodedPath(), response.code());
            if (!response.isSuccessful()) {
                throw new DriveApiException(response.code(), "Drive API error " + response.code() + ": " + text);
            }
            return text;
        }
    }
}
