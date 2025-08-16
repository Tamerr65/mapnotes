package com.app.mapnotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class SettingsActivity extends AppCompatActivity {

    FirebaseUser user;
    FirebaseAuth auth;
    FirebaseFirestore db;

    private ImageButton backButton;
    private LinearLayout changePasswordOption, logoutOption, deleteAccountOption, darkModeOption;
    private TextView versiontxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color60));

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        user = auth.getCurrentUser();


        backButton = findViewById(R.id.backButton);
        changePasswordOption = findViewById(R.id.change_password_option);
        logoutOption = findViewById(R.id.logout_option);
        deleteAccountOption = findViewById(R.id.delete_account_option);
        darkModeOption = findViewById(R.id.darkmode_option);

        versiontxt = findViewById(R.id.version_text);

        // Navigation zurück zur Hauptansicht
        backButton.setOnClickListener(v -> {

            Intent intent = new Intent(SettingsActivity.this, MapsActivity.class);
            startActivity(intent);
            finish();

        });

        // Klick-Optionen für die Einstellungen
        changePasswordOption.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class))
        );

        logoutOption.setOnClickListener(v ->
            showLogoutConfirmationDialog()
        );

        deleteAccountOption.setOnClickListener(v ->
                showDeleteAccountConfirmationDialog()
        );

        darkModeOption.setOnClickListener(v ->
                showCustomDarkModeDialog()
        );

        try {
            versiontxt.setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {

            versiontxt.setText("v?");
            throw new RuntimeException(e);
        }
    }

    // Dialog zum Abmelden mit Bestätigung
    private void showLogoutConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialog)
                .setTitle("Abmelden")
                .setMessage("Möchtest du dich wirklich abmelden?")
                .setPositiveButton("Abmelden", (Dialog, which) -> {

                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Abbrechen", null)
                .show(); // Dialog anzeigen

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.color10));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.textColor));
    }

    // Dialog zur Auswahl des Dark-Modes (An, Aus, Systemstandard)
    private void showCustomDarkModeDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialog)
                .setTitle("Dunkler Modus")
                .setItems(new String[]{"An", "Aus", "Systemstandard"}, (dialogg, which) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    switch (which) {
                        case 0: // "On" ausgewählt
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            editor.putInt("dark_mode", AppCompatDelegate.MODE_NIGHT_YES);
                            break;

                        case 1: // "Off" ausgewählt
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            editor.putInt("dark_mode", AppCompatDelegate.MODE_NIGHT_NO);
                            break;

                        case 2: // "System default" ausgewählt
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            editor.putInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                    }
                    editor.apply();
                    dialogg.dismiss();
                    recreate();
                })
                .setNegativeButton("Abbrechen", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.textColor));
    }

    // Dialog zum Bestätigen der Konto-Löschung
    private void showDeleteAccountConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialog)
                .setTitle("Konto löschen")
                .setMessage("Möchtest du deinen Account wirklich löschen? Alle deine Daten werden unwiderruflich gelöscht.")
                .setPositiveButton("Bestätigen", (Dialog, which) -> {
                    deleteAccount();
                })
                .setNegativeButton("Abbrechen", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.accentBlue));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.accentRed));
    }

    // Löscht zuerst alle Notizen des Nutzers und anschließend das Benutzerkonto
    private void deleteAccount() {
        if (user != null) {
            String userId = user.getUid();

            db.collection("notes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            WriteBatch batch = db.batch();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                batch.delete(document.getReference());
                            }

                            batch.commit().addOnCompleteListener(batchTask -> {
                                if (batchTask.isSuccessful()) {

                                    user.delete().addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(),
                                                    "Account und Daten erfolgreich gelöscht",
                                                    Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Konto konnte nicht gelöscht werden",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Notizen konnten nicht gelöscht werden",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Fehler beim Abrufen der Notizen",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}