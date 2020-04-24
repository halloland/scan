package halloland.scan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MatHelper {
    public static Mat applyFilters(Mat frame, int threshold) {
        Mat mask = new Mat();

        Mat blured = new Mat();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
        Imgproc.medianBlur(frame, blured, 3);
        Imgproc.medianBlur(blured, blured, 5);
        //Imgproc.GaussianBlur(frame, blured, new Size(15, 15), 15);
        //Imgproc.blur(frame, blured, new Size(3,3));
        //Imgproc.blur(frame, blured, new Size(7,10));

        Imgproc.cvtColor(blured, mask, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.medianBlur(mask, mask, 7);



        List<Mat> sources = new ArrayList<Mat>();
        sources.add(blured);
        List<Mat> destinations = new ArrayList<Mat>();
        destinations.add(mask);

        int[] ch = {1, 0};
        MatOfInt fromTo = new MatOfInt(ch);

        Core.mixChannels(sources, destinations, fromTo);

//        Imgproc.adaptiveThreshold(mask, mask, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
      /*  Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
       */
//        Imgproc.erode(mask, mask, element);
//
        Imgproc.Canny(mask, mask, threshold, threshold * 3, 3);
        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
        Imgproc.dilate(mask, mask, element2);


        //Imgproc.Canny(mask, mask, 50, 150, 3);
       /*



*/
        return mask;
    }

    public static Mat rotate(Mat frame) {
        Core.transpose(frame, frame);
        Core.flip(frame, frame, 1);

        return frame;
    }

    public static Mat getMatFromData(byte[] data, int width, int height, double ratio) {
        long currentTime = System.currentTimeMillis();



        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                width, height, null);

        Rect rectangle = new Rect(0, 0, width, height);
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();


        yuvImage.compressToJpeg(rectangle, 60, out);
      //  Log.d("timeSpend", "create jpeg : " + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();
        byte[] imageBytes = out.toByteArray();
        Mat frame = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_GRAYSCALE);
     //   Log.d("timeSpend", "create frame : " + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();
        Imgproc.resize(frame, frame, new Size(width / ratio, height / ratio),0 ,0, Imgproc.INTER_AREA);
     //   Log.d("timeSpend", "resize frame : " + (System.currentTimeMillis() - currentTime));
        return frame;
    }

    public static org.opencv.core.Size calculateFrameSizeFromPoints(List<Point> points, double ratio) {
        org.opencv.core.Point[] sortedPoints = (org.opencv.core.Point[]) points.toArray();
        double maxY = 0;
        double maxX = 0;
        double widthTop = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[1].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[1].y, 2));
        double widthBottom = Math.sqrt(Math.pow(sortedPoints[2].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[2].y - sortedPoints[3].y, 2));
        double heightLeft = Math.sqrt(Math.pow(sortedPoints[0].x - sortedPoints[2].x, 2) + Math.pow(sortedPoints[0].y - sortedPoints[2].y, 2));
        double heightRight = Math.sqrt(Math.pow(sortedPoints[1].x - sortedPoints[3].x, 2) + Math.pow(sortedPoints[1].y - sortedPoints[3].y, 2));

        maxX = (widthBottom + widthTop) / 2;
        maxY = (heightLeft + heightRight) / 2;

        return new org.opencv.core.Size(maxX * ratio, maxY * ratio);
    }


}
