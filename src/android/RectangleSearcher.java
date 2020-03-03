package halloland.scan;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RectangleSearcher {
    private double maxArea = 0;
    private Point[] bestRectangle;


    public Point[] getBiggestRectangleFromPoints(List<Point> arr) {
        bestRectangle = null;
        maxArea = 0;
        Point[] data = new Point[4];
        int n = arr.size();

        combinationUtil(arr, data, 0, n - 1, 0, 4);

        return bestRectangle;
    }


    public static List<Point> sortRectPoints(List<Point> sourcePoints) {
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

    private void combinationUtil(List<Point> arr, Point[] data, int start,
                                 int end, int index, int r) {
        if (index == r) {
            List<Point> comb = new ArrayList<>();
            for (int j = 0; j < r; j++) {
                comb.add(data[j]);
            }

            List<Point> sorted = RectangleSearcher.sortRectPoints(comb);

            Point intersect = GeometryTools.getLinesIntersectionPoint(sorted.get(0), sorted.get(3), sorted.get(1), sorted.get(2));
            if (intersect.x == Double.MAX_VALUE || !isValidRectanle(sorted)) {
                return;
            }


            double lengthP1 = GeometryTools.getLineLength(sorted.get(0), sorted.get(3));
            double lengthP2 = GeometryTools.getLineLength(sorted.get(1), sorted.get(2));
            double ratio = lengthP1 / lengthP2;


            if (ratio > 0.6 && ratio < 1.4) {
                Point[] foundPoints = new Point[4];
                foundPoints[0] = sorted.get(0);
                foundPoints[1] = sorted.get(1);
                foundPoints[2] = sorted.get(2);
                foundPoints[3] = sorted.get(3);


                foundPoints = (Point[]) sorted.toArray();
                double averageLenth1 = (GeometryTools.getLineLength(foundPoints[0], foundPoints[1]) + GeometryTools.getLineLength(foundPoints[2], foundPoints[3])) / 2;
                double averageLenth2 = (GeometryTools.getLineLength(foundPoints[0], foundPoints[2]) + GeometryTools.getLineLength(foundPoints[1], foundPoints[3])) / 2;

                double contourArea = averageLenth1 * averageLenth2/*Imgproc.contourArea(new MatOfPoint(foundPoints))*/;

                if (contourArea > maxArea) {
                    maxArea = contourArea;

                    bestRectangle = foundPoints;
                }
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
                new Point(points.get(2).x - points.get(0).x, points.get(2).y - points.get(0).y),
                new Point(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y)
        ));

        double cos2 = Math.abs(GeometryTools.getCos(
                new Point(points.get(3).x - points.get(2).x, points.get(3).y - points.get(2).y),
                new Point(points.get(3).x - points.get(1).x, points.get(3).y - points.get(1).y)
        ));

        double cos3 = Math.abs(GeometryTools.getCos(
                new Point(points.get(3).x - points.get(1).x, points.get(3).y - points.get(1).y),
                new Point(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y)
        ));

        double cos4 = Math.abs(GeometryTools.getCos(
                new Point(points.get(3).x - points.get(2).x, points.get(3).y - points.get(2).y),
                new Point(points.get(2).x - points.get(0).x, points.get(2).y - points.get(0).y)
        ));


        if (cos1 > 0.2 || cos2 > 0.2 || cos3 > 0.2 || cos4 > 0.2) {
            allowed = false;
        }


        return allowed;
    }
}
