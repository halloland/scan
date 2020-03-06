package halloland.scan;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainScreen extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    MainScreen context;

    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder holderOpencv;
    private SurfaceView opencvView;

    private ProgressBar progressBar;

    private RectangleView rectangleView;

    private SurfaceView preview;
    private Button shotBtn;
    private Button autoBtn;
    private Button doneBtn;
    private Button cancelBtn;
    private TextView waitText;

    private RectangleSearcher rectangleSearcher;

    private double ratio = 1.00;

    private float ratioHeight = 0;
    private float ratioWidth = 0;

    private int linesThreshold = 255;

    boolean finished = true;

    private List<Point> lastPoints;
    private int counter = 0;
    private String cachePath;
    private boolean disabled = false;

    private boolean automatic = true;

    private List<Mat> pages = new ArrayList<>();

    public int getResourceId(String name, String defType) {
        return getResources().getIdentifier(name, defType, this.getPackageName());
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;

        Intent intent = getIntent();
        String cachePath = intent.getStringExtra("cachePath");

        if (cachePath != null) {
            this.cachePath = cachePath;
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

        setContentView(this.getResourceId("scan", "layout"));


        preview = (SurfaceView) findViewById(this.getResourceId("SurfaceView01", "id"));
        opencvView = (SurfaceView) findViewById(this.getResourceId("SurfaceView03", "id"));

        holderOpencv = opencvView.getHolder();
        holderOpencv.addCallback((SurfaceHolder.Callback) new LinesDrawer());
        holderOpencv.setFormat(PixelFormat.TRANSLUCENT);


        opencvView.setZOrderMediaOverlay(true);
        surfaceHolder = preview.getHolder();


        surfaceHolder.addCallback(this);

        progressBar = (ProgressBar) findViewById(this.getResourceId("progressBar", "id"));

        rectangleView = (RectangleView) findViewById(this.getResourceId("rectangle", "id"));


        shotBtn = (Button) findViewById(this.getResourceId("Button01", "id"));

        doneBtn = (Button) findViewById(this.getResourceId("buttonFinish", "id"));

        cancelBtn = (Button) findViewById(this.getResourceId("buttonCancel", "id"));

        autoBtn = (Button) findViewById(this.getResourceId("button", "id"));

        waitText = (TextView) findViewById(this.getResourceId("textView", "id"));

        doneBtn.setOnClickListener(this);
        autoBtn.setOnClickListener(this);
        shotBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);

        this.rectangleSearcher = new RectangleSearcher();


    }


    private class LinesDrawer implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
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


    private void DrawOnUi(List<Point> list) {
        context.runOnUiThread(new Runnable() {
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


    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open();

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Size previewSize = camera.getParameters().getPreviewSize();
        List<Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        Size biggestSize = null;
        int biggestArea = 0;


        float aspect = (float) previewSize.width / previewSize.height;

        for (int i = 0; i < pictureSizes.size(); i++) {

            Size size = pictureSizes.get(i);

            int area = size.width * size.height;
            if (area > biggestArea && (float) size.width / size.height == aspect) {
                biggestSize = size;
                biggestArea = area;
            }
        }


        if (biggestSize != null) {
            Log.e("biggest size", biggestSize.width + "x" + biggestSize.height);
            Camera.Parameters params = camera.getParameters();
            //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.setPictureSize(biggestSize.width, biggestSize.height);
            camera.setParameters(params);
        } else {
            // TODO if not found biggest size throw error
        }


        int previewSurfaceWidth = preview.getWidth();
        int previewSurfaceHeight = preview.getHeight();

        LayoutParams lp = preview.getLayoutParams();

        // здесь корректируем размер отображаемого preview, чтобы не было искажений

        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // портретный вид
            camera.setDisplayOrientation(90);

            lp.height = previewSurfaceHeight;
            lp.width = (int) (previewSurfaceHeight / aspect);
            ;
        } else {
            // ландшафтный
            camera.setDisplayOrientation(0);
            lp.width = previewSurfaceWidth;
            lp.height = (int) (previewSurfaceWidth / aspect);
        }

        preview.setLayoutParams(lp);
        camera.startPreview();
        camera.autoFocus(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        if (v == shotBtn) {
            this.disabled = true;
            showLoader();
            hideShotBtn();
            hideWaitText();

            camera.takePicture(null, null, null, this);
        } else if (v == autoBtn) {
            automatic = !automatic;
            if (!automatic) {
                autoBtn.setTextColor(Color.parseColor("#696969"));
                counter = 0;
            } else {
                autoBtn.setTextColor(Color.parseColor("#FFFFFF"));
            }

        } else if(v == doneBtn){
            this.done();
        } else if(v == cancelBtn){
            this.cancel();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.cancel();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void cancel(){
        hideWaitText();
        hideShotBtn();
        showLoader();
        doneBtn.setVisibility(View.GONE);
        doneBtn.setVisibility(View.INVISIBLE);
        disabled = true;
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        Draw(null);

        Intent data = new Intent();

        setResult(RESULT_CANCELED, data);
        finish();
    }

    private void done() {


            /*
            if (Imgcodecs.imwrite(destination, destImage)) {
                Log.e("savedfilde", "yes");
            } else {
                Log.e("savedfilde", "no");
            }*/
        hideWaitText();
        hideShotBtn();
        showLoader();
        doneBtn.setVisibility(View.GONE);
        doneBtn.setVisibility(View.INVISIBLE);
        disabled = true;
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        Draw(null);

        new Thread() {
            @Override
            public void run() {
                String[] filePathes = new String[pages.size()];

                for (int i = 0; i < pages.size(); i++) {
                    Mat image = pages.get(i);

                    String newPath = cachePath + "/" + System.currentTimeMillis() + i + "/";
                    File dir = new File(newPath);
                    if (!dir.exists())
                        dir.mkdirs();
                    String destination = newPath + "original_document.jpg";
                    filePathes[i] = destination;
                    if (Imgcodecs.imwrite(destination, image)) {
                        Log.e("savedfilde", "yes");
                    } else {
                        Log.e("savedfilde", "no");
                    }
                    image.release();
            /*PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(image.width(), image.height(), i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);*/
/*
            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, bmp);
            *//*drawOpencv(bmp);*//*
            image.release();*/

          /*  page.getCanvas().drawBitmap(bmp, 0, 0, null);
            document.finishPage(page);*/
                }

                Intent data = new Intent();

                data.putExtra("paths", filePathes);


        /*File file = new File(newPath, "original_document.pdf");


        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            document.writeTo(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.e("hallolandError", Arrays.asList(e.getStackTrace()).toString());

        }*/


                setResult(RESULT_OK, data);
                finish();
            }
        }.start();
        // PdfDocument document = new PdfDocument();

    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Mat frame = Imgcodecs.imdecode(new MatOfByte(paramArrayOfByte), Imgcodecs.IMREAD_UNCHANGED);
                    Core.transpose(frame, frame);
                    Core.flip(frame, frame, 1);
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);

                    Mat destImage = null;
                    if (lastPoints != null) {
                        for (int i = 0; i < lastPoints.size(); i++) {
                            Point point = lastPoints.get(i);
                            point.x *= ratioWidth * (float) frame.width() / (float) preview.getWidth(); //(double) (frame.width() / srcMat.width());
                            point.y *= ratioHeight * (float) frame.height() / (float) preview.getHeight();///(double) (frame.height() / srcMat.height());
                        }

                        org.opencv.core.Size resultSize = MatHelper.calculateFrameSizeFromPoints(lastPoints, ratio);

                        MatOfPoint2f src = new MatOfPoint2f(
                                (Point[]) lastPoints.toArray()
                        );

                        MatOfPoint2f dst = new MatOfPoint2f(
                                new Point(0, 0),
                                new Point(resultSize.width, 0),
                                new Point(0, resultSize.height),
                                new Point(resultSize.width, resultSize.height)
                        );
                        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);
                        destImage = new Mat();
                        Imgproc.warpPerspective(frame, destImage, warpMat, resultSize);
                    } else {
                        destImage = frame;
                    }


                    pages.add(destImage);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoader();
                            Draw(null);
                            rectangleView.clear();

                            camera.startPreview();
                            camera.autoFocus(context);
                            if (pages.size() == 1) {
                                doneBtn.setVisibility(View.GONE);
                                doneBtn.setVisibility(View.VISIBLE);
                            }
                            doneBtn.setText("Done (" + pages.size() + ")");
                            showShotBtn();

                        }
                    });

                    counter = 0;
                    disabled = false;


                } catch (Exception e) {
                    Log.e("hallolandError", Arrays.asList(e.getStackTrace()).toString());
                }
            }
        }.start();
    }


    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera) {
        if (paramBoolean) {
           /* if(!paramBoolean){
                paramCamera.autoFocus(this);
            }*/
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        camera.autoFocus(this);

        return super.onTouchEvent(event);

    }

    public void setLoaderVisibility(int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                progressBar.setVisibility(visibility);
            }
        });
    }

    public void hideLoader() {
        setLoaderVisibility(View.INVISIBLE);
    }

    public void showLoader() {
        setLoaderVisibility(View.VISIBLE);
    }

    public void setShotBtnVisibility(int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shotBtn.setVisibility(View.GONE);
                shotBtn.setVisibility(visibility);
            }
        });
    }

    public void showShotBtn() {
        setShotBtnVisibility(View.VISIBLE);

    }

    public void hideShotBtn() {
        setShotBtnVisibility(View.INVISIBLE);
    }


    public void setWaitTextVisibility(int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                waitText.setVisibility(View.GONE);
                waitText.setVisibility(visibility);
            }
        });
    }

    public void showWaitText() {
        setWaitTextVisibility(View.VISIBLE);

    }

    public void hideWaitText() {
        setWaitTextVisibility(View.INVISIBLE);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera paramCamera) {
        if (disabled) {
            return;
        }


        if (finished) {
            finished = false;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Camera.Parameters parameters = camera.getParameters();

                    int height = parameters.getPreviewSize().height;

                    int width = parameters.getPreviewSize().width;
                    ratio = (double) width / 800;
                    Mat frame = MatHelper.rotate(MatHelper.getMatFromData(data, width, height, ratio));
                    Mat mask = MatHelper.applyFilters(frame);

                    // drawOpencv(mask);


                    if (!mask.empty()) {
                        List<Point> rectangle = findBiggestRectangle(mask);
                        if (rectangle != null) {
                            if (lastPoints != null && automatic) {
                                List<Point> points = RectangleSearcher.sortRectPoints(lastPoints);
                                List<Point> newPoints = RectangleSearcher.sortRectPoints(rectangle);
                                boolean canAddCounter = true;
                                for (int i = 0; i < points.size(); i++) {
                                    if (GeometryTools.getLineLength(points.get(i), newPoints.get(i)) > 10) {
                                        canAddCounter = false;
                                    }
                                }

                                if (canAddCounter) {
                                    counter++;

                                } else {
                                    counter = 0;
                                }


                                if (counter == 5) {
                                    DrawOnUi(rectangle);
                                    showLoader();
                                    disabled = true;
                                    hideWaitText();
                                    hideShotBtn();
                                    camera.takePicture(null, null, null, context);

                                }
                            }
                            DrawOnUi(rectangle);


                        } else {
                            lastPoints = null;
                            counter = 0;
                            DrawOnUi(null);
                        }
                        if (counter == 2) {
                            showWaitText();
                        } else if (counter == 0) {
                            hideWaitText();
                        }

                    }

                    finished = true;
                }
            };

            thread.start();
        }

    }


    private List<Point> findBiggestRectangle(Mat mask) {
        List<Point> resultPoints = PointsHelper.getEstimatedPoints(mask, linesThreshold);


        if (resultPoints == null) {
            return null;
        }
        int currentThreshHold = linesThreshold;
        if (resultPoints.size() < 4) {
            while (resultPoints != null && resultPoints.size() < 4 && currentThreshHold > 40) {

                currentThreshHold -= 10;
                resultPoints = PointsHelper.getEstimatedPoints(mask, currentThreshHold);
            }
        }


        if (resultPoints == null) {
            return null;
        }

        Point[] bestRectangle = rectangleSearcher.getBiggestRectangleFromPoints(resultPoints);


        List<Point> rectangle = null;
        if (bestRectangle != null) {
            rectangle = Arrays.asList(bestRectangle);

        }

        ratioHeight = (float) preview.getHeight() / mask.height();
        ratioWidth = (float) preview.getWidth() / mask.width();


        return rectangle;
    }
}