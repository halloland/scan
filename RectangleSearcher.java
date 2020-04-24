package halloland.scan;

import android.os.Build;
import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import androidx.annotation.RequiresApi;

public class RectangleSearcher {
    private double maxArea = 0;
    private Point[] bestRectangle;
    private double angle = 0.15;


    public Point[] getBiggestRectangleFromPoints(List<Point> arr, double angle) {
        this.angle = angle;
        return getBiggestRectangleFromPoints(arr);
    }

    public Point[] getBiggestRectangleFromPoints(List<Point> arr) {

        bestRectangle = null;
        maxArea = 0;
        Point[] data = new Point[4];
        int n = arr.size();


        combinationUtil(arr, data, 0, n - 1, 0, 4);
        angle = 0.15;

        return bestRectangle;
    }


    public static List<Point> sortRectPoints(List<Point> sourcePoints) {

        Point center = PointsHelper.centroid(sourcePoints);
        List<Point> newList = new ArrayList<>(sourcePoints);
        Collections.sort(newList, new Comparator<Point>() {
            @Override
            public int compare(Point a, Point b) {
                double a1 = (Math.toDegrees(Math.atan2(a.x - center.x, a.y - center.y)) + 360) % 360;
                double a2 = (Math.toDegrees(Math.atan2(b.x - center.x, b.y - center.y)) + 360) % 360;
                return (int) (a1 - a2);
            }
        });

        return newList;
    }

    private void combinationUtil(List<Point> arr, Point[] data, int start,
                                 int end, int index, int r) {
        if (index == r) {
            List<Point> comb = new ArrayList<>();
            for (int j = 0; j < r; j++) {
                comb.add(data[j]);
            }
            List<Point> sorted = sortRectPoints(comb);

            Point intersect = GeometryTools.getLinesIntersectionPoint(sorted.get(0), sorted.get(3), sorted.get(1), sorted.get(2));
            if (intersect.x == Double.MAX_VALUE || intersect.y == Double.MAX_VALUE || !isValidRectanle(sorted)) {
                return;
            }


            double contourArea = getAreaFromPoints(sorted);

            if (contourArea > maxArea) {
                maxArea = contourArea;

                bestRectangle = sorted.toArray(new Point[4]);
            }


            return;
        }

        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr.get(i);
            combinationUtil(arr, data, i + 1, end, index + 1, r);
        }
    }

    private boolean isValidRectanle(List<Point> points) {
        boolean allowed = true;


        double cos1 = Math.abs(GeometryTools.getCos(
                new Point(points.get(0).x - points.get(1).x, points.get(0).y - points.get(1).y),
                new Point(points.get(2).x - points.get(1).x, points.get(2).y - points.get(1).y)
        ));

        double cos2 = Math.abs(GeometryTools.getCos(
                new Point(points.get(1).x - points.get(2).x, points.get(1).y - points.get(2).y),
                new Point(points.get(2).x - points.get(3).x, points.get(2).y - points.get(3).y)
        ));

        double cos3 = Math.abs(GeometryTools.getCos(
                new Point(points.get(2).x - points.get(3).x, points.get(2).y - points.get(3).y),
                new Point(points.get(3).x - points.get(0).x, points.get(3).y - points.get(0).y)
        ));

        double cos4 = Math.abs(GeometryTools.getCos(
                new Point(points.get(3).x - points.get(0).x, points.get(3).y - points.get(0).y),
                new Point(points.get(0).x - points.get(1).x, points.get(0).y - points.get(1).y)
        ));


        if (cos1 > angle || cos2 > angle || cos3 > angle || cos4 > angle) {
            allowed = false;
        }


        return allowed;
    }


    private double getAreaFromPoints(List<Point> points){
        double length1 = GeometryTools.getLineLength(points.get(0), points.get(1));
        double length2 = GeometryTools.getLineLength(points.get(1), points.get(2));
        double length3 = GeometryTools.getLineLength(points.get(2), points.get(3));
        double length4 = GeometryTools.getLineLength(points.get(3), points.get(0));


        double length5 = GeometryTools.getLineLength(points.get(3), points.get(1));


        double p1 = (length1 + length5 + length4) / 2;
        double s1 = Math.sqrt(p1 * (p1 - length1) * (p1 - length5) * (p1 - length4));

        double p2 = (length2 + length5 + length3) / 2;
        double s2 = Math.sqrt(p2 * (p1 - length2) * (p2 - length5) * (p2 - length3));


        return (s1 + s2);

    }
}
