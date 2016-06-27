package hathibelagal.github.io.mycamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    private static final int MY_REQUEST_CODE = 1;
    private static final String TAG = "MY CAMERA";

    private TextureView preview;
    private FloatingActionButton fab;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private String backCamera;
    private CameraCaptureSession cameraCaptureSession;

    private Handler handler;

    private int previewWidth = 640;
    private int previewHeight = 480;

    private CaptureRequest.Builder requestBuilder;

    private ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
    }

    private void requestPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            initializeUI();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_REQUEST_CODE);
        }
    }

    private void initializeUI() {
        preview = (TextureView)findViewById(R.id.preview);
        fab = (FloatingActionButton)findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPhotoCaptureSequence();
            }
        });

        initializeCamera();
        initializePreview();
        initializeImageReader();
    }

    private void initializeImageReader() {
        int width = 0;
        int height = 0;
        try {
            StreamConfigurationMap map
                    = cameraManager.getCameraCharacteristics(backCamera)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            for(Size size:sizes) {
                Log.d(TAG, "Resolution: " + size.getWidth() + "x" + size.getHeight());
            }
            Arrays.sort(sizes, new Comparator<Size>() {
                @Override
                public int compare(Size size1, Size size2) {
                    return size2.getWidth() * size2.getHeight() -
                            size1.getWidth() * size1.getHeight();
                }
            });
            width = sizes[0].getWidth();
            height = sizes[0].getHeight();
            Log.d(TAG, "Max resolution: " + width + "x" + height);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                String filename = Util.getANewFilename();
                File storageDirectory = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                );
                final File photoFile = new File(storageDirectory, filename);
                final byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            FileOutputStream fileOutputStream
                                    = new FileOutputStream(photoFile);
                            fileOutputStream.write(data);
                            fileOutputStream.close();

                            Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                            intent.putExtra("FILENAME", photoFile.getAbsolutePath());
                            startActivity(intent);

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }, handler);
    }

    private void startPhotoCaptureSequence() {
        requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        CaptureRequest lockFocusRequest = requestBuilder.build();

        try {
            cameraCaptureSession.capture(lockFocusRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    Integer autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if(autoFocusState == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED
                            || autoFocusState == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if(autoExposureState == null) {
                            takePicture();
                        } else {
                            if(autoExposureState == CameraMetadata.CONTROL_AE_STATE_CONVERGED) {
                                takePicture();
                            } else {
                                startPrecaptureSequence();
                            }
                        }
                    }
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPrecaptureSequence() {
        requestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        CaptureRequest precaptureRequest = requestBuilder.build();

        try {
            cameraCaptureSession.capture(precaptureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    if(result.get(CaptureResult.CONTROL_AE_STATE)
                            == CameraMetadata.CONTROL_AE_STATE_PRECAPTURE) {
                        return;
                    } else {
                        takePicture();
                    }
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        try {
            CaptureRequest.Builder photoRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            photoRequestBuilder.addTarget(imageReader.getSurface());
            photoRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.capture(photoRequestBuilder.build(), null, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void initializePreview() {
        try {
            int orientationOfSensor = cameraManager.getCameraCharacteristics(backCamera)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.d(TAG, "Orientation of sensor is " + orientationOfSensor);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        preview.getLayoutParams().width = previewHeight;
        preview.getLayoutParams().height = previewWidth;
        preview.requestLayout();

        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                startPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    private void startPreview() {
        handler = new Handler();

        CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(final CameraDevice cameraDevice) {
                MainActivity.this.cameraDevice = cameraDevice;

                try {
                    cameraDevice.createCaptureSession(Arrays.asList(new Surface(preview.getSurfaceTexture()), imageReader.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                                    MainActivity.this.cameraCaptureSession = cameraCaptureSession;

                                    try {
                                        requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                        requestBuilder.addTarget(new Surface(preview.getSurfaceTexture()));
                                        cameraCaptureSession.setRepeatingRequest(
                                                requestBuilder.build(),
                                                null,
                                                handler
                                        );
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                                }
                            }, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {

            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {

            }
        };

        try {
            cameraManager.openCamera(backCamera, callback, handler);
        }catch(SecurityException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initializeCamera() {
        cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            String[] cameras = cameraManager.getCameraIdList();
            for(String camera:cameras) {
                CameraCharacteristics characteristics
                        = cameraManager.getCameraCharacteristics(camera);
                if(characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_BACK) {
                    backCamera = camera;
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_REQUEST_CODE) {
            if(grantResults.length==2) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    initializeUI();
                } else {
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraCaptureSession.close();
        cameraDevice.close();
    }
}
