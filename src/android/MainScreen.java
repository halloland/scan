package halloland.scan;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.pdf.PdfDocument;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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


    private double ratio = 1.00;

    private float ratioHeight = 0;
    private float ratioWidth = 0;

    private int linesThreshold = 200;

    boolean finished = true;
    private Mat srcMat;

    private List<Point> lastPoints;
    private int counter = 0;
    private String cachePath;
    private boolean disabled = false;

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
        shotBtn.setText("Shot");

        shotBtn.setOnClickListener(this);
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


    private void drawOpencv(Bitmap image) {

        try {
            Canvas canvas = holderOpencv.lockCanvas(null);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


            if (canvas != null) {
                Paint p = new Paint();
                p.setColor(Color.RED);
                canvas.drawColor(Color.RED);
                canvas.drawBitmap(Bitmap.createScaledBitmap(image, opencvView.getWidth(), opencvView.getHeight(), false), 0, 0, p);
            }

            holderOpencv.unlockCanvasAndPost(canvas);

        } catch (Exception e) {
            //e.printStackTrace();
        }

    }


    public List<Point> cloneList(List<Point> list) {
        List<Point> clone = new ArrayList<Point>(list.size());
        for (Point item : list) clone.add(item.clone());
        return clone;
    }

    private void DrawOnUi(List<Point> list){
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
            List<Point> scaledPoints = cloneList(points);

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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        if (v == shotBtn) {
            this.disabled = true;
            progressBar.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);


            camera.takePicture(null, null, null, this);
        }
    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
        new Thread() {
            @Override
            public void run() {
                try {

                    Mat frame = Imgcodecs.imdecode(new MatOfByte(paramArrayOfByte), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                    Core.transpose(frame, frame);
                    Core.flip(frame, frame, 1);
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);



                    for (int i = 0; i < lastPoints.size(); i++) {
                        Point point = lastPoints.get(i);
                        point.x *= ratioWidth * (float) frame.width() / (float) preview.getWidth(); //(double) (frame.width() / srcMat.width());
                        point.y *= ratioHeight * (float) frame.height() / (float) preview.getHeight();///(double) (frame.height() / srcMat.height());
                    }

                    org.opencv.core.Size resultSize = getResultFrameSize(lastPoints);

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
                    Mat destImage = new Mat();
                    Imgproc.warpPerspective(frame, destImage, warpMat, resultSize);


                    pages.add(destImage);

                    if (pages.size() == 2) {
                        String newPath = cachePath + "/" + System.currentTimeMillis() + "/";
                        File dir = new File(newPath);
                        if (!dir.exists())
                            dir.mkdirs();
                        String destination = newPath + "original_document.pdf";

            /*
            if (Imgcodecs.imwrite(destination, destImage)) {
                Log.e("savedfilde", "yes");
            } else {
                Log.e("savedfilde", "no");
            }*/
                        Intent data = new Intent();

                        data.putExtra("path", destination);


                        PdfDocument document = new PdfDocument();


                        for (int i = 0; i < pages.size(); i++) {
                            Mat image = pages.get(i);

                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(image.width(), image.height(), i + 1).create();
                            PdfDocument.Page page = document.startPage(pageInfo);

                            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(image, bmp);
                            /*drawOpencv(bmp);*/

                            page.getCanvas().drawBitmap(bmp, 0, 0, null);
                            document.finishPage(page);
                        }


                        File file = new File(newPath, "original_document.pdf");


                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file);

                            document.writeTo(fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (Exception e) {
                            Log.e("hallolandError", Arrays.asList(e.getStackTrace()).toString());

                        }


                        setResult(RESULT_OK, data);
                        finish();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                progressBar.setVisibility(View.INVISIBLE);
                                Draw(null);
                                rectangleView.clear();

                                camera.startPreview();

                            }
                        });
                        disabled = false;
                    }


                } catch (Exception e) {
                    Log.e("hallolandError", Arrays.asList(e.getStackTrace()).toString());
                }
            }
        }.start();
    }

    private org.opencv.core.Size getResultFrameSize(List<Point> rectPoints) {
        Point[] sortedPoints = (Point[]) rectPoints.toArray();
        double maxY = 0;
        double maxX = 0;
        double widthTop = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[1].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[1].y, 2));
        double widthBottom = Math.sqrt(Math.pow(sortedPoints[2].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[2].y - sortedPoints[3].y, 2));
        double heightLeft = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[2].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[2].y, 2));
        double heightRight = Math.sqrt(Math.pow(sortedPoints[1].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[1].y - sortedPoints[3].y, 2));

        maxX = (widthBottom + widthTop) / 2;
        maxY = (heightLeft + heightRight) / 2;

        return new org.opencv.core.Size(maxX * this.ratio, maxY * this.ratio);
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera) {
       /* if (paramBoolean) {

        }*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        camera.autoFocus(this);

        return super.onTouchEvent(event);

    }


    private Mat getMatFromFrameData(byte[] data) {
        int width = 0;
        int height = 0;
        Camera.Parameters parameters = camera.getParameters();

        height = parameters.getPreviewSize().height;

        width = parameters.getPreviewSize().width;

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                width, height, null);

        Rect rectangle = new Rect(0, 0, width, height);
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();


        yuvImage.compressToJpeg(rectangle, 100, out);

        byte[] imageBytes = out.toByteArray();


        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        this.ratio = (double) width / 800;

        Bitmap resized = Bitmap.createScaledBitmap(image, (int) (width / ratio), (int) (height / this.ratio), true);

        Mat mat = new Mat(resized.getWidth(), resized.getHeight(), CvType.CV_8UC1);
        Bitmap bmp32 = resized.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);

        return mat;
    }


    private Mat applyFilters(Mat frame) {
        Mat mask = new Mat();
        Imgproc.medianBlur(frame, mask, 33);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2GRAY);

        Imgproc.adaptiveThreshold(mask, mask, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 17, 2);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));

        Imgproc.erode(mask, mask, element);
        Imgproc.dilate(mask, mask, element);


        return mask;
    }


    private Point getLinesIntersectionPoint(Point A, Point B, Point C, Point D) {
        double a1 = B.y - A.y;
        double b1 = A.x - B.x;
        double c1 = a1 * (A.x) + b1 * (A.y);

        double a2 = D.y - C.y;
        double b2 = C.x - D.x;
        double c2 = a2 * (C.x) + b2 * (C.y);

        double determinant = a1 * b2 - a2 * b1;

        if (determinant == 0) {

            return new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        } else {
            double x = (b2 * c1 - b1 * c2) / determinant;
            double y = (a1 * c2 - a2 * c1) / determinant;
            return new Point(x, y);
        }
    }


    private List<Point> sortRectPoints(List<Point> sourcePoints) {
        List<Point> points = new ArrayList<>(sourcePoints);
        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                if (o1.x > o2.x) {

                    return 1;
                } else if (o1.x == o2.x) {
                    if (o1.y > o2.y) {
                        return 1;
                    } else {
                        return -1;
                    }
                }


                return -1;
            }
        });

        Point[] sortedPoints = new Point[4];

        if (points.get(0).y > points.get(1).y) {
            sortedPoints[0] = points.get(1);
            sortedPoints[2] = points.get(0);
        } else {
            sortedPoints[0] = points.get(0);
            sortedPoints[2] = points.get(1);
        }

        if (points.get(2).y > points.get(3).y) {
            sortedPoints[1] = points.get(3);
            sortedPoints[3] = points.get(2);
        } else {
            sortedPoints[1] = points.get(2);
            sortedPoints[3] = points.get(3);
        }

        return Arrays.asList(sortedPoints);
    }

    public double getLineLength(Point p1, Point p2){
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }


    public void setLoaderVisibility(int visibility){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                progressBar.setVisibility(visibility);
            }
        });
    }

    public void hideLoader(){
        setLoaderVisibility(View.INVISIBLE);
    }

    public void showLoader(){
        setLoaderVisibility(View.VISIBLE);
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

                    Mat frame = getMatFromFrameData(data);

                    // Rotate image
                    Core.transpose(frame, frame);
                    Core.flip(frame, frame, 1);
                    srcMat = frame.clone();


                    Mat mask = applyFilters(frame);
                    finished = true;

                    if (!mask.empty()) {
                        List<Point> rectangle = findBiggestRectangle(mask);
                        if (rectangle != null) {
                            if (lastPoints != null) {
                                List<Point> points = sortRectPoints(lastPoints);
                                List<Point> newPoints = sortRectPoints(rectangle);
                                boolean canAddCounter = true;
                                for (int i = 0; i < points.size(); i++){
                                    if(getLineLength(points.get(i), newPoints.get(i)) > 10){
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
                                    camera.takePicture(null, null, null, context);

                                }
                            }
                            DrawOnUi(rectangle);


                        } else {
                            counter = 0;
                            DrawOnUi(null);
                        }
                    }
                }
            };

            thread.start();
        }

    }


    private List<Point> getPoints(Mat mask, int treshHold) {
        Mat lines = new Mat();
        List<Pair<Point, Point>> linePoints = new ArrayList<>();


        Imgproc.HoughLinesP(mask, lines, 1, Math.PI / 180, treshHold, 30, 10); // runs the actual detection


        if (lines.rows() == 0 || lines.rows() > 55) {
            return null;
        }
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            double initialX = (l[2] - l[0]) * 100;
            double initialY = (l[3] - l[1]) * 100;
            linePoints.add(new Pair<>(new Point(l[0] - initialX, l[1] - initialY), new Point(l[2] + initialX, l[3] + initialY)));
        }


        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < linePoints.size(); i++) {
            Pair<Point, Point> point1 = linePoints.get(i);
            for (int j = 0; j < linePoints.size(); j++) {
                if (j != i) {
                    Pair<Point, Point> point2 = linePoints.get(j);
                    Point intersect = this.getLinesIntersectionPoint(point1.first, point1.second, point2.first, point2.second);
                    if (intersect.x < mask.width() && intersect.y < mask.height() && !points.contains(intersect)) {
                        double cos = Math.abs(this.getCos(
                                new Point(point1.second.x - point1.first.x, point1.second.y - point1.first.y),
                                new Point(point2.second.x - point2.first.x, point2.second.y - point2.first.y)
                        ));


                        if (cos <= 0.2) {
                            points.add(intersect);
                        }
                    }

                }
            }
        }

        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            for (int j = 0; j < points.size(); j++) {
                if (j != i) {
                    Point p2 = points.get(j);

                    double distance = getLineLength(p1, p2);
                    if (distance < 25) {
                        points.remove(p2);
                        j--;
                    }

                }
            }
        }

        return points;


    }

    private List<Point> findBiggestRectangle(Mat mask) {
        List<Point> resultPoints = this.getPoints(mask, linesThreshold);


        if (resultPoints == null) {
            return null;
        }
        int currentThreshHold = linesThreshold;
        if (resultPoints.size() < 4) {
            while (resultPoints != null && resultPoints.size() < 4 && currentThreshHold > 40) {

                currentThreshHold -= 10;
                resultPoints = this.getPoints(mask, currentThreshHold);
            }
        }


        if (resultPoints == null) {
            return null;
        }


        List<Point[]> contours = new ArrayList<>();

        Point[] bestRectanle = null;
        double maxArea = 0;

        for (int firstPointIndex = 0; firstPointIndex < resultPoints.size(); firstPointIndex++) {
            Point p1 = resultPoints.get(firstPointIndex);

            for (int secondPointIndex = 0; secondPointIndex < resultPoints.size(); secondPointIndex++) {
                if (firstPointIndex != secondPointIndex) {


                    Point p2 = resultPoints.get(secondPointIndex);


                    for (int thirdPointIndex = 0; thirdPointIndex < resultPoints.size(); thirdPointIndex++) {
                        if (thirdPointIndex != firstPointIndex && thirdPointIndex != secondPointIndex) {
                            Point p3 = resultPoints.get(thirdPointIndex);

                            for (int fourthPointIndex = 0; fourthPointIndex < resultPoints.size(); fourthPointIndex++) {
                                if (fourthPointIndex != firstPointIndex && fourthPointIndex != secondPointIndex && fourthPointIndex != thirdPointIndex) {
                                    Point p4 = resultPoints.get(fourthPointIndex);

                                    boolean duplicated = false;

                                    for (int k = 0; k < contours.size(); k++) {
                                        Point[] foundRect = contours.get(k);
                                        int duplicatedPoints = 0;
                                        for (Point point : foundRect) {
                                            if (point == p1
                                                    || point == p2
                                                    || point == p3
                                                    || point == p4
                                            ) {

                                                duplicatedPoints++;

                                            }
                                        }
                                        if (duplicatedPoints == 4) {
                                            duplicated = true;

                                        }


                                    }

                                    if (!duplicated) {


                                        Point intersect = this.getLinesIntersectionPoint(p1, p2, p3, p4);


                                        if (intersect.x < srcMat.width() && intersect.y < srcMat.height()) {
                                            Point[] pointx = new Point[4];
                                            pointx[0] = p1;
                                            pointx[1] = p2;
                                            pointx[2] = p3;
                                            pointx[3] = p4;
                                            contours.add(pointx);

                                            double lengthP1 = getLineLength(p1, p2);
                                            double lengthP2 = getLineLength(p3, p4);
                                            double ratio = lengthP1 / lengthP2;


                                            if (ratio > 0.8 && ratio < 1.2) {

                                                Point[] foundPoints = new Point[4];
                                                foundPoints[0] = p1;
                                                foundPoints[1] = p2;
                                                foundPoints[2] = p3;
                                                foundPoints[3] = p4;


                                                foundPoints = (Point[]) sortRectPoints(Arrays.asList(foundPoints)).toArray();


                                                double contourArea = Imgproc.contourArea(new MatOfPoint(foundPoints));

                                                if (contourArea > maxArea && this.isValidRectanle(Arrays.asList(foundPoints))) {
                                                    maxArea = contourArea;
                                                    bestRectanle = foundPoints;
                                                }


                                            }

                                        }
                                    }
                                }
                            }

                        }

                    }
                }

            }
        }
        List<Point> rectangle = null;
        if (bestRectanle != null) {
            rectangle = Arrays.asList(bestRectanle);

        }


        ratioHeight = (float) preview.getHeight() / mask.height();
        ratioWidth = (float) preview.getWidth() / mask.width();


        return rectangle;
    }

    private boolean isValidRectanle(List<Point> points) {
        boolean allowed = true;


        double cos1 = Math.abs(this.getCos(
                new Point(points.get(2).x - points.get(0).x, points.get(2).y - points.get(0).y),
                new Point(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y)
        ));

        double cos2 = Math.abs(this.getCos(
                new Point(points.get(3).x - points.get(2).x, points.get(3).y - points.get(2).y),
                new Point(points.get(3).x - points.get(1).x, points.get(3).y - points.get(1).y)
        ));

        double cos3 = Math.abs(this.getCos(
                new Point(points.get(3).x - points.get(1).x, points.get(3).y - points.get(1).y),
                new Point(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y)
        ));

        double cos4 = Math.abs(this.getCos(
                new Point(points.get(3).x - points.get(2).x, points.get(3).y - points.get(2).y),
                new Point(points.get(2).x - points.get(0).x, points.get(2).y - points.get(0).y)
        ));


        if (cos1 > 0.2 || cos2 > 0.2 || cos3 > 0.2 || cos4 > 0.2) {
            allowed = false;
        }


        return allowed;
    }

    private double getCos(Point p1, Point p2) {
        double dotProd = p1.x * p2.x + p1.y * p2.y;
        double length1 = Math.sqrt(Math.pow(p1.x, 2) + Math.pow(p1.y, 2));
        double length2 = Math.sqrt(Math.pow(p2.x, 2) + Math.pow(p2.y, 2));

        return dotProd / (length1 * length2);
    }
}