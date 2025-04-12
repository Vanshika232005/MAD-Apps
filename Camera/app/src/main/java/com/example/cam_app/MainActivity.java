package com.example.cam_app;

import android.Manifest; // Import Manifest
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager; // Import PackageManager
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.cam_app.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private ImageGridAdapter imageGridAdapter;
    private final List<ImageItem> imageList = new ArrayList<>();
    private Uri selectedFolderUri = null;
    private Uri tempPhotoUri = null;

    // --- ActivityResultLaunchers ---

    // Launcher for choosing a folder (Unchanged)
    private final ActivityResultLauncher<Uri> openDirectoryLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    selectedFolderUri = uri;
                    binding.tvSelectedFolder.setText("Selected: " + getFolderName(uri));
                    Log.d(TAG, "Selected folder URI: " + uri.toString());
                    loadImagesFromFolder(uri);
                } else {
                    Log.d(TAG, "Folder selection cancelled");
                    Toast.makeText(this, "Folder selection cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher for taking a photo (Unchanged)
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    Log.d(TAG, "Photo taken successfully at temp URI: " + tempPhotoUri);
                    if (tempPhotoUri != null) {
                        saveImageToSelectedFolder(tempPhotoUri);
                    }
                } else {
                    Log.e(TAG, "Photo capture failed or was cancelled.");
                    if (tempPhotoUri != null) {
                        getContentResolver().delete(tempPhotoUri, null, null);
                    }
                    Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                }
                tempPhotoUri = null;
            });

    // Launcher for Image Details Activity (Unchanged)
    private final ActivityResultLauncher<Intent> imageDetailsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Returned from Details, image deleted. Refreshing.");
                    if (selectedFolderUri != null) {
                        loadImagesFromFolder(selectedFolderUri);
                    }
                } else {
                    Log.d(TAG, "Returned from Details, no changes.");
                }
            });

    // *** NEW: Launcher for requesting Camera permission ***
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted by user.");
                    // Permission is granted. Continue the action (launch camera).
                    launchCamera();
                } else {
                    Log.w(TAG, "Camera permission denied by user.");
                    // Explain to the user that the feature is unavailable because
                    // the permissions were denied. You could optionally show a dialog.
                    Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_LONG).show();
                }
            });


    // --- Lifecycle Methods --- (Unchanged)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupButtonClickListeners();
    }

    // --- Setup Methods ---

    // setupRecyclerView() (Unchanged)
    private void setupRecyclerView() {
        ImageGridAdapter.OnItemClickListener listener = item -> {
            Intent intent = new Intent(MainActivity.this, ImageDetailsActivity.class);
            intent.putExtra(ImageDetailsActivity.EXTRA_IMAGE_ITEM, item);
            imageDetailsLauncher.launch(intent);
        };
        imageGridAdapter = new ImageGridAdapter(imageList, listener);
        binding.recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerViewImages.setAdapter(imageGridAdapter);
    }

    // setupButtonClickListeners() (Modified)
    private void setupButtonClickListeners() {
        binding.btnChooseFolder.setOnClickListener(v -> {
            openDirectoryLauncher.launch(null);
        });

        binding.btnTakePhoto.setOnClickListener(v -> {
            // *** MODIFIED: Check permission before launching camera ***
            checkCameraPermissionAndLaunch();
        });
    }

    // --- Core Logic Methods ---

    // *** NEW: Method to check permission and decide whether to launch or request ***
    private void checkCameraPermissionAndLaunch() {
        // 0. Check if a folder is selected first (existing logic)
        if (selectedFolderUri == null) {
            Toast.makeText(this, "Please choose a folder first", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            Log.d(TAG, "Camera permission already granted. Launching camera.");
            launchCamera();
        }
        // 2. Optionally check if you should show rationale (if denied previously)
        // else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected, and what
        // features are disabled if it's declined. In this UI, include a
        // "cancel" or "no thanks" button that lets the user continue
        // using your app without granting the permission.
        // Show your rationale UI and then launch the permission request launcher.
        // For simplicity, we'll skip explicit rationale here and directly request.
        // }
        // 3. Request the permission
        else {
            // Directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.d(TAG, "Camera permission not granted. Requesting permission...");
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }


    // launchCamera() (Unchanged - but now only called *after* permission check/grant)
    private void launchCamera() {
        tempPhotoUri = createImageFileUri();
        if (tempPhotoUri == null) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to create temp photo URI");
            return;
        }
        Log.d(TAG, "Launching camera, output URI: " + tempPhotoUri);
        try {
            takePictureLauncher.launch(tempPhotoUri);
        } catch (Exception e) {
            // Catch potential exceptions during launch (e.g., ActivityNotFoundException if no camera app)
            Log.e(TAG, "Error launching camera intent", e);
            Toast.makeText(this, "Could not launch camera app. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Clean up temp file if launch fails
            if (tempPhotoUri != null) {
                getContentResolver().delete(tempPhotoUri, null, null);
                tempPhotoUri = null;
            }
        }
    }

    // createImageFileUri() (Unchanged)
    private Uri createImageFileUri() {
        // ... (keep the existing code)
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getCacheDir();
            File imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            Log.d(TAG, "Temp file created at: " + imageFile.getAbsolutePath());
            return FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    imageFile);
        } catch (IOException ex) {
            Log.e(TAG, "Error creating temp file URI", ex);
            return null;
        }
    }


    // saveImageToSelectedFolder() (Unchanged)
    private void saveImageToSelectedFolder(Uri sourceUri) {
        // ... (keep the existing code)
        if (selectedFolderUri == null) {
            Toast.makeText(this, "No destination folder selected", Toast.LENGTH_SHORT).show();
            getContentResolver().delete(sourceUri, null, null); // Clean up temp
            return;
        }

        DocumentFile destinationFolder = DocumentFile.fromTreeUri(this, selectedFolderUri);

        if (destinationFolder == null || !destinationFolder.canWrite()) {
            Toast.makeText(this, "Cannot write to selected folder", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Selected folder is null or not writable: " + selectedFolderUri);
            getContentResolver().delete(sourceUri, null, null); // Clean up temp
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        DocumentFile newFile = destinationFolder.createFile("image/jpeg", fileName);

        if (newFile == null) {
            Toast.makeText(this, "Failed to create image file in folder", Toast.LENGTH_LONG).show();
            Log.e(TAG, "destinationFolder.createFile returned null for folder: " + destinationFolder.getUri());
            getContentResolver().delete(sourceUri, null, null); // Clean up temp
            return;
        }

        Log.d(TAG, "Attempting to copy " + sourceUri + " to " + newFile.getUri());

        ContentResolver resolver = getContentResolver();
        try (InputStream in = resolver.openInputStream(sourceUri);
             OutputStream out = resolver.openOutputStream(newFile.getUri())) {

            if (in == null || out == null) {
                throw new IOException("Failed to open streams for copy operation.");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "Successfully copied image to " + newFile.getUri());
            Toast.makeText(this, "Image saved to folder", Toast.LENGTH_SHORT).show();
            loadImagesFromFolder(selectedFolderUri); // Refresh list

        } catch (Exception e) {
            Log.e(TAG, "Error copying image", e);
            Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (newFile.exists()) {
                newFile.delete();
            }
        } finally {
            int deletedRows = resolver.delete(sourceUri, null, null);
            Log.d(TAG, "Deleted temp file " + sourceUri + " - Rows affected: " + deletedRows);
        }
    }

    // loadImagesFromFolder() (Unchanged)
    private void loadImagesFromFolder(Uri folderUri) {
        // ... (keep the existing code)
        Log.d(TAG, "Loading images from: " + folderUri);
        ContentResolver contentResolver = getContentResolver();
        DocumentFile directory = DocumentFile.fromTreeUri(this, folderUri);

        if (directory == null || !directory.isDirectory() || !directory.canRead()) {
            Log.e(TAG, "Invalid folder URI, not a directory, or cannot read: " + folderUri);
            binding.tvSelectedFolder.setText("Error loading folder");
            imageList.clear();
            if (imageGridAdapter != null) {
                imageGridAdapter.updateData(new ArrayList<>(imageList));
            }
            return;
        }

        imageList.clear();

        for (DocumentFile file : directory.listFiles()) {
            String fileType = file.getType();
            if (file.isFile() && fileType != null && fileType.startsWith("image/")) {
                try {
                    String name = file.getName();
                    if (name == null) name = "Unknown";
                    long size = file.length();
                    long date = file.lastModified();
                    imageList.add(new ImageItem(file.getUri(), name, size, date));
                    Log.d(TAG, "Found image: " + name + ", URI: " + file.getUri());
                } catch (Exception e) {
                    Log.e(TAG, "Error processing file: " + (file.getName() != null ? file.getName() : "Unknown"), e);
                }
            }
        }

        Collections.sort(imageList, (o1, o2) -> Long.compare(o2.getDateTaken(), o1.getDateTaken()));

        Log.d(TAG, "Loaded " + imageList.size() + " images");
        if (imageGridAdapter != null) {
            imageGridAdapter.updateData(new ArrayList<>(imageList));
        }

        if (imageList.isEmpty()) {
            Toast.makeText(this, "No images found in this folder", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Utility Methods --- (Unchanged)
    private String getFolderName(Uri uri) {
        // ... (keep the existing code)
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
        if (documentFile != null && documentFile.getName() != null) {
            return documentFile.getName();
        }
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments != null && !pathSegments.isEmpty()) {
            return pathSegments.get(pathSegments.size() - 1);
        }
        return "Unknown Folder";
    }
}