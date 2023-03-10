package com.example.classroomwithfacedetection.UI.Attendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.classroomwithfacedetection.Untils.Constants;
import com.example.classroomwithfacedetection.Untils.FaceModel;
import com.example.classroomwithfacedetection.Untils.Untils;
import com.example.classroomwithfacedetection.databinding.ActivityDiemdanhtestBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class Attendance extends AppCompatActivity {
    private final String PATH = "mobile_face_net.tflite";
    private ImageView facePRV;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    private FaceDetector faceDetector;
    private Interpreter tflite;
    private InputImage inputImage;
    private Bitmap bitmap;
    private final int CAMERA_REQUEST_CODE = 100;
    private ActivityDiemdanhtestBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDiemdanhtestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        uinitUI();
        // load model
        try {
            tflite = new Interpreter(Untils.loadModelFile(this,PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
        faceDetector = FaceDetection.getClient(highAccuracyOpts);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
        }else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        cameraBing();

    }

    private void uinitUI() {
        previewView = binding.preview;
        facePRV = binding.facePRV;
    }

    private void cameraBing() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(()->{
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                cameraProvider.unbindAll();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                @SuppressLint("RestrictedApi") ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setCameraSelector(cameraSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                }, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();

                        if (mediaImage!=null){
                            bitmap = FaceModel.rotateBitmap(toBitmap(mediaImage),-90f);
                            //inputImage = InputImage.fromMediaImage(mediaImage,image.getImageInfo().getRotationDegrees());
                            faceDetector.process(InputImage.fromBitmap(bitmap,0))
                                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                                        @Override
                                        public void onSuccess(List<Face> faces) {
                                            if (faces.size()!=0){
                                                Bitmap xas = Bitmap.createBitmap(bitmap);
                                                Constants.USER_DB.child(Constants.AUTH.getCurrentUser().getUid()).child("face_data")
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                                                if (task.isComplete()){
                                                                    ArrayList<Float> da = new ArrayList<>();
                                                                    for (DataSnapshot dt : task.getResult().getChildren()){
                                                                        da.add(dt.getValue(Float.class));
                                                                    }
                                                                    Bitmap crop = FaceModel.cropRectFromBitmap(bitmap,faces.get(0).getBoundingBox(),false);
                                                                    binding.facePRV.setImageBitmap(crop);
                                                                    FaceModel faceNetModel = new FaceModel(Attendance.this);
                                                                    float[] em = faceNetModel.getFaceEmbedding(xas,faces.get(0).getBoundingBox(),false);
                                                                    float x;
                                                                    if ((x = FaceModel.match(em,Untils.fromArray(da)))<1.0f){
                                                                        binding.result.setText("dung"+String.format("%.10f",x)+" /"+em[10]+"/"+Untils.fromArray(da)[10]);
                                                                        setResult(RESULT_OK);
                                                                        //---close the activity---
                                                                        finish();
                                                                    }else {
                                                                        binding.result.setText("sai"+String.format("%.10f",x));
                                                                    }
                                                                }
                                                            }
                                                        });
                                                //recognizeImage(cropFace);
                                            }
                                        }
                                    })
                                    .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                                        @Override
                                        public void onComplete(@NonNull Task<List<Face>> task) {
                                            image.close();
                                        }
                                    });
                        }
                    }
                });
                cameraProvider.bindToLifecycle((LifecycleOwner) this,cameraSelector,preview,imageAnalysis);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}
