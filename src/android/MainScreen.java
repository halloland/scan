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
import android.widget.SeekBar;
import android.widget.TextView;

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

public class MainScreen extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback,
        SeekBar.OnSeekBarChangeListener {

    MainScreen context;

    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder holderTransparent;

    private SurfaceHolder holderOpencv;
    private SurfaceView transparentView;
    private SurfaceView opencvView;
    private ProgressBar progressBar;

    private RectangleView rectangleView;

    private SurfaceView preview;
    private Button shotBtn;
    private int blur = 2;

    private double ratio = 1.00;

    private float ratioHeight = 0;
    private float ratioWidth = 0;
    private int treshHold1 = 50;
    private int treshHold2 = 200;

    private int b = 33;
    private int c = 2;

    private TextView textB;
    private TextView textC;

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
        transparentView = (SurfaceView) findViewById(this.getResourceId("SurfaceView02", "id"));
        opencvView = (SurfaceView) findViewById(this.getResourceId("SurfaceView03", "id"));


        surfaceHolder = preview.getHolder();
        holderTransparent = transparentView.getHolder();
        holderTransparent.addCallback((SurfaceHolder.Callback) new LinesDrawer());
        holderTransparent.setFormat(PixelFormat.TRANSLUCENT);

        holderOpencv = opencvView.getHolder();
        holderOpencv.addCallback((SurfaceHolder.Callback) new LinesDrawer());
        holderOpencv.setFormat(PixelFormat.TRANSLUCENT);


        transparentView.setZOrderMediaOverlay(true);
        opencvView.setZOrderMediaOverlay(true);


        surfaceHolder.addCallback(this);

        textC = (TextView) findViewById(this.getResourceId("textViewC", "id"));

        textB = (TextView) findViewById(this.getResourceId("textViewB", "id"));

        progressBar = (ProgressBar) findViewById(this.getResourceId("progressBar", "id"));

        rectangleView = (RectangleView) findViewById(this.getResourceId("rectangle", "id"));


        shotBtn = (Button) findViewById(this.getResourceId("Button01", "id"));
        shotBtn.setText("Shot");

        shotBtn.setOnClickListener(this);


        final SeekBar seekBar = (SeekBar) findViewById(getResourceId("seekBar", "id"));
        seekBar.setOnSeekBarChangeListener(this);

        final SeekBar seekBar2 = (SeekBar) findViewById(getResourceId("seekBar2", "id"));
        seekBar2.setOnSeekBarChangeListener(this);


    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == getResourceId("seekBar", "id")) {
            this.treshHold1 = progress;
        } else {
            this.treshHold2 = progress;
        }


    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

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

    public void addB() {
        this.b += 2;
        this.textB.setText(String.valueOf(this.b));
    }

    public void minusB() {
        this.b -= 2;
        this.textB.setText(String.valueOf(this.b));
    }

    public void minusC() {
        this.c--;
        this.textC.setText(String.valueOf(this.c));
    }

    public void plusC() {
        this.c++;
        this.textC.setText(String.valueOf(this.c));
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


        /*

        Canvas canvas = holderTransparent.lockCanvas(null);


        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (points != null) {
            points = this.orderPointsClockwise(new MatOfPoint2f((Point[]) points.toArray())).toList();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStyle(Paint.Style.STROKE);

            paint.setColor(Color.GREEN);

            paint.setStrokeWidth(8);

            Point firstPoint = points.get(0);

            Point previousPoint = firstPoint;
            for (int i = 1; i < points.size(); i++) {
                Point point = points.get(i);

                canvas.drawLine((float) previousPoint.x * ratioWidth, (float) previousPoint.y * ratioHeight,
                        (float) point.x * ratioWidth, (float) point.y * ratioHeight, paint);

                previousPoint = point;
            }
            canvas.drawLine((float) previousPoint.x * ratioWidth, (float) previousPoint.y * ratioHeight,
                    (float) firstPoint.x * ratioWidth, (float) firstPoint.y * ratioHeight, paint);
        }

        holderTransparent.unlockCanvasAndPost(canvas);*/


    }

    MatOfPoint2f orderPointsClockwise(MatOfPoint2f screenCnt2f) {
        System.out.println(screenCnt2f.dump());

        List<Point> points = screenCnt2f.toList();
        // # initialize a list of coordinates that will be ordered
        // # such that the first entry in the list is the top-left,
        // # the second entry is the top-right, the third is the
        // # bottom-right, and the fourth is the bottom-left
        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                double s1 = p1.x + p1.y;
                double s2 = p2.x + p2.y;
                return Double.compare(s1, s2);
            }
        });
        Point topLeft = points.get(0);
        Point bottomRight = points.get(3);


        // # now, compute the difference between the points, the
        // # top-right point will have the smallest difference,
        // # whereas the bottom-left will have the largest difference
        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                double s1 = p1.y - p1.x;
                double s2 = p2.y - p2.x;
                return Double.compare(s1, s2);
            }
        });
        Point topRight = points.get(0);
        Point bottomLeft = points.get(3);

        Point[] pts = new Point[]{topLeft, topRight, bottomRight, bottomLeft};

        screenCnt2f = new MatOfPoint2f(pts);
        // System.out.println(screenCnt2f.dump());
        return screenCnt2f;
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
        Log.d("surfacechanged", "true");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (holder) {

        }
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
        } else if (v.getId() == getResourceId("buttonPlusB", "id")) {
            this.addB();
        } else if (v.getId() == getResourceId("buttonMinusB", "id")) {
            this.minusB();
        } else if (v.getId() == getResourceId("buttonPlusC", "id")) {
            this.plusC();
        } else if (v.getId() == getResourceId("buttonMinusC", "id")) {
            this.minusC();
        }

       /* List<Point> rect = new ArrayList<>();
        List<Point> rect2 = new ArrayList<>();
        rect.add(new Point(50, 50));
        rect.add(new Point(50, 100));
        rect.add(new Point(100, 50));
        rect.add(new Point(100, 100));


        rect2.add(new Point(50 + 400, 50+ 400));
        rect2.add(new Point(50+ 400, 100+ 400));
        rect2.add(new Point(100+ 400, 50+ 400));
        rect2.add(new Point(100+ 400, 100+ 400));

        if(blur == 2){
            rectangleView.render(rect);
            blur = 1;
        } else if(blur == 1){
            rectangleView.render(rect2);
            blur = 3;
        } else {
            blur = 2;
            rectangleView.render(null);
        }*/


    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("hallolandError", "1");

                    Mat frame = Imgcodecs.imdecode(new MatOfByte(paramArrayOfByte), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                    Core.transpose(frame, frame);
                    Core.flip(frame, frame, 1);
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
                    Log.e("hallolandError", "2 " + String.valueOf(lastPoints));

                    for (int i = 0; i < lastPoints.size(); i++) {
                        Point point = lastPoints.get(i);
                        point.x *= ratioWidth * (float) frame.width() / (float) preview.getWidth(); //(double) (frame.width() / srcMat.width());
                        point.y *= ratioHeight * (float) frame.height() / (float) preview.getHeight();///(double) (frame.height() / srcMat.height());
                    }
                    Log.e("hallolandError", "3");

                    org.opencv.core.Size resultSize = getResultFrameSize(lastPoints);

                    MatOfPoint2f src = new MatOfPoint2f(
                            (Point[]) lastPoints.toArray()
                    );

                    Log.e("hallolandError", "4");
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


                    Log.e("hallolandError", "5");


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
                        Log.e("success", "success " + frame.width() + " " + frame.height());

                        data.putExtra("path", destination);


                        PdfDocument document = new PdfDocument();


                        for (int i = 0; i < pages.size(); i++) {
                            Mat image = pages.get(i);

                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(image.width(), image.height(), i + 1).create();
                            PdfDocument.Page page = document.startPage(pageInfo);

                            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
                          /*  Utils.matToBitmap(image, bmp);
                            drawOpencv(bmp);*/

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

                            /*camera.setPreviewCallback(null);
                            camera.stopPreview();
                            camera.release();
                            camera = null;*/

                         /*   camera.stopPreview();
                            camera.startPreview();*/

                                camera.startPreview();

                            }
                        });
                        disabled = false;
                    }
                    Log.e("hallolandError", "6");

                    Log.e("hallolandError", "7");


                } catch (Exception e) {
                    Log.e("hallolandError", Arrays.asList(e.getStackTrace()).toString());
                }
            }
        }.start();


        // после того, как снимок сделан, показ превью отключается. необходимо включить его

    }

    private org.opencv.core.Size getResultFrameSize(List<Point> rectPoints) {
        Point[] sortedPoints = (Point[]) rectPoints.toArray();
        double maxY = 0;
        double maxX = 0;
        double widthTop = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[1].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[1].y, 2));
        double widthBottom = Math.sqrt(Math.pow(sortedPoints[2].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[2].y - sortedPoints[3].y, 2));
        double heightLeft = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[2].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[2].y, 2));
        double heightRight = Math.sqrt(Math.pow(sortedPoints[1].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[1].y - sortedPoints[3].y, 2));

       /* if (widthTop > widthBottom) {
            maxX = widthTop;
        } else {
            maxX = widthBottom;
        }*/
        maxX = (widthBottom + widthTop) / 2;
        /*if (heightLeft > heightRight) {
            maxY = heightLeft;
        } else {
            maxY = heightRight;
        }*/

        maxY = (heightLeft + heightRight) / 2;

        return new org.opencv.core.Size(maxX * this.ratio, maxY * this.ratio);
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera) {
        if (paramBoolean) {
            // если удалось сфокусироваться, делаем снимок
            // paramCamera.takePicture(null, null, null, this);
        }
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
        //return Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_ANYDEPTH);
    }


    private Mat applyFilters(Mat frame) {
        Mat mask = new Mat();
        //frame.convertTo(frame, -1, 0.1, treshHold1);
        //Imgproc.blur(frame, mask, new org.opencv.core.Size(3, 3));
        Mat newDest = frame.clone();
        //Imgproc.GaussianBlur(frame, mask, new org.opencv.core.Size(25, 25), 0);
        Imgproc.medianBlur(frame, mask, b);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2GRAY);

        Imgproc.adaptiveThreshold(mask, mask, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 17, c);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));

        //Imgproc.erode(mask, mask, element);
        /*Mat dest = new Mat();
        Core.add(dest, Scalar.all(0), dest);
        frame.copyTo(dest, mask);*/


        //Imgproc.Canny(mask, mask, treshHold1, 100, 3, false);
        //Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, element, new Point(-1, 1), 1);
        Imgproc.erode(mask, mask, element);
        Imgproc.dilate(mask, mask, element);
        //Imgproc.dilate(mask, mask, element);


        Mat lines = new Mat(); // will hold the results of the detection


        // Imgproc.HoughLinesP(mask, lines, 1, Math.PI/180, treshHold2, 30, 10); // runs the actual detection
        // Log.d("treshHold2", String.valueOf(lines.rows()));




       /* if(lines.rows() > 60){
            treshHold2 = 250;
        } else if(lines.rows() < 5){
            treshHold2 = 30;
        } else {
            treshHold2 = 100;
        }

        while ((lines.rows() > 50 || lines.rows() < 5) && (treshHold2 > 11 && treshHold2 < 245)){
            if(lines.rows() < 50){
                treshHold2 -= 10;
            } else {
                treshHold2 += 10;
            }

            Log.d("treshHold2", String.valueOf(treshHold2));
            Imgproc.HoughLinesP(mask, lines, 1, Math.PI/180, treshHold2, 30, 10); // runs the actual detection
        }*/


       /* if(lines.rows() == 0 || lines.rows() > 60){
            return null;
        }
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            double initialX = (l[2] - l[0]) * 100;
            double initialY = (l[3] - l[1]) * 100;
            Imgproc.line(mask, new Point(l[0] - initialX, l[1] - initialY), new Point(l[2] + initialX, l[3] + initialY), new Scalar(255, 0, 255), 3, Imgproc.LINE_AA, 0);
        }*/


       /* Bitmap bmp = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mask, bmp);
        drawOpencv(bmp);*/
        return mask;
    }


    private Point lineLineIntersection(Point A, Point B, Point C, Point D) {
        // Line AB represented as a1x + b1y = c1
        double a1 = B.y - A.y;
        double b1 = A.x - B.x;
        double c1 = a1 * (A.x) + b1 * (A.y);

        // Line CD represented as a2x + b2y = c2
        double a2 = D.y - C.y;
        double b2 = C.x - D.x;
        double c2 = a2 * (C.x) + b2 * (C.y);

        double determinant = a1 * b2 - a2 * b1;

        if (determinant == 0) {
            // The lines are parallel. This is simplified
            // by returning a pair of FLT_MAX
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

    @Override
    public void onPreviewFrame(byte[] data, Camera paramCamera) {
        Log.e("error_points", "1");
        if (disabled) {
            return;
        }

        Log.e("error_points", "2");

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

                                double l1 = Math.sqrt(Math.pow(points.get(0).x - newPoints.get(0).x, 2) + Math.pow(points.get(0).y - newPoints.get(0).y, 2));
                                double l2 = Math.sqrt(Math.pow(points.get(1).x - newPoints.get(1).x, 2) + Math.pow(points.get(1).y - newPoints.get(1).y, 2));
                                double l3 = Math.sqrt(Math.pow(points.get(2).x - newPoints.get(2).x, 2) + Math.pow(points.get(2).y - newPoints.get(2).y, 2));
                                double l4 = Math.sqrt(Math.pow(points.get(3).x - newPoints.get(3).x, 2) + Math.pow(points.get(3).y - newPoints.get(3).y, 2));

                                if (l1 < 10 && l2 < 10 && l3 < 10 && l4 < 10) {
                                    counter++;

                                } else {
                                    counter = 0;
                                }

                                if (counter == 5) {

                                    context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Draw(rectangle);
                                            progressBar.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    disabled = true;
                                    camera.takePicture(null, null, null, context);

                                }
                            }
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Draw(rectangle);
                                }
                            });


                        } else {
                            counter = 0;
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Draw(null);
                                }
                            });
                        }
                    }

                    Log.d("counter1", String.valueOf(counter));
                }
            };

            thread.start();
        }

    }


    private List<Point> getPoints(Mat mask, int treshHold) {
        Mat lines = new Mat(); // will hold the results of the detection
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
            //Imgproc.line(newDest, new Point(l[0] - initialX, l[1] - initialY), new Point(l[2] + initialX, l[3] + initialY), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }


        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < linePoints.size(); i++) {
            Pair<Point, Point> point1 = linePoints.get(i);
            for (int j = 0; j < linePoints.size(); j++) {
                if (j != i) {
                    Pair<Point, Point> point2 = linePoints.get(j);
                    Point intersect = this.lineLineIntersection(point1.first, point1.second, point2.first, point2.second);
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

        List<Point> resultPoints = points;

        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            for (int j = 0; j < points.size(); j++) {
                if (j != i) {
                    Point p2 = points.get(j);

                    double distance = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
                    if (distance < 25) {
                        resultPoints.remove(p2);
                        j--;
                    }

                }
            }
        }

        return resultPoints;


    }

    private List<Point> findBiggestRectangle(Mat mask) {
        List<Point> resultPoints = this.getPoints(mask, treshHold2);


        if (resultPoints == null) {
            return null;
        }
        int currentThreshHold = treshHold2;
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

        for (int i = 0; i < resultPoints.size(); i++) {
            Point p1 = resultPoints.get(i);
            // Imgproc.circle(this.srcMat, p1, 2, new Scalar(255, 255, 0), 2);

            for (int j = 0; j < resultPoints.size(); j++) {
                if (i != j) {


                    Point p2 = resultPoints.get(j);


                    for (int q = 0; q < resultPoints.size(); q++) {
                        if (q != i && q != j) {
                            Point p3 = resultPoints.get(q);

                            for (int p = 0; p < resultPoints.size(); p++) {
                                if (p != i && p != j && p != q) {
                                    Point p4 = resultPoints.get(p);
//                                    Imgproc.line(srcMat, p1, p2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
//                                    Imgproc.line(srcMat, p3, p4, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
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


                                        Point intersect = this.lineLineIntersection(p1, p2, p3, p4);


                                        if (intersect.x < srcMat.width() && intersect.y < srcMat.height()) {
                                            Point[] pointx = new Point[4];
                                            pointx[0] = p1;
                                            pointx[1] = p2;
                                            pointx[2] = p3;
                                            pointx[3] = p4;
                                            contours.add(pointx);

                                            double lengthP1 = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
                                            double lengthP2 = Math.sqrt(Math.pow(p3.x - p4.x, 2) + Math.pow(p3.y - p4.y, 2));
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
            //Draw(Arrays.asList(bestRectanle));

        }

       /* Bitmap bmp = Bitmap.createBitmap(srcMat.cols(), srcMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcMat, bmp);
        drawOpencv(bmp);*/
        ratioHeight = (float) preview.getHeight() / mask.height();
        ratioWidth = (float) preview.getWidth() / mask.width();

        //Log.d("ratios", ratioWidth + " " + ratioHeight);


        /*if(points.size() > 3){

            double maxVal = 0;

            MatOfPoint2f approx = new MatOfPoint2f();
            try {

                Point[] points_array = (Point[]) points.toArray(new Point[0]);
                Imgproc.approxPolyDP(new MatOfPoint2f(points_array), approx, Imgproc.arcLength(new MatOfPoint2f(points_array), true) * 0.1, true);
            } catch (Exception e2){
                Log.w("errorss", e2.getMessage());
            }


            double contourArea = 0;
            List<Point> approxPoints = approx.toList();
            try{
                contourArea = Imgproc.contourArea(new MatOfPoint((Point[]) approxPoints.toArray(new Point[0])));
            } catch (Exception e){
                Log.w("errorss", e.getMessage());
            }


            if(approxPoints.size() > 3){
                List<Point> rectPoints = this.findRectangleFromPoints(approxPoints);

                if(true*//*this.isValidRectanle(rectPoints)*//*) {


                    if (rectangle == null) {
                        rectangle = rectPoints;
                    } else {
                        if (maxVal < contourArea) {

                            maxVal = contourArea;
                            rectangle = rectPoints;
                        }
                    }
                }
            }

        }*/
        /*Mat hierarchy
        Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxVal = 0;
        for (MatOfPoint contour : contours) {
            RotatedRect boundingRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

            double rectangleArea = boundingRect.size.area();


            if (rectangleArea > 10000) {
                Point[] rotated_rect_points = new Point[4];
                boundingRect.points(rotated_rect_points);
                double contourArea = Imgproc.contourArea(contour);

                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) * 0.005, true);


                if(approx.total() > 3){
                    List<Point> points2 = this.findRectangleFromPoints(approx.toList());


                    if(this.isValidRectanle(points2)) {
                        if (rectangle == null) {
                            rectangle = points2;
                        } else {
                            if (maxVal < contourArea) {
                                maxVal = contourArea;
                                rectangle = points2;
                            }
                        }
                    }
                }
            }
        }*/

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