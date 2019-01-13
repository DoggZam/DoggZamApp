package com.doggzam.doggzam;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private FirebaseVisionBarcodeDetector detector;
    private Uri photoURI;
    TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvInfo = findViewById(R.id.tv_info);

        FirebaseApp.initializeApp(this);

        FloatingActionButton fab =  findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                String imageFileName = "temp";
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "com.doggzam.fileprovider",
                        photoFile);
                Log.d("MainActivity", "onClick: " + photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {


//            Uri photoUri = Uri.parse(data.getStringExtra("URI_EXTRA"));
//
//            imageView.setImageBitmap(photo);
//            // Photo is returned. Density = 560

//            Log.d(TAG, "onActivityResult: " + photoUri);
            FirebaseVisionImage image = null;
            try {
                image = FirebaseVisionImage.fromFilePath(this, photoURI);
                File file = new File(photoURI.toString());
                file.delete();
                Log.d("MainActivity", "onActivityResult: " + image.toString());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("MainActivity", "onActivityResult: IOException");
            }

            // Get an instance of FirebaseVision
            FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                    .getVisionLabelDetector();


            // TODO: 1/13/2019 Run recognition on image
            Task<List<FirebaseVisionLabel>> result =
                    detector.detectInImage(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                        @Override
                                        public void onSuccess(List<FirebaseVisionLabel> labels) {
                                            // Task completed successfully
                                            // ...
                                            Log.d("MainActivity", "onSuccess: ");
                                            for (FirebaseVisionLabel label: labels) {
                                                String text = label.getConfidence() + ":\t" + label.getLabel() + "\n";
                                                tvInfo.append(text);
                                            }
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                            Log.d("MainActivity", "onFailure: ");
                                        }
                                    });

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
