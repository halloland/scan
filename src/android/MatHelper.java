package halloland.scan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MatHelper {
    public static Mat applyFilters(Mat frame) {
        Mat mask = new Mat();
        Imgproc.medianBlur(frame, mask, 9);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2GRAY);

        Imgproc.adaptiveThreshold(mask, mask, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));

        Imgproc.erode(mask, mask, element);
        Imgproc.dilate(mask, mask, element);

        return mask;
    }

    public static Mat rotate(Mat frame) {
        Core.transpose(frame, frame);
        Core.flip(frame, frame, 1);

        return frame;
    }

    public static Mat getMatFromData(byte[] data, int width, int height, double ratio) {

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                width, height, null);

        Rect rectangle = new Rect(0, 0, width, height);
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();


        yuvImage.compressToJpeg(rectangle, 60, out);

        byte[] imageBytes = out.toByteArray();


        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        //double ratio = (double) width / 800;

        Bitmap resized = Bitmap.createScaledBitmap(image, (int) (width / ratio), (int) (height / ratio), true);

        Mat mat = new Mat(resized.getWidth(), resized.getHeight(), CvType.CV_8UC1);

        Bitmap bmp32 = resized.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);

        return mat;
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
