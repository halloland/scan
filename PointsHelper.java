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

    public static Point centroid(List<Point> knots)  {
        double centroidX = 0, centroidY = 0;

        for(Point knot : knots) {
            centroidX += knot.x;
            centroidY += knot.y;
        }
        return new Point(centroidX / knots.size(), centroidY / knots.size());
    }

    public static List<Point> sort(List<Point> points){
        Point center = centroid(points);

        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {

                return comparePoints(p1, p2, center);
            }
        });

        return points;
    }


    public static int comparePoints(Point a, Point b, Point center)
    {
        if (a.x - center.x >= 0 && b.x - center.x < 0)
            return 1;
        if (a.x - center.x < 0 && b.x - center.x >= 0)
            return -1;
        if (a.x - center.x == 0 && b.x - center.x == 0) {
            if (a.y - center.y >= 0 || b.y - center.y >= 0){
                if(a.y > b.y){
                    return 1;
                } else {
                    return -1;
                }
            }
            if(b.y > a.y){
                return 1;
            } else {
                return -1;
            }

        }

        // compute the cross product of vectors (center -> a) x (center -> b)
        double det = (a.x - center.x) * (b.y - center.y) - (b.x - center.x) * (a.y - center.y);
        if (det < 0)
            return 1;
        if (det > 0)
            return -1;

        // points a and b are on the same line from the center
        // check which point is closer to the center
        double d1 = (a.x - center.x) * (a.x - center.x) + (a.y - center.y) * (a.y - center.y);
        double d2 = (b.x - center.x) * (b.x - center.x) + (b.y - center.y) * (b.y - center.y);
        if(d1 > d2){
            return 1;
        } else {
            return -1;
        }
    }

    public static List<Point> removePointsDuplications(List<Point> points, int width, int height) {
        List<Point> newList = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            boolean canAdd = true;
            for (int j = 0; j < newList.size(); j++) {
                Point p2 = newList.get(j);

                double distance = GeometryTools.getLineLength(p1, p2);
                if (distance < 10 || p1.y < 20 || p1.x < 20 || p1.y + 20 > height || p1.x + 20 > width) {
                    canAdd = false;
                }
            }
            if(canAdd){
                newList.add(p1);
            }
        }

        return newList;
    }

}
