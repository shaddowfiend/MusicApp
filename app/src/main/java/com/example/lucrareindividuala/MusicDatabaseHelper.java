package com.example.lucrareindividuala;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MusicDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MusicApp.db";
    private static final int DATABASE_VERSION = 1;

    // Tabele
    public static final String TABLE_USERS = "users";
    public static final String TABLE_SONGS = "songs";
    public static final String TABLE_FRIENDS = "friends";

    // Coloane
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_SONG_TITLE = "title";
    public static final String COLUMN_SONG_PATH = "path";
    public static final String COLUMN_CURRENTLY_PLAYING = "currently_playing";
    public static final String COLUMN_FRIEND_USERNAME = "friend_username";

    public MusicDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_CURRENTLY_PLAYING + " TEXT)";

        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SONG_TITLE + " TEXT, " +
                COLUMN_SONG_PATH + " TEXT, " +
                COLUMN_USERNAME + " TEXT)";

        String CREATE_FRIENDS_TABLE = "CREATE TABLE " + TABLE_FRIENDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_FRIEND_USERNAME + " TEXT)";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_SONGS_TABLE);
        db.execSQL(CREATE_FRIENDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIENDS);
        onCreate(db);
    }

    // Adaugă un utilizator
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Verifică autentificarea
    public Cursor getUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, new String[]{COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?", new String[]{username, password}, null, null, null);
    }

    // Adaugă o piesă
    public boolean addSong(String title, String path, String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SONG_TITLE, title);
        values.put(COLUMN_SONG_PATH, path);
        values.put(COLUMN_USERNAME, username);
        long result = db.insert(TABLE_SONGS, null, values);
        return result != -1;
    }

    // Obține piesele utilizatorului
    public Cursor getSongs(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SONGS, new String[]{COLUMN_ID, COLUMN_SONG_TITLE, COLUMN_SONG_PATH},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
    }

    // Adaugă un prieten
    public boolean addFriend(String username, String friendUsername) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Verificăm dacă prietenul există deja în tabelul de prieteni
        Cursor cursor = db.query(TABLE_FRIENDS, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_FRIEND_USERNAME + "=?",
                new String[]{username, friendUsername}, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            return false; // Prietenul există deja
        }

        // Dacă prietenul nu există, îl adăugăm
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_FRIEND_USERNAME, friendUsername);

        long result = db.insert(TABLE_FRIENDS, null, values);
        return result != -1; // Returnează true dacă inserarea a reușit
    }

    // Obține lista de prieteni
    public Cursor getFriends(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_FRIENDS, new String[]{COLUMN_FRIEND_USERNAME},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
    }

    // Actualizează piesa curentă ascultată
    public boolean updateCurrentlyPlaying(String username, String songTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CURRENTLY_PLAYING, songTitle);
        int result = db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        return result > 0;
    }
    public boolean removeFriend(String username, String friendUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_FRIENDS,
                COLUMN_USERNAME + "=? AND " + COLUMN_FRIEND_USERNAME + "=?",
                new String[]{username, friendUsername});
        return rowsDeleted > 0; // Returnează true dacă ștergerea a reușit
    }
    public boolean removeSong(String username, String songTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_SONGS,
                COLUMN_USERNAME + "=? AND " + COLUMN_SONG_TITLE + "=?",
                new String[]{username, songTitle});
        return rowsDeleted > 0; // Returnează true dacă ștergerea a reușit
    }
    // Obține piesa curentă ascultată de un utilizator
    public String getCurrentlyPlaying(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_CURRENTLY_PLAYING},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(COLUMN_CURRENTLY_PLAYING));
        }
        return null;
    }
}