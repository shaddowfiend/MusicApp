package com.example.lucrareindividuala;

import android.database.Cursor;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_SONG_REQUEST_CODE = 101; // Declarați constanta

    private ListView songsListView;
    private Button addSongButton, friendsButton;
    private MusicDatabaseHelper dbHelper;
    private String username;

    private MediaPlayer mediaPlayer = null;
    private String currentSongPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Username is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        songsListView = findViewById(R.id.songsListView);
        addSongButton = findViewById(R.id.addSongButton);
        friendsButton = findViewById(R.id.friendsButton);
        dbHelper = new MusicDatabaseHelper(this);

        loadSongs(username);

        addSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddSongActivity.class);
                intent.putExtra("username", username);
                startActivityForResult(intent, ADD_SONG_REQUEST_CODE); // Utilizați constanta declarată
            }
        });

        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FriendsActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });

        songsListView.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = dbHelper.getSongs(username);
            if (cursor != null && cursor.moveToPosition(position)) {
                try {
                    int titleIndex = cursor.getColumnIndex(MusicDatabaseHelper.COLUMN_SONG_TITLE);
                    int pathIndex = cursor.getColumnIndex(MusicDatabaseHelper.COLUMN_SONG_PATH);

                    if (titleIndex != -1 && pathIndex != -1) {
                        String songTitle = cursor.getString(titleIndex);
                        String songPath = cursor.getString(pathIndex);

                        currentSongPath = songPath;
                        showMusicControlsDialog(songTitle, songPath);
                    } else {
                        Toast.makeText(this, "Error: Song data is missing", Toast.LENGTH_SHORT).show();
                    }
                } finally {
                    cursor.close(); // Închideți cursorul chiar dacă apare o eroare
                }
            }
        });
    }

    private void showMusicControlsDialog(String songTitle, String songPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_music_controls, null);

        builder.setTitle("Now Playing: " + songTitle);

        Button playButton = dialogView.findViewById(R.id.playButton);
        Button pauseButton = dialogView.findViewById(R.id.pauseButton);
        Button stopButton = dialogView.findViewById(R.id.stopButton);

        playButton.setOnClickListener(v -> startPlaying(songPath));
        pauseButton.setOnClickListener(v -> pausePlaying());
        stopButton.setOnClickListener(v -> stopPlaying());

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startPlaying(String songPath) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(songPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing: " + songPath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void pausePlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            currentSongPath = null;
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadSongs(String username) {
        Cursor cursor = dbHelper.getSongs(username);
        ArrayList<String> songs = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int titleIndex = cursor.getColumnIndex(MusicDatabaseHelper.COLUMN_SONG_TITLE);
                if (titleIndex != -1) {
                    songs.add(cursor.getString(titleIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs);
        songsListView.setAdapter(adapter);

        // Adăugarea listener-ului pentru apasare lungă
        songsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String songTitle = songs.get(position);

            // Confirmați ștergerea
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this song?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (dbHelper.removeSong(username, songTitle)) {
                            Toast.makeText(MainActivity.this, "Song deleted successfully", Toast.LENGTH_SHORT).show();
                            loadSongs(username); // Reîncărcați lista de piese
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to delete song", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true; // Consumăm evenimentul
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_SONG_REQUEST_CODE && resultCode == RESULT_OK) { // Utilizați constanta declarată
            loadSongs(username);
        }
    }
}