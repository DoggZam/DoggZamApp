package com.doggzam.doggzam;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

class Utils {
    private static final String COMPUTE_REGION = "us-central1";
    private static final String PROJECT_ID = "dogg-zam-app";
    private static final String MODEL_ID = "ICN6473335258590864622";
    private static final String SCORE_THRESHOLD = "0.5";
    private static final String TAG = "Utils";

    static void uploadImage(Uri imageUri, MainActivityInteraction interaction, WeakReference<Context> weakReference) {

        if (imageUri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap = scaleBitmapDown(
                        MediaStore.Images.Media.getBitmap(weakReference.get().getContentResolver(), imageUri));

                interaction.setMainImageBitmap(bitmap);

                PredictTask predictTask = new PredictTask(interaction, weakReference);
                predictTask.execute(PROJECT_ID, COMPUTE_REGION, MODEL_ID, SCORE_THRESHOLD, imageUri.toString());
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(weakReference.get(), R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(weakReference.get(), R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private static Bitmap scaleBitmapDown(Bitmap bitmap) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int maxDimension = 1200;

        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}
