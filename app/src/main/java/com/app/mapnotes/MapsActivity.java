package com.app.mapnotes;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        ImageView add = findViewById(R.id.add);
        add.setOnClickListener(view -> showAddNoteDialog());

        ImageView settings = findViewById(R.id.settings);
        settings.setOnClickListener(view -> {
            startActivity(new Intent(MapsActivity.this, SettingsActivity.class));
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkersFromFirestore();

        LatLng startPoint = new LatLng(51.1657, 10.4515); // Deutschland-Koordinaten
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 6));

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Klick auf Infofenster -> Bearbeiten/Löschen-Dialog
        mMap.setOnInfoWindowClickListener(marker -> {
            String docId = (String) marker.getTag();
            if (docId != null) {
                showEditDeleteDialog(marker, docId);
            }
        });
    }

    private void showAddNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_note, null);

        EditText titleInput = view.findViewById(R.id.title_input);
        EditText descInput = view.findViewById(R.id.desc_input);

        builder.setView(view)
                .setTitle("Notiz hinzufügen")
                .setPositiveButton("Speichern", null)
                .setNegativeButton("Abbrechen", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.textColor));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.color10));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();

            if (title.isEmpty()) {
                titleInput.setError("Der Titel darf nicht leer sein");
                return;
            }
            if (desc.isEmpty()) {
                descInput.setError("Die Beschreibung darf nicht leer sein");
                return;
            }

            LatLng position = mMap.getCameraPosition().target;
            saveNoteToFirestore(title, desc, position.latitude, position.longitude);
            dialog.dismiss();
        });
    }

    private void saveNoteToFirestore(String title, String description, double lat, double lng) {
        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("description", description);
        note.put("lat", lat);
        note.put("lng", lng);
        note.put("userId", userId);
        note.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notes").add(note)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Notiz wurde gespeichert", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadMarkersFromFirestore() {
        db.collection("notes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    mMap.clear(); // verhindert doppelte Marker

                    for (DocumentSnapshot doc : Objects.requireNonNull(value).getDocuments()) {
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        String title = doc.getString("title");
                        String desc = doc.getString("description");

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(title)
                                    .snippet(desc));
                            if (marker != null) {
                                marker.setTag(doc.getId()); // Firestore-Dokument-ID merken
                            }
                        }
                    }
                });
    }

    private void showEditDeleteDialog(Marker marker, String docId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        builder.setTitle(marker.getTitle())
                .setItems(new String[]{"Bearbeiten", "Löschen"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditNoteDialog(marker, docId);
                    } else if (which == 1) {
                        db.collection("notes").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    marker.remove();
                                    Toast.makeText(this, "Notiz gelöscht", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showEditNoteDialog(Marker marker, String docId) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_note, null);
        EditText titleInput = view.findViewById(R.id.title_input);
        EditText descInput = view.findViewById(R.id.desc_input);

        titleInput.setText(marker.getTitle());
        descInput.setText(marker.getSnippet());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialog)
                .setView(view)
                .setTitle("Notiz bearbeiten")
                .setPositiveButton("Speichern", null)
                .setNegativeButton("Abbrechen", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.textColor));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.color10));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newTitle = titleInput.getText().toString().trim();
            String newDesc = descInput.getText().toString().trim();

            if (newTitle.isEmpty()) {
                titleInput.setError("Titel darf nicht leer sein");
                return;
            }
            if (newDesc.isEmpty()) {
                descInput.setError("Beschreibung darf nicht leer sein");
                return;
            }

            db.collection("notes").document(docId)
                    .update("title", newTitle, "description", newDesc)
                    .addOnSuccessListener(aVoid -> {
                        marker.setTitle(newTitle);
                        marker.setSnippet(newDesc);
                        marker.showInfoWindow();
                        Toast.makeText(this, "Notiz aktualisiert", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            View view = getLayoutInflater().inflate(R.layout.info_window, null);

            TextView title = view.findViewById(R.id.info_window_title);
            TextView description = view.findViewById(R.id.info_window_description);

            title.setText(marker.getTitle());
            description.setText(marker.getSnippet());

            return view;
        }

        @Override
        public View getInfoContents(@NonNull Marker marker) {
            return null;
        }
    }
}