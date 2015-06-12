package ru.blogspot.c0dedgarik.arc;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class VideoStream {
    protected Camera mCamera;
    protected boolean mSurfaceReady = false;
    protected boolean mUnlocked = false;
    protected int mCameraId = 0;
    protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
    protected SurfaceHolder mSurfaceHolder = null;
    protected boolean mCameraOpenedManually = true;
    //protected boolean mStreaming = false;
    protected boolean mPreviewStarted = false;
    //private OutputStream mSocketOutput;

    private HttpStream mHttpStream = null;

    //final private ByteArrayOutputStream buffer = new ByteArrayOutputStream();


    public VideoStream() {
        this(CameraInfo.CAMERA_FACING_BACK);
    }

    public VideoStream(final int camera) {
        setCamera(camera);
    }

    /**
     * Sets the camera that will be used to capture video.
     * You can call this method at any time and changes will take effect next time you start the stream.
     *
     * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
     */
    public void setCamera(int camera) {
        CameraInfo cameraInfo = new CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == camera) {
                this.mCameraId = i;
                break;
            }
        }
    }

    /**
     * Sets a Surface to show a preview of recorded media (video).
     * You can call this method at any time and changes will take effect next time you call }.
     */
    public synchronized void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        if (mSurfaceHolderCallback != null && mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
        }
        if (surfaceHolder != null) {
            mSurfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mSurfaceReady = false;
                    stopPreview();
                    ARCLog.d("Surface destroyed !");
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mSurfaceReady = true;
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    ARCLog.d("Surface Changed !");
                }
            };
            mSurfaceHolder = surfaceHolder;
            mSurfaceHolder.addCallback(mSurfaceHolderCallback);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mSurfaceReady = true;
        }
    }

    /**
     * Stops the preview.
     */
    public synchronized void stopPreview() {

        stop();
    }

    /**
     * Stops the stream.
     */
    public synchronized void stop() {
        if (mCamera != null) {

            mCamera.setPreviewCallback(null);


            // We need to restart the preview
            if (!mCameraOpenedManually) {
                destroyCamera();
            } else {
                try {
                    startPreview();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Switch between the front facing and the back facing camera of the phone.
     * If {@link #startPreview()} has been called, the preview will be  briefly interrupted.
     * If {@link } has been called, the stream will be  briefly interrupted.
     * You should not call this method from the main thread if you are already streaming.
     *
     * @throws IOException
     * @throws RuntimeException public void switchCamera() throws RuntimeException, IOException {
     *                          if (Camera.getNumberOfCameras() == 1) throw new IllegalStateException("Phone only has one camera !");
     *                          <p/>
     *                          boolean previewing = mCamera!=null && mCameraOpenedManually;
     *                          mCameraId = (mCameraId == CameraInfo.CAMERA_FACING_BACK) ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK;
     *                          setCamera(mCameraId);
     *                          stopPreview();
     *                          if (previewing) startPreview();
     *                          //if (streaming) start();
     *                          }*
     */

    public int getCamera() {
        return mCameraId;
    }

    public synchronized void start() throws IllegalStateException, IOException {
        if (!mPreviewStarted) mCameraOpenedManually = false;
        //super.start();
        httpStream();
    }

    /*
    protected synchronized void sendJpeg(ByteArrayOutputStream buffer) {

        if(null == mSocketOutput) return;



        try {

            if (!mStreaming) {
                mSocketOutput.write(("HTTP/1.1 200 OK\r\n" +
                        "Connection: keep-alive\r\n" +
                        "Server: ARC-MJPG-Streamer/0.1\r\n" +
                        "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Expires: -1\r\n" +
                        "Content-Type: multipart/x-mixed-replace;boundary=" + BOUNDARY + "\r\n\r\n").getBytes());
                mSocketOutput.flush();

                mStreaming = true;
            }


            mSocketOutput.write(("\r\n--" + BOUNDARY + "\r\n" +
                    "Content-type: image/jpeg\r\n" +
                    "Content-Length: " + buffer.size() + "\r\n" +
                    "X-Timestamp: 0.000000\r\n\r\n").getBytes());

            buffer.writeTo(mSocketOutput);
            mSocketOutput.flush();

        } catch (IOException e) {
            ARCLog.d(e.getMessage());
            mSocketOutput = null;
            mStreaming = false;
        }

    }*/

    public synchronized void setSocketOutput(OutputStream out) {
        mHttpStream.setSocketOutput(out);
    }

    private class HttpStream implements Runnable, Camera.PreviewCallback {

        private OutputStream mSocketOutput = null;
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean mStreaming = false;
        private byte[] frame;
        private int imageFormat = 0;
        private final Rect area = new Rect(0, 0, 640, 480);
        private Handler mHandler = new Handler();
        final String BOUNDARY = "Ba4oTvQMY8ew04N8dcnM";

        public void setSocketOutput(OutputStream outputStream) {
            mSocketOutput = outputStream;
        }


        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mSocketOutput != null) {
                if (imageFormat == 0) {
                    Camera.Parameters parameters = camera.getParameters();
                    imageFormat = parameters.getPreviewFormat();
                }
                frame = data;
                mHandler.post(this);
            }
        }

        @Override
        public void run() {
            try {

                if (!mStreaming) {
                    mSocketOutput.write(("HTTP/1.1 200 OK\r\n" +
                            "Connection: keep-alive\r\n" +
                            "Server: ARC-MJPG-Streamer/0.1\r\n" +
                            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Expires: -1\r\n" +
                            "Content-Type: multipart/x-mixed-replace;boundary=" + BOUNDARY + "\r\n\r\n").getBytes());
                    mSocketOutput.flush();

                    mStreaming = true;
                }

                buffer.reset();
                new YuvImage(frame, imageFormat, 640, 480, null).compressToJpeg(area, 50, buffer);
                buffer.flush();

                mSocketOutput.write(("\r\n--" + BOUNDARY + "\r\n" +
                        "Content-type: image/jpeg\r\n" +
                        "Content-Length: " + buffer.size() + "\r\n" +
                        "X-Timestamp: 0.000000\r\n\r\n").getBytes());

                buffer.writeTo(mSocketOutput);
                mSocketOutput.flush();

            } catch (IOException e) {
                ARCLog.e(e.getMessage());

                mSocketOutput = null;
                mStreaming = false;
            }
        }
    }

    protected void httpStream() throws RuntimeException, IOException {
        // Opens the camera if needed

        ARCLog.d("httpStream start");


        //mStreaming = false;
        //mPreviewStarted = false;

        createCamera();

        // Starts the preview if needed
        if (!mPreviewStarted) {
            try {
                mCamera.startPreview();
                mPreviewStarted = true;
            } catch (RuntimeException e) {
                destroyCamera();
                throw e;
            }
        }

        mHttpStream = new HttpStream();
        mCamera.setPreviewCallback(mHttpStream);
    }

    protected synchronized void destroyCamera() {
        if (mCamera != null) {
            //if (mStreaming) super.stop();
            lockCamera();
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {
                ARCLog.e(e.getMessage() != null ? e.getMessage() : "unknown error");
            }
            mCamera = null;
            mUnlocked = false;
            mPreviewStarted = false;
        }
    }

    protected void lockCamera() {
        if (mUnlocked) {
            ARCLog.d("Locking camera");
            try {
                mCamera.reconnect();
            } catch (Exception e) {
                ARCLog.e(e.getMessage());
            }
            mUnlocked = false;
        }
    }

    protected void unlockCamera() {
        if (!mUnlocked) {
            ARCLog.d("Unlocking camera");
            try {
                mCamera.unlock();
            } catch (Exception e) {
                ARCLog.e(e.getMessage());
            }
            mUnlocked = true;
        }
    }

    public synchronized void startPreview() throws RuntimeException, IOException {
        if (!mPreviewStarted) {
            createCamera();
            try {
                mCamera.startPreview();
                mPreviewStarted = true;
                mCameraOpenedManually = true;
            } catch (RuntimeException e) {
                destroyCamera();
                throw e;
            }
        }
    }

    protected synchronized void createCamera() throws RuntimeException, IOException {
        if (mSurfaceHolder == null || mSurfaceHolder.getSurface() == null || !mSurfaceReady)
            throw new IllegalStateException("Invalid surface holder !");

        if (mCamera == null) {
            mCamera = Camera.open(mCameraId);
            mUnlocked = false;
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    // On some phones when trying to use the camera facing front the media server will die
                    // Whether or not this callback may be called really depends on the phone
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        // In this case the application must release the camera and instantiate a new one
                        ARCLog.e("Media server died !");
                        // We don't know in what thread we are so stop needs to be synchronized
                        mCameraOpenedManually = false;
                        stop();
                    } else {
                        ARCLog.e("Error unknown with the camera: " + error);
                    }
                }
            });

            Camera.Parameters parameters = mCamera.getParameters();

            parameters.setPreviewSize(640, 480);
            //parameters.setPreviewFormat(ImageFormat.YUY2);

            /*if (mMode == MODE_MEDIACODEC_API) {
                getClosestSupportedQuality(parameters);
                parameters.setPreviewFormat(ImageFormat.YV12);
                parameters.setPreviewSize(mQuality.resX, mQuality.resY);
                parameters.setPreviewFrameRate(mQuality.framerate);
            }

            if (mFlashState) {
                if (parameters.getFlashMode()==null) {
                    // The phone has no flash or the choosen camera can not toggle the flash
                    throw new IllegalStateException("Can't turn the flash on !");
                } else {
                    parameters.setFlashMode(mFlashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
                }
            }*/

            try {
                mCamera.setParameters(parameters);
                //mCamera.setDisplayOrientation(mQuality.orientation);
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (RuntimeException e) {
                destroyCamera();
                throw e;
            } catch (IOException e) {
                destroyCamera();
                throw e;
            }
        }
    }
}
