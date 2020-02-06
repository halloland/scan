package halloland.scan;


import android.animation.ValueAnimator;
import android.app.Activity;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.view.View;

import android.hardware.Camera;
import android.hardware.Camera.Size;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
    private int treshHold2 = 130;


    public int getResourceId(String name, String defType) {
        return getResources().getIdentifier(name, defType, this.getPackageName());
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);




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

        final SeekBar seekBar2 = (SeekBar)findViewById(getResourceId("seekBar1", "id"));
        seekBar2.setOnSeekBarChangeListener(this);




    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getId() == getResourceId("seekBar", "id")){
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
        float aspect = (float) previewSize.width / previewSize.height;

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

            //camera.takePicture(null, null, null, this);
            camera.autoFocus(this);
        }
    }

    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
    {
        // сохраняем полученные jpg в папке /sdcard/CameraExample/
        // имя файла - System.currentTimeMillis()

        try
        {
            File saveDir = new File("/sdcard/CameraExample/");

            if (!saveDir.exists())
            {
                saveDir.mkdirs();
            }

            FileOutputStream os = new FileOutputStream(String.format("/sdcard/CameraExample/%d.jpg", System.currentTimeMillis()));
            os.write(paramArrayOfByte);
            os.close();
        }
        catch (Exception e)
        {
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
            paramCamera.takePicture(null, null, null, this);
        }
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





        Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);


        Mat gray = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        Core.transpose(frame, frame);
        Core.flip(frame, frame, 1);

        Core.transpose(gray, gray);
        Core.flip(gray, gray, 1);
       // org.opencv.core.Core.flip(frame, frame, -1);
        //org.opencv.core.Core.flip(gray, gray, -1);



        Imgproc.blur(gray, frame, new org.opencv.core.Size(5, 5));



        //Imgproc.threshold(mRgba, mRgba, 125, 200, Imgproc.THRESH_BINARY);
       // Log.d("treshhold", String.valueOf(treshHold1) + " " + String.valueOf(treshHold2));

        Imgproc.Canny(frame, frame, treshHold1, treshHold2, 3, false);


        Imgproc.dilate(frame, frame, new Mat(), new Point(-1, 1), 1);


        Bitmap bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bmp);


        drawOpencv(bmp);

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
            MatOfPoint2f shape = null;

            for (MatOfPoint contour : contours) {
                RotatedRect boundingRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

                double rectangleArea = boundingRect.size.area();

                // test min ROI area in pixels
                if (rectangleArea > 100) {
                    Point rotated_rect_points[] = new Point[4];
                    boundingRect.points(rotated_rect_points);
                    org.opencv.core.Rect rect = Imgproc.boundingRect(new MatOfPoint(rotated_rect_points));


                    double contourArea = Imgproc.contourArea(contour);

                    MatOfPoint2f approx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) * 0.02, true);
                    long count = approx.total();

                    Log.d("countsies", String.valueOf(count));

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



            if(largestRect != null && shape != null){



                if(shape.total() <= 8 && shape.total() > 3){
                    List<Point> points = shape.toList();

                    Collections.sort(points, new Comparator<Point>() {
                        @Override
                        public int compare(Point o1, Point o2) {
                            if(o1.x < o2.x && o1.y < o2.y){
                                return 1;
                            }
                            return 0;
                        }
                    });
                    Draw(points);
                }
            }
        }
    }
}