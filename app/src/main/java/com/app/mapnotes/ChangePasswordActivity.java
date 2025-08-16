package com.app.mapnotes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword;
    private Button btnSave;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color60));

        mAuth = FirebaseAuth.getInstance();

        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar_reset);
        backBtn = findViewById(R.id.backButton);

        setSaveButtonEnabled(false);

        backBtn.setOnClickListener(v -> finish());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        };

        etOldPassword.addTextChangedListener(textWatcher);
        etNewPassword.addTextChangedListener(textWatcher);

        btnSave.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(ChangePasswordActivity.this, "Bitte altes und neues Passwort eingeben", Toast.LENGTH_SHORT).show();
                return;
            }
            changePassword(oldPassword, newPassword);
        });
    }


    private void updateSaveButtonState() {
        boolean enable = !etOldPassword.getText().toString().trim().isEmpty() &&
                !etNewPassword.getText().toString().trim().isEmpty();
        setSaveButtonEnabled(enable);
    }


    private void setSaveButtonEnabled(boolean enabled) {
        btnSave.setEnabled(enabled);
        btnSave.setAlpha(enabled ? 1.0f : 0.5f);
    }


    private void changePassword(String oldPassword, String newPassword) {
        setSaveButtonEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "Benutzer nicht angemeldet oder keine E-Mail vorhanden", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            setSaveButtonEnabled(true);
            return;
        }

        String email = currentUser.getEmail();
        mAuth.signInWithEmailAndPassword(email, oldPassword)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ChangePasswordActivity.this, "Passwort erfolgreich geändert!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(ChangePasswordActivity.this, "Fehler beim Ändern des Passworts: "
                                                + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        setSaveButtonEnabled(true);
                                    }
                                });
                    } else {
                        setSaveButtonEnabled(true);
                        Toast.makeText(ChangePasswordActivity.this, "Anmeldung fehlgeschlagen: "
                                + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
