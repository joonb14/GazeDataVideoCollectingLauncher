/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2video;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.lang.IllegalStateException;
import java.lang.RuntimeException;

public class Camera2VideoFragment extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private boolean debugFlag = true;
    private static final String TAG = "MOBED";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    View rootview;
    private boolean endFlag = false;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mButtonVideo;

    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG,"onOpened");
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }

            //MOBED
            //Needs a little delay... both for app and user!
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"run()");
                    startRecordingVideo();
                    endFlag = false;
                }
            }, 150);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;

    public static Camera2VideoFragment newInstance() {
        return new Camera2VideoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_camera2_video, container, false);
        loadApps();
        loadListView();
        addGridListeners();
        return rootview;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "onResume mTextureView.isAvailable()");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            Log.d(TAG, "onResume mTextureView.setSurfaceTextureListener(mSurfaceTextureListener)");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        try {
            if (!endFlag) {
                //Erase temporarily stored file
                File tempFile = new File(mNextVideoAbsolutePath);
                if (tempFile.exists()) {
                    //erase the file
                    tempFile.delete();
                    Log.e(TAG, "Paused before stop recording. Erase temporarily stored file");
                }
            }
        }
        catch (RuntimeException e) {
            Log.e(TAG, "Null Pointer Exception on mNextVideoAbsolutePath");
            e.printStackTrace();
        }
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onClick(View view) {

    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //MOBED, Camera id 1 for front facing camera
            String cameraId = "1";

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
//            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    width, height, mVideoSize);
            mVideoSize = new Size(640,480);
            mPreviewSize = new Size(1,1);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            Log.d(TAG, "OpenCamera - mMediaRecorder created");
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }

    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        Log.d(TAG,"setUpCaptureRequestBuilder");
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException {
        Log.d(TAG,"setUpMediaRecorder");
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mNextVideoAbsolutePath = getVideoFilePath(getActivity());
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        Log.d(TAG,"getVideoFilePath count: "+CameraActivity.getCount());
        return (dir == null ? "" : ( "/sdcard/CaptureApp/"+ "Video_")) + CameraActivity.getCount() + "_" + System.currentTimeMillis()+ ".mp4";
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording

            Log.d(TAG, "Start Video Recording");

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Start recording
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void stopRecordingVideo() throws RuntimeException{
        // Stop recording
        Log.d(TAG, "stopRecordingVideo start");
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            String gyroData = CameraActivity.getGyroData();
            String acceleroData = CameraActivity.getAcceleroData();
            appendLog(CameraActivity.getCount()+", " + System.currentTimeMillis() +", " + positionX + ", " + positionY + ", " + gyroData + ", " +  acceleroData);
            //append LogFile

            CameraActivity.addCount();

            Log.d(TAG, "stopRecordingVideo count: "+CameraActivity.getCount());
            startPreview();

            Log.d(TAG, "Stopped Video Recording");
            endFlag = true;
        }
        catch (RuntimeException e){
            Log.e(TAG, "RuntimeException: Trying stopRecordingVideo() before Camera is Ready");
            //In case of RuntimeException. Erase temporarily stored file
            File tempFile = new File(mNextVideoAbsolutePath);
            if (tempFile.exists()){
                //erase the file
                tempFile.delete();
                Log.d(TAG, "In case of RuntimeException. Erase temporarily stored file");
            }
            e.printStackTrace();
        }
    }

    public void appendLog(String text)
    {
        File logFile = new File("sdcard/LogFile/log.csv");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append("VideoNum, time, gazeX, gazeY, gyroscopeX, gyroscopeY, gyroscopeZ, AccelerometerX, AccelerometerY, AccelerometerZ");
                buf.newLine();
                buf.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

    /**
     * MOBED
     *
     * */

    PackageManager packageManager;
    public static List<AppInfo> apps;
    GridView grdView;
    public static ArrayAdapter<AppInfo> adapter;

    private void loadApps() {
        try {
            if (packageManager == null)
                packageManager = getActivity(). getPackageManager();
            if (apps == null) {
                apps = new ArrayList<AppInfo>();

                Intent i = new Intent(Intent.ACTION_MAIN, null);
                i.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> availableApps = packageManager.queryIntentActivities(i, 0);
                for (ResolveInfo ri : availableApps) {
                    AppInfo appinfo = new AppInfo();
                    appinfo.label = ri.loadLabel(packageManager);
                    appinfo.name = ri.activityInfo.packageName;
                    appinfo.icon = ri.activityInfo.loadIcon(packageManager);
                    SharedPreferences sf = CameraActivity.getSF();
                    appinfo.count = sf.getInt(appinfo.label.toString(),0);
                    apps.add(appinfo);
                    //Log.d(TAG," | loadApps: "+appinfo.label+ " | count: "+appinfo.count);
                }

                //Sort apps by pressed count
                apps.sort(new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo arg0, AppInfo arg1) {
                        // TODO Auto-generated method stub
                        int count0 = arg0.count;
                        int count1 = arg1.count;
                        return Integer.compare(count1, count0);
                    }
                });

                //Used for Debugging
                if(debugFlag){
                    for (int loopcount=0;loopcount<apps.size();loopcount++){
                        Log.d(TAG,apps.get(loopcount).name.toString()+": "+apps.get(loopcount).count);
                    }
                }

                //Now to force user to press 4 corners, locate 4 most pressed apps in 4 corners.
                //Additionally place 6th ~ 9th most used app at the top, left, right, bottom
                //And Center for 5th
                if(apps.size()>9) {
                    AppInfo app0 = apps.get(0);
                    AppInfo app1 = apps.get(1);
                    AppInfo app2 = apps.get(2);
                    AppInfo app3 = apps.get(3);
                    AppInfo app4 = apps.get(4);
                    AppInfo app5 = apps.get(5);
                    AppInfo app6 = apps.get(6);
                    AppInfo app7 = apps.get(7);
                    AppInfo app8 = apps.get(8);

                    //TODO Relate colsize, rowsize with Display Metric(dp)
                    //TODO This doesn't guarantee the right position for each selected apps. So Must be revised
                    int colsize = 5;
                    int rowsize = 7;
                    SwapAppInfo(apps.get(0),app0);
                    SwapAppInfo(apps.get(colsize-1),app1);
                    SwapAppInfo(apps.get((rowsize-1)*colsize),app2);
                    SwapAppInfo(apps.get(rowsize*colsize-1),app3);
                    SwapAppInfo(apps.get(colsize*(rowsize/2)+colsize/2),app4);
                    SwapAppInfo(apps.get(colsize/2),app5);
                    SwapAppInfo(apps.get(colsize*(rowsize/2)),app6);
                    SwapAppInfo(apps.get(colsize*(1+rowsize/2)-1),app8);
                }

                //Used for Debugging
                if(debugFlag){
                    for (int loopcount=0;loopcount<apps.size();loopcount++){
                        Log.d(TAG,apps.get(loopcount).name.toString()+": "+apps.get(loopcount).count);
                    }
                }
            }

        } catch (Exception ex) {
            Log.e("Error loadApps", ex.getMessage().toString() + " loadApps");
        }
    }
    private void SwapAppInfo(AppInfo app1, AppInfo app2){
        AppInfo temp = new AppInfo();
        temp.count = app1.count;
        temp.name = app1.name;
        temp.icon = app1.icon;
        temp.label = app1.label;

        app1.count = app2.count;
        app1.name = app2.name;
        app1.icon = app2.icon;
        app1.label = app2.label;

        app2.count = temp.count;
        app2.label = temp.label;
        app2.name = temp.name;
        app2.icon = temp.icon;
    }

    private void loadListView() {

        try {
            grdView = (GridView) rootview.findViewById(R.id.grd_allApps);
            if (adapter == null) {
                adapter = new ArrayAdapter<AppInfo>(rootview.getContext(), R.layout.grd_items, apps) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {

                        ViewHolderItem viewHolder = null;

                        if (convertView == null) {
                            convertView = getLayoutInflater().inflate(
                                    R.layout.grd_items, parent, false
                            );
                            viewHolder = new ViewHolderItem();
                            viewHolder.icon = (ImageView) convertView.findViewById(R.id.img_icon);
                            viewHolder.name = (TextView) convertView.findViewById(R.id.txt_name);
                            viewHolder.label = (TextView) convertView.findViewById(R.id.txt_label);

                            convertView.setTag(viewHolder);
                        } else {
                            viewHolder = (ViewHolderItem) convertView.getTag();
                        }

                        AppInfo appInfo = apps.get(position);

                        if (appInfo != null) {
                            viewHolder.icon.setImageDrawable(appInfo.icon);
                            viewHolder.label.setText(appInfo.label);
                            viewHolder.name.setText(appInfo.name);
                            //Log.d("MOBED"," | loadListView: "+appInfo.label+ " | Package name: "+appInfo.name);
                        }
                        return convertView;

                    }

                    final class ViewHolderItem {
                        ImageView icon;
                        TextView label;
                        TextView name;
                    }
                };
            }

            grdView.setAdapter(adapter);
        } catch (Exception ex) {
            Log.e("Error loadListView", ex.getMessage().toString() + " loadListView");
        }
    }
    private int positionX;
    private int positionY;
    private int position;
    public void addGridListeners() {
        try {
            grdView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d("MOBED", "Item position: " +i);
                    Log.d("MOBED", "Screen X: " +positionX);
                    Log.d("MOBED", "Screen Y: " +positionY);
                    position = i;
                    SharedPreferences sf = CameraActivity.getSF();
                    String applabel = apps.get(position).label.toString();
                    int count = sf.getInt(applabel,0);
                    SharedPreferences.Editor editor = sf.edit();
                    count+=1;
                    editor.putInt(applabel,count);
                    editor.commit();
                    Log.d(TAG,applabel+" count: "+count);
                    stopRecordingVideo();

                    Handler delayHandler = new Handler();
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = packageManager.getLaunchIntentForPackage(apps.get(position).name.toString());
                            rootview.getContext().startActivity(intent);
                        }
                    }, 1000);
                }
            });
            grdView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    positionX = (int)event.getX();
                    positionY = (int)event.getY();
                    return false; // not consumed; forward to onClick
                }
            });
        } catch (Exception ex) {
            Log.e("Error Grid", ex.getMessage().toString() + " Grid");
        }

    }
}
