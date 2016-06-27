package hathibelagal.github.io.mycamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Arrays;

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

        initializeCamera();
        initializePreview();
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
                    cameraDevice.createCaptureSession(Arrays.asList(new Surface(preview.getSurfaceTexture())),
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
