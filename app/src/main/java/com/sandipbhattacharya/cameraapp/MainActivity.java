package com.sandipbhattacharya.cameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.sandipbhattacharya.cameraapp.helper.MultiFrameJpg2Dcm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCameraApi";
    private Button btnTake;
    private Button btnGallery;
    private int FPS = 0;

    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private static final long FRAME_INTERVAL_MS = 80;
    private Handler captureHandler = new Handler();
    private int capturedFrames = 0;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private File folder;
    private int frameCount = 0;
    private Boolean isClicked = false;
    private Boolean isThirdFrame = false;
    private String folderName = "MyPhotoDir";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private List<Bitmap> frameList = new ArrayList<>(6);

    private long lastCaptureTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("LISTSIZE", "onCreate: "+frameList.size());
        textureView = findViewById(R.id.texture);
        if (textureView != null)
            textureView.setSurfaceTextureListener(textureListener);
        btnTake = findViewById(R.id.btnTake);
        btnGallery = findViewById(R.id.btnGallery);
        if (btnTake != null)
            btnTake.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isClicked = true;
                    Log.d("CCC", "onClick: CLICKED");

//                    takePicture();
                }
            });
        if (btnGallery != null)
            btnGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, CustomGalleryActivity.class);
                    startActivity(intent);
                }
            });
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (isClicked && capturedFrames < 6) {
                long currentTime = System.currentTimeMillis();

                // Ensure a minimum interval between Runnable executions
                if (currentTime - lastCaptureTime >= FRAME_INTERVAL_MS) {
                    capturedFrames++;
                    lastCaptureTime = currentTime;

                    Log.d("FRAMELISTLAST", "LAST: "+lastCaptureTime);
                    // Capture frame
                    Bitmap frame =  captureFrame();
                    if (frame != null) {
                        frameList.add(frame);
                        Log.d("FRAMELIST", "LIST : " + frameList.size());

                        if (capturedFrames == 6) {
                            frameList.remove(0);
                            // Save frames asynchronously in a background thread
                            SaveFramesUtils.saveFramesAsync(frameList, new SaveFramesUtils.SaveFramesCallback() {
                                @Override
                                public void onSaveComplete() {
                                    frameList.clear();
                                    Log.d("FRAMELIST", "reached: " + frameList.size());
                                    createDcmFile();
                                    isClicked = false;
                                    capturedFrames = 0;
                                }
                            });
                            Log.d("FRAMELIST", "reached: "+frameList.size());
                        }
                    }
                }
            }
        }
    };

    private void createDcmFile() {
        try {
            // Get external storage directory
            File externalStorageDirectory = Environment.getExternalStorageDirectory();

            // Specify the folder containing JPEG images
            File jpegFolder = new File(externalStorageDirectory, "Download/DICOM");

            // Specify the output DICOM file
            File dicomOutputFile = new File(externalStorageDirectory, "Download/multiframe.dcm");

            File[] jpegFiles = jpegFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpeg"));
            if (jpegFiles != null && jpegFiles.length > 0) {
                for (File file : jpegFiles) {
                    Log.d("@@@", "Found JPEG file: " + file.getName());
                }
                MultiFrameJpg2Dcm multiFrameJpg2Dcm = new MultiFrameJpg2Dcm(jpegFiles, dicomOutputFile);
                Log.d("@@@", "Multi-Frame DICOM file created successfully: " + dicomOutputFile.getAbsolutePath());
            } else {
                Log.d("@@@", "No JPEG files found in the specified folder.");
            }

        } catch (Exception e) {
            Log.d("@@@", "Error creating Multi-Frame DICOM: " + e);
            e.printStackTrace();
        }
    }

    private Bitmap captureFrame() {
        Bitmap frame = null;
        try {
            frame = Bitmap.createBitmap(textureView.getWidth(), textureView.getHeight(), Bitmap.Config.ARGB_8888);
            textureView.getBitmap(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frame;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private boolean isExternalStorageAvailableForRW() {
        // Check if the external storage is available for read and write by calling
        // Environment.getExternalStorageState() method. If the returned state is MEDIA_MOUNTED,
        // then you can read and write files. So, return true in that case, otherwise, false.
        String extStorageState = Environment.getExternalStorageState();
        if (extStorageState.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                return true;
            } else {
                //Permission is revoked
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            // Permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}