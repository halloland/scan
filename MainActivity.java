package halloland.scan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {


    public static final String LOG_TAG = "myLogs";
    private RectangleView rectangleView;

    CameraService[] myCameras = null;
    double ratio = 1.00;
    boolean finished = true;
    boolean disabled = false;

    private CameraManager mCameraManager = null;
    private final int CAMERA1 = 0;
    private final int CAMERA2 = 1;
    int linesThreshold = 20;
    int counter = 0;
    List<Point> lastPoints;
    RectangleSearcher rectangleSearcher;
    private float ratioHeight = 0;
    private float ratioWidth = 0;
    private SurfaceView opencvView;
    private SurfaceHolder holderOpencv;
    private TextureView mTextureView = null;
    private ImageView imageView;
    public TextView textView;
    public Bitmap currentPhoto;
    public int currentPhotoIndex = 1;
    public int index = 0;
    public int maxIndex = 0;

    public Button buttonNext;
    public Button buttonPrev;


    public int getResourceId(String name, String defType) {
        return getResources().getIdentifier(name, defType, this.getPackageName());
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        ) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }


        if (!OpenCVLoader.initDebug()) {
            Log.d("CVerror", "OpenCV library Init failure");
        } else {

            Log.d("CVerror", "success");
            // load your library and do initializing stuffs like System.loadLibrary();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getResourceId("camera", "layout"));

        mTextureView = findViewById(getResourceId("textureView", "id"));


        imageView = findViewById(getResourceId("imageView", "id"));


        Bitmap photo = BitmapFactory.decodeResource(getResources(),
                getResourceId("l" + currentPhotoIndex, "drawable"));

        imageView.setImageBitmap(photo);



        textView = (TextView) findViewById(getResourceId("textView2", "id"));

        buttonNext = (Button) findViewById(getResourceId("buttonNext", "id"));
        buttonPrev = (Button) findViewById(getResourceId("buttonPrev", "id"));

        buttonNext.setOnClickListener(this);
        buttonPrev.setOnClickListener(this);

        FrameLayout frameLayout = findViewById(getResourceId("FrameLayout01", "id"));
        rectangleView = findViewById(getResourceId("rectangle", "id"));
        SeekBar seekBar = findViewById(getResourceId("seekBar", "id"));
        seekBar.setOnSeekBarChangeListener(this);

        opencvView = (SurfaceView) findViewById(this.getResourceId("SurfaceView03", "id"));

        holderOpencv = opencvView.getHolder();
        //holderOpencv.addCallback((SurfaceHolder.Callback) new MainScreen.LinesDrawer());
        holderOpencv.setFormat(PixelFormat.TRANSLUCENT);



        this.rectangleSearcher = new RectangleSearcher();






        Thread thread = new Thread() {
            @Override
            public void run() {
                currentPhoto = Bitmap.createScaledBitmap(photo, 800,
                        450, true);

                ratio = 1920 / 800;

                while (true){

                    if (disabled) {
                        continue;
                    }


                    if (finished) {
                        long time = System.currentTimeMillis();
                        Bitmap bitmap = currentPhoto;
                        finished = false;
                                Mat frame = new Mat();
                                Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                Utils.bitmapToMat(bmp32, frame);



                                Mat mask = MatHelper.applyFilters(frame, linesThreshold);

                                ratioHeight = (float) 2280 / mask.height();
                                ratioWidth = (float) 1080 / mask.width();

                                if (!mask.empty()) {
                                    List<MatOfPoint> contours = new ArrayList<>();
                                    Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
                                    MatOfPoint2f matOfPoint2f = new MatOfPoint2f();


                                    double maxArea = 0;
                                    int countourIndex = 0;
                                    int prevContourIndex = 0;
                                    MatOfPoint2f lastApprox = new MatOfPoint2f();
                                    for(int i =0; i < contours.size(); i ++){
                                        MatOfPoint contour = contours.get(i);
                                        org.opencv.core.Rect rect = Imgproc.boundingRect(contour);
                                        MatOfPoint2f approxCurve = new MatOfPoint2f();
                                        matOfPoint2f.fromList(contour.toList());
                                        double contourArea = rect.area();//Imgproc.contourArea(contour);
                                        Imgproc.approxPolyDP(matOfPoint2f, approxCurve, Imgproc.arcLength(matOfPoint2f, false) * 0.005, false);
                                        if(contourArea > maxArea){
                                            prevContourIndex = countourIndex;
                                            lastApprox = approxCurve;
                                            maxArea = contourArea;
                                            countourIndex = i;
                                        }
                                    }

                                    if(maxArea > 0){
                                        List<Point> points = lastApprox.toList();

                                        List<Point> filteredPoints = PointsHelper.sort(PointsHelper.removePointsDuplications(points, mask.width(), mask.height()));
                                        maxIndex = filteredPoints.size() - 1;
                                        List<Point> rectPoints = new ArrayList<>();
                                        if(filteredPoints.size() != 4){
                                            for (int i =0; i < filteredPoints.size(); i++){
                                               // int i = index;
                                                Point p1 = filteredPoints.get(i);
                                                Point p2 = filteredPoints.get(0);
                                                Point p4 = filteredPoints.get(1);
                                                if(i + 1 != filteredPoints.size()){
                                                    p2 = filteredPoints.get(i + 1);
                                                }
                                                if(i + 2 > filteredPoints.size() - 1){
                                                    p4 = filteredPoints.get(0);
                                                } else {
                                                    p4 = filteredPoints.get(i + 2);
                                                }
                                                double length = GeometryTools.getLineLength(p1, p2);
                                                Point p3;
                                                if(i == 0){
                                                     p3 = filteredPoints.get(filteredPoints.size() - 1);
                                                } else {
                                                     p3 = filteredPoints.get(i - 1);
                                                }

                                                double cos1 = Math.abs(GeometryTools.getCos(new Point(p2.x - p1.x, p2.y - p1.y), new Point(p1.x - p3.x, p1.y - p3.y)));
                                                double cos2 = Math.abs(GeometryTools.getCos(new Point(p2.x - p1.x, p2.y - p1.y), new Point(p2.x - p4.x, p2.y - p4.y)));

                                                if(cos1 > 0.2 && cos2 > 0.2){
                                                    Point intersect = GeometryTools.getLinesIntersectionPoint(p1, p3, p2, p4);
                                                    rectPoints.add(intersect);
                                                } else {
                                                    if(!rectPoints.contains(p1)){
                                                        rectPoints.add(p1);
                                                    }
                                                    if(!rectPoints.contains(p2)){
                                                        rectPoints.add(p2);
                                                    }
                                                }
                                            }
                                        } else {
                                            rectPoints = filteredPoints;
                                        }

                                        rectPoints = PointsHelper.removePointsDuplications(rectPoints, mask.width(), mask.height());




                                       Point[] rect = rectangleSearcher.getBiggestRectangleFromPoints(rectPoints);

                                        double angle = 0.15;
                                        while (rect == null && angle < 0.4){
                                            angle += 0.15;
                                            rect = rectangleSearcher.getBiggestRectangleFromPoints(filteredPoints, angle);
                                        }

                                        if(rect != null){
                                            DrawOnUi(Arrays.asList(rect));
                                        } else {
                                            DrawOnUi(null);
                                        }

                                        drawOpencv(frame);
                                    }

                                    double fps = 1000 / ((System.currentTimeMillis() - time) );

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView.setText(String.valueOf(fps));
                                        }
                                    });

                                }

                                finished = true;


                    }
                }

            }
        };

        thread.start();
    }
    private void drawOpencv(Mat image) {
        Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);
        try {
            Canvas canvas = holderOpencv.lockCanvas(null);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


            if (canvas != null) {
                Paint p = new Paint();
                p.setColor(Color.RED);
                canvas.drawColor(Color.RED);
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, opencvView.getWidth(), opencvView.getHeight(), false), 0, 0, p);
            }

            holderOpencv.unlockCanvasAndPost(canvas);

        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

       /* myCameras[CAMERA1].focus(event);*/



        return super.onTouchEvent(event);

    }

    @Override
    public void onClick(View v) {

        if(v == buttonNext){
            if(currentPhotoIndex == 32){
                currentPhotoIndex = 1;
            } else {
                currentPhotoIndex++;
            }

        } else if(v == buttonPrev) {
            if(currentPhotoIndex == 1){
                currentPhotoIndex = 32;
            } else {
                currentPhotoIndex--;
            }
        } else {
            index++;
            if(index > maxIndex){
                index = 0;
            }
        }

        Bitmap photo = BitmapFactory.decodeResource(getResources(),
                getResourceId("l" + currentPhotoIndex, "drawable"));
        imageView.setImageBitmap(photo);

        currentPhoto = Bitmap.createScaledBitmap(photo, 800,
                450, true);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(progress > 0){
            this.linesThreshold = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public class CameraService {


        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private CaptureRequest.Builder builder;

        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }

        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
            }
        };

        public void focus(MotionEvent motionEvent){

            try {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(String.valueOf(CAMERA1));

                final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                final int y = (int)((motionEvent.getX() / (float) mTextureView.getWidth())  * (float)sensorArraySize.height());
                final int x = (int)((motionEvent.getY() / (float) mTextureView.getHeight()) * (float)sensorArraySize.width());
                final int halfTouchWidth  = 10;
                final int halfTouchHeight = 10;
                MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth,  0),
                        Math.max(y - halfTouchHeight, 0),
                        halfTouchWidth  * 2,
                        halfTouchHeight * 2,
                        MeteringRectangle.METERING_WEIGHT_MAX - 1);


                CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                       /* mManualFocusEngaged = false;*/



                        if (request.getTag() == "FOCUS_TAG") {
                            //the focus trigger is complete -
                            //resume repeating (preview surface will get frames), clear AF trigger
                            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                            try {
                                mCaptureSession.setRepeatingRequest(builder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                        super.onCaptureFailed(session, request, failure);

                        //mManualFocusEngaged = false;
                    }
                };

                mCaptureSession.stopRepeating();

                //cancel any existing AF trigger (repeated touches, etc.)
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                mCaptureSession.capture(builder.build(), captureCallbackHandler, null);

                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

                mCaptureSession.capture(builder.build(), captureCallbackHandler, null);

                //Now add a new AF trigger with focus region
                if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1) {
                    builder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
                }
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

                builder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

                //then we ask for a single request (not repeating!)
                mCaptureSession.capture(builder.build(), captureCallbackHandler, null);


            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        private void createCameraPreviewSession() {


            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            texture.setDefaultBufferSize(2280, 1080);
            Surface surface = new Surface(texture);

            try {
                 builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                builder.addTarget(surface);



                mCameraDevice.createCaptureSession(Arrays.asList(surface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    builder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mCaptureSession.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }


        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera() {
            try {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                mCameraManager.openCamera(mCameraID, mCameraCallback, null);


            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());

            }
        }

        public void closeCamera() {

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }



    }


    @Override
    public void onPause() {
      /*  if(myCameras[CAMERA1].isOpen()){myCameras[CAMERA1].closeCamera();}
        if(myCameras[CAMERA2].isOpen()){myCameras[CAMERA2].closeCamera();}*/
        super.onPause();
    }


    private void DrawOnUi(List<Point> list) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Draw(list);
            }
        });
    }

    private void Draw(List<Point> points) {

        lastPoints = points;

        if (points != null) {
            List<Point> scaledPoints = PointsHelper.cloneListPoints(points);

            for (int i = 0; i < scaledPoints.size(); i++) {
                Point point = scaledPoints.get(i);
                point.x *= ratioWidth;
                point.y *= ratioHeight;
            }

            this.rectangleView.render(scaledPoints);
        } else {
            this.rectangleView.render(null);
        }
    }
}