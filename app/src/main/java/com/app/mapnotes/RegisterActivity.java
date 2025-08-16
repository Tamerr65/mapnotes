package com.app.mapnotes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth auth;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color60));

        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        registerButton = findViewById(R.id.register_btn);
        loginTextView = findViewById(R.id.login_btn);

        auth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> register());
        loginTextView.setOnClickListener(v -> login());
    }

    // Wechsel zurück zum Login-Screen
    private void login() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Registrierungsprozess mit Validierung, Firebase-Auth und Benutzerprofil-Update
    private void register() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }

        setInputsEnabled(false);
        showLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        showLoading(false);
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registrierung erfolgreich", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, MapsActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Fehler beim Aktualisieren des Profils: "
                                                    + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            setInputsEnabled(true);
                                        }
                                    });
                        } else {
                            showLoading(false);
                            Toast.makeText(RegisterActivity.this, "Fehler: Benutzer konnte nicht abgerufen werden", Toast.LENGTH_SHORT).show();
                            setInputsEnabled(true);
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, "Registrierung fehlgeschlagen: "
                                + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        setInputsEnabled(true);
                    }
                });
    }

    // Aktiviert alle Eingabefelder und den Button
    private void setInputsEnabled(boolean enabled) {
        nameInput.setEnabled(enabled);
        emailInput.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        confirmPasswordInput.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    // Überprüfung der Eingaben (Name, Email, Passwort)
    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Bitte Namen eingeben", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Bitte E-Mail eingeben", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Bitte eine gültige E-Mail eingeben", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Bitte Passwort eingeben", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Das Passwort muss mindestens 6 Zeichen lang sein", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwörter stimmen nicht überein", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean loading) {
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        View dimOverlay = findViewById(R.id.dim_overlay);

        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            dimOverlay.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            dimOverlay.setVisibility(View.GONE);
        }
    }
}