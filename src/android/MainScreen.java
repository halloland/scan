package halloland.scan;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.view.View;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainScreen extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback,
        SeekBar.OnSeekBarChangeListener
{
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder holderTransparent;

    private SurfaceHolder holderOpencv;
    private SurfaceView transparentView;
    private SurfaceView opencvView;

    private SurfaceView preview;
    private Button shotBtn;



    private float ratioHeight = 0;
    private float ratioWidth = 0;

    int  deviceHeight,deviceWidth;

    private int treshHold1 = 75;

    private Mat srcMat;

    private List<Point> lastPoints;

    private String cachePath;

    public int getResourceId(String name, String defType) {
        return getResources().getIdentifier(name, defType, this.getPackageName());
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String cachePath = intent.getStringExtra("cachePath");

        if(cachePath != null){
            this.cachePath = cachePath;
        }



        if(!OpenCVLoader.initDebug()){
            Log.d("CVerror","OpenCV library Init failure");
        }else{

            Log.d("CVerror","success");
            // load your library and do initializing stuffs like System.loadLibrary();
        }

        // если хотим, чтобы приложение постоянно имело портретную ориентацию
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // если хотим, чтобы приложение было полноэкранным
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // и без заголовка
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(this.getResourceId("scan", "layout"));


        // наше SurfaceView имеет имя SurfaceView01
        preview = (SurfaceView) findViewById(this.getResourceId("SurfaceView01", "id"));
        transparentView = (SurfaceView)findViewById(this.getResourceId("SurfaceView02", "id"));
        opencvView = (SurfaceView)findViewById(this.getResourceId("SurfaceView03", "id"));


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

        // кнопка имеет имя Button01
        shotBtn = (Button) findViewById(this.getResourceId("Button01", "id"));
        shotBtn.setText("Shot");
        shotBtn.setOnClickListener(this);
        deviceWidth=getScreenWidth();

        deviceHeight=getScreenHeight();


        final SeekBar seekBar = (SeekBar)findViewById(getResourceId("seekBar", "id"));
        seekBar.setOnSeekBarChangeListener(this);



    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getId() == getResourceId("seekBar", "id")){
            this.treshHold1 = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    private class LinesDrawer implements SurfaceHolder.Callback{

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

    public static int getScreenWidth() {

        return Resources.getSystem().getDisplayMetrics().widthPixels;

    }



    public static int getScreenHeight() {

        return Resources.getSystem().getDisplayMetrics().heightPixels;

    }

    private void drawOpencv(Bitmap image){

        try {
            Canvas canvas = holderOpencv.lockCanvas(null);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);



            if(canvas != null){
                Paint p=new Paint();
                p.setColor(Color.RED);
                canvas.drawColor(Color.RED);
                canvas.drawBitmap(Bitmap.createScaledBitmap(image, opencvView.getWidth(), opencvView.getHeight(), false), 0, 0, p);
            }

            holderOpencv.unlockCanvasAndPost(canvas);

        } catch (Exception e){
            //e.printStackTrace();
        }

    }


    private void Draw(List<Point> points)
    {
        lastPoints = points;

        Canvas canvas = holderTransparent.lockCanvas(null);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


        //holderTransparent.setFixedSize(width, height);
        if(canvas != null){
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStyle(Paint.Style.STROKE);

            paint.setColor(Color.GREEN);

            paint.setStrokeWidth(3);

            Log.d("points", String.valueOf(points.size()));
            Point firstPoint = points.get(0);

            Point previousPoint = firstPoint;
            for (int i = 1; i < points.size(); i++) {
                Point point = points.get(i);

                canvas.drawLine((float) previousPoint.x * ratioWidth, (float) previousPoint.y * ratioHeight,
                        (float) point.x * ratioWidth, (float) point.y * ratioHeight, paint );

                previousPoint = point;
            }





            canvas.drawLine((float) previousPoint.x * ratioWidth, (float) previousPoint.y * ratioHeight,
                    (float) firstPoint.x * ratioWidth, (float) firstPoint.y * ratioHeight, paint );


            holderTransparent.unlockCanvasAndPost(canvas);
        }




    }

    @Override
    protected void onResume()
    {
        super.onResume();
        camera = Camera.open();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (camera != null)
        {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }

    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        synchronized (holder){

        }
        try
        {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Size previewSize = camera.getParameters().getPreviewSize();
        List<Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        Size biggestSize = null;
        int biggestArea = 0;


        float aspect = (float) previewSize.width / previewSize.height;


        for (int i =0; i < pictureSizes.size(); i++){

            Size size = pictureSizes.get(i);
            int area = size.width * size.height;
            if(area > biggestArea && (float) size.width/size.height == aspect){
                biggestSize = size;
                biggestArea = area;
            }
        }


        if(biggestSize != null){
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

        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
            // портретный вид
            camera.setDisplayOrientation(90);

            lp.height = previewSurfaceHeight;
            lp.width = (int) (previewSurfaceHeight / aspect);
            ;
        }
        else
        {
            // ландшафтный
            camera.setDisplayOrientation(0);
            lp.width = previewSurfaceWidth;
            lp.height = (int) (previewSurfaceWidth / aspect);
        }

        preview.setLayoutParams(lp);
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onClick(View v)
    {
        if (v == shotBtn)
        {
            // либо делаем снимок непосредственно здесь
            // 	либо включаем обработчик автофокуса

            camera.takePicture(null, null, null, this);

        }
    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
    {
        try
        {

            Mat frame = Imgcodecs.imdecode(new MatOfByte(paramArrayOfByte), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            Core.transpose(frame, frame);
            Core.flip(frame, frame, 1);

            for (int i = 0; i < lastPoints.size(); i++) {
                Point point = lastPoints.get(i);
                point.x *= (double) (frame.width() / srcMat.width());
                point.y *= (double) (frame.height() / srcMat.height());
            }


            Collections.sort(lastPoints, new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    if(o1.x > o2.x){

                        return 1;
                    } else if(o1.x == o2.x){
                        if(o1.y > o2.y){
                            return 1;
                        } else {
                            return -1;
                        }
                    }


                    return -1;
                }
            });


            Point[] sortedPoints = new Point[4];

            if(lastPoints.get(0).y > lastPoints.get(1).y){
                sortedPoints[0] = lastPoints.get(1);
                sortedPoints[2] = lastPoints.get(0);
            } else {
                sortedPoints[0] = lastPoints.get(0);
                sortedPoints[2] = lastPoints.get(1);
            }

            if(lastPoints.get(2).y > lastPoints.get(3).y){
                sortedPoints[1] = lastPoints.get(3);
                sortedPoints[3] = lastPoints.get(2);
            } else {
                sortedPoints[1] = lastPoints.get(2);
                sortedPoints[3] = lastPoints.get(3);
            }

            Log.e("sizes", Arrays.asList(sortedPoints).toString() + " " + ((int)(sortedPoints[0].x+ sortedPoints[1].x)^2));
            double maxY = 0;
            double maxX = 0;
            double widthTop = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[1].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[1].y, 2));
            double widthBottom = Math.sqrt(Math.pow(sortedPoints[2].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[2].y - sortedPoints[3].y, 2));
            double heightLeft = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[2].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[2].y, 2));
            double heightRight = Math.sqrt(Math.pow(sortedPoints[1].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[1].y - sortedPoints[3].y, 2));

            if(widthTop > widthBottom){
                maxX = widthTop;
            } else {
                maxX = widthBottom;
            }

            if(heightLeft > heightRight){
                maxY = heightLeft;
            } else {
                maxY = heightRight;
            }
            Log.e("sizes", widthTop +" " + widthBottom + " " + heightLeft + " " + heightRight);


            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints
            );





            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(maxX,0),
                    new Point(0,maxY),
                    new Point(maxX,maxY)
            );
            Mat warpMat = Imgproc.getPerspectiveTransform(src,dst);
            //This is you new image as Mat
            Mat destImage = new Mat();
            Imgproc.warpPerspective(frame, destImage, warpMat, new org.opencv.core.Size(maxX, maxY));

            /*Bitmap bmp = Bitmap.createBitmap(destImage.cols(), destImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(destImage, bmp);*/
            //File destination = new File(getTempDirectoryPath() + "/" + System.currentTimeMillis() + "/" + source.getName());

            String newPath = this.cachePath + "/" + System.currentTimeMillis() + "/";
            File dir = new File(newPath);
            if(!dir.exists())
                dir.mkdirs();
            String destination =  newPath + "original_image.bmp";
            if(Imgcodecs.imwrite(destination, destImage)){
                Log.e("savedfilde", "yes");
            } else {
                Log.e("savedfilde", "no");
            }
            Intent data = new Intent();
            Log.e("success", "success " + frame.width() + " " + frame.height());

            data.putExtra("path", destination);

            setResult(RESULT_OK, data);
            finish();



        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // после того, как снимок сделан, показ превью отключается. необходимо включить его
        paramCamera.startPreview();
    }

    @Override
    public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
    {
        if (paramBoolean)
        {
            // если удалось сфокусироваться, делаем снимок
           // paramCamera.takePicture(null, null, null, this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        camera.autoFocus(this);

        return super.onTouchEvent(event);

    }



    @Override
    public void onPreviewFrame(byte[] data, Camera paramCamera)
    {

        int width = 0; int height = 0;
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



        Mat blurredImage = new Mat();
        Mat hsvImage = new Mat();
        Mat morphOutput = new Mat();

        Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

        Mat gray = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        Core.transpose(frame, frame);
        Core.flip(frame, frame, 1);
        srcMat = frame.clone();

        Core.transpose(gray, gray);
        Core.flip(gray, gray, 1);
        Mat mask = new Mat();
        //Imgproc.cvtColor(frame, mask, Imgproc.COLOR_RGB2HSV);



        Imgproc.blur(gray, gray, new org.opencv.core.Size(2, 2));
        Imgproc.Canny(gray, mask, treshHold1, 40, 3, false);

        Imgproc.dilate(mask, mask, new Mat(), new Point(-1, 1), 2);
        Imgproc.erode(mask, mask, new Mat(), new Point(-1, -1), 2);

        frame = mask;
        Bitmap bmp = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mask, bmp);


        drawOpencv(bmp);
        //Imgproc.blur(mask, mask, new org.opencv.core.Size(5, 5));
        // org.opencv.core.Core.flip(frame, frame, -1);
        //org.opencv.core.Core.flip(gray, gray, -1);


        //Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.threshold(gray, morphOutput, treshHold1, 200, Imgproc.THRESH_BINARY_INV);
       // Log.d("treshhold", String.valueOf(treshHold1) + " " + String.valueOf(treshHold2));

       //

        //Imgproc.blur(mask, mask, new org.opencv.core.Size(5, 5));
        //Imgproc.erode(mask, mask, new Mat(), new Point(-1, -1), 3);




        //Imgproc.Canny(mask, mask, treshHold1, 40, 3, false);

        //Imgproc.dilate(gray, gray, new Mat(), new Point(-1, 1), 1);


        //Imgproc.dilate(gray, gray, new Mat(), new Point(-1, 1), 2);










        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        if(!frame.empty()){
            ratioHeight = (float) preview.getHeight() / frame.height();
            ratioWidth = (float) preview.getWidth() / frame.width();

            Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);



            double maxVal = 0;
            int maxValIdx = 0;


          //  Bitmap bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        //    Utils.matToBitmap(frame, bmp);

           // drawOpencv(bmp);

            org.opencv.core.Rect largestRect = null;
            org.opencv.core.Rect rect = null;
            MatOfPoint2f shape = null;

            for (MatOfPoint contour : contours) {
                RotatedRect boundingRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

                double rectangleArea = boundingRect.size.area();

                // test min ROI area in pixels
                if (rectangleArea > 250000) {
                    Point rotated_rect_points[] = new Point[4];
                    boundingRect.points(rotated_rect_points);
                    rect = Imgproc.boundingRect(new MatOfPoint(rotated_rect_points));


                    double contourArea = Imgproc.contourArea(contour);

                    MatOfPoint2f approx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) * 0.02, true);

                    long count = approx.total();

                    Log.d("countsies", String.valueOf(count));
                    if(count == 4){
                        if (largestRect == null) {
                            shape = approx;
                            largestRect = rect;
                        } else {
                            if (rect.width > largestRect.width && maxVal < contourArea) {
                                maxVal = contourArea;
                                largestRect = rect;
                                shape = approx;
                            }
                        }
                    }


                }
            }



            if(largestRect != null && shape != null){


                // TODO get shape with > 5 angles and find the biggest square

                    List<Point> points = shape.toList();
                    List<Point> drawPoints = shape.toList();;
                    Collections.sort(points, new Comparator<Point>() {
                        @Override
                        public int compare(Point o1, Point o2) {
                            if(o1.x > o2.x){

                                return 1;
                            } else if(o1.x == o2.x){
                                if(o1.y > o2.y){
                                    return 1;
                                } else {
                                    return -1;
                                }
                            }


                            return -1;
                        }
                    });


                    Point[] sortedPoints = new Point[4];

                    if(points.get(0).y > points.get(1).y){
                        sortedPoints[0] = points.get(1);
                        sortedPoints[2] = points.get(0);
                    } else {
                        sortedPoints[0] = points.get(0);
                        sortedPoints[2] = points.get(1);
                    }

                    if(points.get(2).y > points.get(3).y){
                        sortedPoints[1] = points.get(3);
                        sortedPoints[3] = points.get(2);
                    } else {
                        sortedPoints[1] = points.get(2);
                        sortedPoints[3] = points.get(3);
                    }


                double cos1 = Math.abs(this.getCos(sortedPoints[0], sortedPoints[1]));
                double cos2 = Math.abs(this.getCos(sortedPoints[2], sortedPoints[3]));

                if(cos1 > 0.80 && cos2 > 0.80){
                    Collections.sort(drawPoints, new Comparator<Point>() {
                        @Override
                        public int compare(Point o1, Point o2) {
                            if(o1.x < o2.x && o1.y < o2.y){
                                return 1;
                            }
                            return 0;
                        }
                    });
                    Draw(drawPoints);
                }


            }
        }
    }

    private double getCos(Point p1, Point p2){
        double length = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));

        return (p1.x - p2.x) / length;
    }
}