package com.example.cam_app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface; // Use androidx version

import com.bumptech.glide.Glide;
import com.example.cam_app.databinding.ActivityImageDetailsBinding; // Import ViewBinding

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class ImageDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ImageDetailsActivity";
    public static final String EXTRA_IMAGE_ITEM = "extra_image_item";

    private ActivityImageDetailsBinding binding;
    private ImageItem currentImageItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve the ImageItem
        if (getIntent().hasExtra(EXTRA_IMAGE_ITEM)) {
            // Handle potential Parcelable deprecation for API 33+ if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentImageItem = getIntent().getParcelableExtra(EXTRA_IMAGE_ITEM, ImageItem.class);
            } else {
                // Use deprecated method for older APIs
                currentImageItem = getIntent().getParcelableExtra(EXTRA_IMAGE_ITEM);
            }
        }


        if (currentImageItem == null) {
            Toast.makeText(this, "Error: Image details not found", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ImageItem extra was null or incorrect type");
            finish(); // Close activity if no data
            return;
        }

        displayImageDetails(currentImageItem);

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void displayImageDetails(ImageItem item) {
        // Load full image
        Glide.with(this)
                .load(item.getUri())
                .error(android.R.drawable.ic_dialog_alert) // Error placeholder
                .into(binding.imageViewFull);

        // Display basic info
        binding.tvDetailName.setText(item.getName());
        binding.tvDetailPath.setText(getPathFromUri(item.getUri())); // Display path
        binding.tvDetailSize.setText(formatFileSize(item.getSize()));

        // Get Date Taken (try EXIF first)
        Long exifDate = getExifDateTaken(item.getUri());
        long dateToDisplay = (exifDate != null) ? exifDate : item.getDateTaken();
        binding.tvDetailDate.setText(formatDate(dateToDisplay));
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteImage() {
        if (currentImageItem == null) return;

        Uri imageUri = currentImageItem.getUri();
        try {
            // Use DocumentFile for deletion as we are using SAF URIs
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, imageUri);

            if (documentFile != null && documentFile.exists() && documentFile.canWrite()) {
                if (documentFile.delete()) {
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Deleted image: " + imageUri);
                    // Set result to OK so MainActivity knows to refresh
                    setResult(Activity.RESULT_OK);
                    finish(); // Go back to MainActivity
                } else {
                    throw new IOException("DocumentFile.delete() returned false for URI: " + imageUri);
                }
            } else if (documentFile == null) {
                throw new IOException("Could not get DocumentFile from URI. Is it a valid SAF URI? URI: " + imageUri);
            } else if (!documentFile.exists()){
                throw new IOException("File does not exist for deletion. URI: " + imageUri);
            }
            else { // Cannot write
                throw new SecurityException("No write permission for URI: " + imageUri);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting image: " + imageUri, e);
            Toast.makeText(this, "Error deleting image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Optionally set result to CANCELED or do nothing
            // setResult(Activity.RESULT_CANCELED);
        }
    }


    // --- Utility Methods ---

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        // Ensure digitGroups is within the bounds of the units array
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            // Optional: Set timezone if needed, otherwise uses device default
            // sdf.setTimeZone(TimeZone.getDefault());
            Date date = new Date(timestamp);
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date timestamp: " + timestamp, e);
            return "N/A";
        }
    }

    private Long getExifDateTaken(Uri imageUri) {
        ContentResolver resolver = getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(imageUri)) {
            if (inputStream == null) {
                Log.w(TAG, "Could not open InputStream for EXIF reading: " + imageUri);
                return null;
            }

            ExifInterface exifInterface = new ExifInterface(inputStream);

            // Try common EXIF date tags
            String dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (dateString == null) {
                dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            }
            if (dateString == null) {
                dateString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
            }

            if (dateString != null) {
                // EXIF format is typically "yyyy:MM:dd HH:mm:ss"
                SimpleDateFormat exifSdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
                // Important: Set timezone to UTC or default if EXIF doesn't specify one,
                // otherwise parsing might be off depending on device timezone.
                // Often, it's safer to parse assuming a common standard like UTC if timezone
                // info isn't embedded, but device local might be okay for simple display.
                // exifSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date date = exifSdf.parse(dateString);
                    return (date != null) ? date.getTime() : null;
                } catch (ParseException e) {
                    Log.w(TAG, "Failed to parse EXIF date string: " + dateString + " for URI: " + imageUri, e);
                }
            }
        } catch (IOException | IllegalArgumentException | StackOverflowError e) {
            // Ignore errors (file not found, format error, large EXIF, etc.)
            Log.e(TAG, "Could not read EXIF data for " + imageUri, e);
        }
        return null; // Return null if tag not found or error occurred
    }

    private String getPathFromUri(Uri uri) {
        // Attempt to get a more user-friendly path for SAF URIs
        if (DocumentsContract.isDocumentUri(this, uri)) {
            DocumentFile docFile = DocumentFile.fromSingleUri(this, uri);
            if (docFile != null && docFile.getName() != null) {
                // Often the best we can easily get is the filename itself for SAF URIs
                return docFile.getName();
            } else {
                // Fallback for Document URIs where name isn't available
                return uri.getPath(); // Might return something like /document/primary:Pictures/image.jpg
            }
        } else {
            // For non-document URIs (like file:// URIs, less common now)
            return uri.getPath();
        }
        // Absolute fallback
        // return uri.toString();
    }
}