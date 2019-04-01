package com.doggzam.doggzam;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.grpc.internal.IoUtils;

public class PredictTask extends AsyncTask<String, Void, String> {

    private static final Integer JSON_RAW_RESOURCE_ID = R.raw.doggzamapp; // example: R.raw.workshopvision;

    private final MainActivityInteraction listener;
    private final WeakReference<Context> weakReference;

    PredictTask(MainActivityInteraction listener, WeakReference<Context> weakReference) {
        this.listener = listener;
        this.weakReference = weakReference;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // TODO: 3/4/2019 interface back to MainActivity to update imageDetails
        listener.setImageDetailsText("Analysing image...");
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
            InputStream inputStream = weakReference.get().getResources().openRawResource(JSON_RAW_RESOURCE_ID);

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

            InputStream inputStreamImage = weakReference.get().getContentResolver().openInputStream(uri);
            assert inputStreamImage != null;
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

            StringBuilder res = new StringBuilder();
            res.append("Prediction results:");
            for (AnnotationPayload annotationPayload : response.getPayloadList()) {
                res.append("\nPredicted class name: ").append(annotationPayload.getDisplayName());
                res.append("\nPredicted class score: ").append(annotationPayload.getClassification().getScore());
            }

            Log.d("automl", res.toString());
            return res.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        listener.setImageDetailsText(result);
    }
}
