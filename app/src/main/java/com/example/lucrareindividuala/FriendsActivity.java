package com.example.lucrareindividuala;


import android.database.Cursor;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity {

    private ListView friendsListView;
    private Button addButton;
    private EditText friendUsernameEditText;
    private MusicDatabaseHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Username is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        friendsListView = findViewById(R.id.friendsListView);
        addButton = findViewById(R.id.addButton);
        friendUsernameEditText = findViewById(R.id.friendUsernameEditText);
        dbHelper = new MusicDatabaseHelper(this);

        loadFriends();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String friendUsername = friendUsernameEditText.getText().toString().trim();

                if (friendUsername.isEmpty()) {
                    Toast.makeText(FriendsActivity.this, "Please enter a valid username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.addFriend(username, friendUsername)) {
                    Toast.makeText(FriendsActivity.this, "Friend added successfully", Toast.LENGTH_SHORT).show();
                    loadFriends(); // Reîncărcați lista de prieteni
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to add friend or friend already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Adăugarea listener-ului pentru apasare lungă (ștergere prieten)
        friendsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Cursor cursor = dbHelper.getFriends(username);
            if (cursor != null && cursor.moveToPosition(position)) {
                try {
                    String friendUsername = cursor.getString(cursor.getColumnIndex(MusicDatabaseHelper.COLUMN_FRIEND_USERNAME));

                    new AlertDialog.Builder(FriendsActivity.this)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to remove this friend?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                if (dbHelper.removeFriend(username, friendUsername)) {
                                    Toast.makeText(FriendsActivity.this, "Friend removed successfully", Toast.LENGTH_SHORT).show();
                                    loadFriends(); // Reîncărcați lista de prieteni
                                } else {
                                    Toast.makeText(FriendsActivity.this, "Failed to remove friend", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                } finally {
                    cursor.close();
                }
            }
            return true; // Consumăm evenimentul
        });
    }

    private void loadFriends() {
        Cursor cursor = dbHelper.getFriends(username);
        ArrayList<String> friends = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                friends.add(cursor.getString(cursor.getColumnIndex(MusicDatabaseHelper.COLUMN_FRIEND_USERNAME)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friends);
        friendsListView.setAdapter(adapter);
    }
}