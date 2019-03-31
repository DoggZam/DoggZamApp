package com.doggzam.doggzam;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.automl.v1beta1.AnnotationPayload;
import com.google.cloud.automl.v1beta1.ExamplePayload;
import com.google.cloud.automl.v1beta1.Image;
import com.google.cloud.automl.v1beta1.ModelName;
import com.google.cloud.automl.v1beta1.PredictResponse;
import com.google.cloud.automl.v1beta1.PredictionServiceClient;
import com.google.cloud.automl.v1beta1.PredictionServiceSettings;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import io.grpc.internal.IoUtils;

public class MainActivity extends AppCompatActivity {

    // TODO: insert project info
    private static final String COMPUTE_REGION = "us-central1"; // example: "us-central1";
    private static final String PROJECT_ID = "dogg-zam-app"; // example: "workshop-vision";
    private static final String MODEL_ID = "ICN6473335258590864622"; // example: "ICN293500301081240869";
    private static final String SCORE_THRESHOLD = "0.5"; // example: "0.7";
    private static final Integer JSON_RAW_RESOURCE_ID = R.raw.doggzamapp; // example: R.raw.workshopvision;

    public static final String FILE_NAME = "temp.jpg";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView mImageDetails;
    private ImageView mMainImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.dialog_select_prompt)
                        .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGalleryChooser();
                            }
                        })
                        .setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCamera();
                            }
                        });
                builder.create().show();
            }
        });

        mImageDetails = findViewById(R.id.image_details);
        mMainImage = findViewById(R.id.main_image);
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    /**
     *  Requests permission to use the camera before starting an CameraActivity to capture an image
     *  and return it to the MainActivity.
     */
    public void startCamera() {
        boolean permissionGranted = PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA);

        if (permissionGranted) {
            // Create an intent to capture an image
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Create a new file to store the returned image
            File cameraFile = getCameraFile();

            // Retrieve the URI for the camera file. The authority is defined in the AndroidManifest
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", cameraFile);
            // Tell the intent this image will be written to a file
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            // Grant permission to read the file's URI after the data is returned
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri imageUri) {

        if (imageUri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap = scaleBitmapDown(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri),
                        1200);

                mMainImage.setImageBitmap(bitmap);
                findViewById(R.id.image_background).setVisibility(View.VISIBLE);

                if (PROJECT_ID != null && COMPUTE_REGION != null && MODEL_ID != null && SCORE_THRESHOLD != null && imageUri != null) {
                    PredictTask predictTask = new PredictTask();
                    predictTask.execute(PROJECT_ID, COMPUTE_REGION, MODEL_ID, SCORE_THRESHOLD, imageUri.toString());
                } else {
                    mImageDetails.setText("ERROR: null prediction parameter.");
                    Log.e("automl", "ERROR: null prediction parameter.");
                }
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
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


    public class PredictTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // TODO: 3/4/2019 interface back to MainActivity to update imageDetails
            mImageDetails.setText("Analysing image...");
        }


        @Override
        protected String doInBackground(String... predictParams) {
            try {
                String projectId = predictParams[0];
                String computeRegion = predictParams[1];
                String modelId = predictParams[2];
                String scoreThreshold = predictParams[3];
                Uri uri = Uri.parse(predictParams[4]);

                // TODO: 3/5/2019 Create getScopes method inside an interface from MainActivity
                InputStream inputStream = getResources().openRawResource(JSON_RAW_RESOURCE_ID);

                GoogleCredential credential;
                credential = GoogleCredential.fromStream(inputStream);
                Collection<String> scopes = Collections.singleton("https://www.googleapis.com/auth/cloud-platform");

                if (credential.createScopedRequired()) {
                    credential = credential.createScoped(scopes);
                }

                GoogleCredentials sac = ServiceAccountCredentials.newBuilder()
                        .setPrivateKey(credential.getServiceAccountPrivateKey())
                        .setPrivateKeyId(credential.getServiceAccountPrivateKeyId())
                        .setClientEmail(credential.getServiceAccountId())
                        .setScopes(scopes)
                        // .setAccessToken(new AccessToken(credential.getAccessToken(), new LocalDate().plusYears(1).toDate()))
                        .build();

                // Latest generation Google libs, GoogleCredentials extends Credentials
                CredentialsProvider cp = FixedCredentialsProvider.create(sac);
                PredictionServiceSettings settings = PredictionServiceSettings.newBuilder().setCredentialsProvider(cp).build();

                // Instantiate client for prediction service.
                PredictionServiceClient predictionClient = PredictionServiceClient.create(settings);

                // Get the full path of the model.
                ModelName name = ModelName.of(projectId, computeRegion, modelId);

                InputStream inputStreamImage = getContentResolver().openInputStream(uri);
                byte[] bytes = IoUtils.toByteArray(inputStreamImage);
                ByteString content = ByteString.copyFrom(bytes);
                Image image = Image.newBuilder().setImageBytes(content).build();
                ExamplePayload examplePayload = ExamplePayload.newBuilder().setImage(image).build();

                // Additional parameters that can be provided for prediction e.g. Score Threshold
                Map<String, String> params = new HashMap<>();
                if (scoreThreshold != null) {
                    params.put("score_threshold", scoreThreshold);
                }
                // Perform the AutoML Prediction request
                PredictResponse response = predictionClient.predict(name, examplePayload, params);

                String res = "";
                res += "Prediction results:";
                for (AnnotationPayload annotationPayload : response.getPayloadList()) {
                    res += "\nPredicted class name: " + annotationPayload.getDisplayName();
                    res += "\nPredicted class score: " + annotationPayload.getClassification().getScore();
                }

                Log.d("automl", res);
                return res;
            } catch (IOException e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mImageDetails.setText(result);
        }
    }
}
