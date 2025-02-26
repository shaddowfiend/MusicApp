package com.example.lucrareindividuala;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AddSongActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int PICK_AUDIO_REQUEST = 200;
    private String selectedFilePath;
    private MusicDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song);

        Button saveButton = findViewById(R.id.saveButton);
        dbHelper = new MusicDatabaseHelper(this);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkAndRequestPermissions()) {
                    // Nu continuați dacă permisiunea nu este acordată
                    Toast.makeText(AddSongActivity.this, "Please grant storage permission", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Deschide File Explorer pentru alegerea unui fișier audio
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("audio/*"); // Restrânge la fișiere audio
                startActivityForResult(intent, PICK_AUDIO_REQUEST);
            }
        });
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Pentru Android 13 sau mai nou
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_AUDIO)) {
                    // Arătați un mesaj explicativ
                    Toast.makeText(this, "Storage permission is required to access files", Toast.LENGTH_SHORT).show();
                } else {
                    // Direcționați utilizatorul spre setări dacă permisiunea este blocată permanent
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)) {
                        Toast.makeText(this, "Please enable storage permission in app settings", Toast.LENGTH_SHORT).show();
                        openAppSettings();
                        return false;
                    }
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        REQUEST_CODE_PERMISSIONS);
                return false;
            }
        } else {
            // Pentru versiuni anterioare
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Arătați un mesaj explicativ
                    Toast.makeText(this, "Storage permission is required to access files", Toast.LENGTH_SHORT).show();
                } else {
                    // Direcționați utilizatorul spre setări dacă permisiunea este blocată permanent
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "Please enable storage permission in app settings", Toast.LENGTH_SHORT).show();
                        openAppSettings();
                        return false;
                    }
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSIONS);
                return false;
            }
        }
        return true; // Permisiunea este acordată
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisiune acordată
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permisiune refuzată
                Toast.makeText(this, "Permission denied. Cannot access files.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            selectedFilePath = getPathFromUri(selectedFileUri);

            if (selectedFilePath == null) {
                Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidAudioFile(selectedFilePath)) {
                Toast.makeText(this, "Invalid file format. Only MP3 files are allowed.", Toast.LENGTH_SHORT).show();
                return;
            }

            String songTitle = getSongTitleFromPath(selectedFilePath);
            String username = getIntent().getStringExtra("username");

            if (dbHelper.addSong(songTitle, selectedFilePath, username)) {
                Toast.makeText(this, "Song added successfully", Toast.LENGTH_SHORT).show();

                // Trimiteți un rezultat înapoi către MainActivity
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Failed to add song", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) return null;

        if (DocumentsContract.isDocumentUri(this, uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isExternalStorageDocument(uri)) {
                String[] parts = documentId.split(":");
                if (parts.length > 1) {
                    return Environment.getExternalStorageDirectory() + "/" + parts[1];
                }
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(documentId));
                return getDataColumn(contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String[] split = documentId.split(":");
                Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Audio.Media.DATA}, selection, selectionArgs, null);
        if (cursor == null) return null;

        try {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private boolean isValidAudioFile(String filePath) {
        if (filePath == null) return false;
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        return "mp3".equalsIgnoreCase(extension);
    }

    private String getSongTitleFromPath(String filePath) {
        if (filePath == null) return "Unknown";
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
}