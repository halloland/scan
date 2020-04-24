package halloland.scan;

import org.opencv.core.Point;

public class GeometryTools {
    static double getLineLength(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    static Point getLinesIntersectionPoint(Point A, Point B, Point C, Point D) {
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

    public static double getCos(Point p1, Point p2) {
        double dotProd = p1.x * p2.x + p1.y * p2.y;
        double length1 = Math.sqrt(Math.pow(p1.x, 2) + Math.pow(p1.y, 2));
        double length2 = Math.sqrt(Math.pow(p2.x, 2) + Math.pow(p2.y, 2));

        return dotProd / (length1 * length2);
    }


    public static double getCos2(Point p1, Point p2){
        double length1 = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));

        return ((p1.x - p2.x) / length1);
    }
}
