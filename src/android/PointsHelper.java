package halloland.scan;

import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PointsHelper {

    public static List<Point> getEstimatedPoints(Mat mask, int treshHold) {
        List<Pair<Point, Point>> linePoints = PointsHelper.getLinesPoints(mask, treshHold);
        List<Point> intersections = PointsHelper.getIntersectionsFromLinePoints(linePoints);


        return PointsHelper.removePointsDuplications(intersections);
    }

    public static List<Point> orderPointsClockwise(List<Point> points) {

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


        return Arrays.asList(pts);
    }

    public static List<Point> cloneListPoints(List<Point> list) {
        List<Point> clone = new ArrayList<Point>(list.size());
        for (Point item : list) clone.add(item.clone());
        return clone;
    }

    public static List<Point> removePointsDuplications(List<Point> points) {
        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            for (int j = 0; j < points.size(); j++) {
                if (j != i) {
                    Point p2 = points.get(j);

                    double distance = GeometryTools.getLineLength(p1, p2);
                    if (distance < 10) {
                        points.remove(p2);

                        j--;
                    }

                }
            }
        }

        return points;
    }

    public static List<Point> getIntersectionsFromLinePoints(List<Pair<Point, Point>> linePoints) {
        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < linePoints.size(); i++) {
            Pair<Point, Point> point1 = linePoints.get(i);
            for (int j = 0; j < linePoints.size(); j++) {
                if (j != i) {
                    Pair<Point, Point> point2 = linePoints.get(j);
                    Point intersect = GeometryTools.getLinesIntersectionPoint(point1.first, point1.second, point2.first, point2.second);
                    if (intersect.x != Double.MAX_VALUE && intersect.y != Double.MAX_VALUE && !points.contains(intersect)) {
                        Point p1 = new Point(point1.second.x - point1.first.x, point1.second.y - point1.first.y);
                        Point p2 = new Point(point2.second.x - point2.first.x, point2.second.y - point2.first.y);
                        double cos = Math.abs(GeometryTools.getCos(p1, p2));


                        if (cos <= 0.2) {
                            points.add(intersect);
                        }
                    }

                }
            }
        }

        return points;
    }

    public static List<Pair<Point, Point>> getLinesPoints(Mat mask, int treshHold) {
        Mat lines = new Mat();
        List<Pair<Point, Point>> linePoints = new ArrayList<>();

        Imgproc.HoughLinesP(mask, lines, 1, Math.PI / 180, treshHold, 50, 10); // runs the actual detection

        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            double initialX = (l[2] - l[0]) * 100;
            double initialY = (l[3] - l[1]) * 100;

            double cos = Math.abs(GeometryTools.getCos(new Point(100, 0), new Point(l[2] - l[0], l[3] - l[1])));


            if (cos <= 0.2 || cos >= 0.90) {
                linePoints.add(new Pair<>(new Point(l[0] - initialX, l[1] - initialY), new Point(l[2] + initialX, l[3] + initialY)));
            }
        }

        return linePoints;
    }

}
