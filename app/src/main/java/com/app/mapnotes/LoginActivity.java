package com.app.mapnotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerTextView;
    private FirebaseAuth auth;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color60));

        sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        int darkMode = sharedPreferences.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_btn);
        registerTextView = findViewById(R.id.register_btn);

        auth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> login());
        registerTextView.setOnClickListener(v -> register());

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            startActivity(new Intent(this, MapsActivity.class));
            finish();
        }
    }

    private void login() {
        setInputsEnabled(false);
        showLoading(true);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!isValidInput(email, password)) {
            setInputsEnabled(true);
            showLoading(false);
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, MapsActivity.class));
                        finish();
                    } else {
                        showLoading(false);

                        String errorMsg = getString(R.string.login_failed);
                        if (task.getException() != null) {
                            errorMsg += ": " + task.getException().getMessage();
                            Log.e(TAG, "Login error", task.getException());
                        }

                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        setInputsEnabled(true);
                    }
                });
    }

    private boolean isValidInput(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setInputsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        emailInput.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
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


    private void register() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}