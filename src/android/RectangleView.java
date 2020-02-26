package halloland.scan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RectangleView extends View {

    int value = 1;

    boolean empty = false;
    List<Point> lastPoints= null;
    List<Point> currentPoints = null;
    List<Point> backupPoints = null;
    ValueAnimator moveAnimator = ValueAnimator.ofInt(1, 100);
    private boolean animating = false;


    public RectangleView(Context context, AttributeSet attrs) {
        super(context, attrs);


        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                if(backupPoints == null || lastPoints == null || currentPoints == null) {
                    return;
                }
                value = (int) animation.getAnimatedValue();
                Log.d("lastpointsasdasd", String.valueOf(value));
                for (int i = 0; i< backupPoints.size(); i++){
                    Point backupPoint = backupPoints.get(i);
                    Point lastPoint = lastPoints.get(i);
                    Point currentPoint = currentPoints.get(i);

                    currentPoint.x = lastPoint.x - (lastPoint.x - backupPoint.x) * value / 100;
                    currentPoint.y = lastPoint.y - (lastPoint.y - backupPoint.y) * value / 100;
                }





                invalidate();
            }
        });
        moveAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                animating = false;
                if(currentPoints != null){
                    lastPoints = cloneList(currentPoints);
                }

                Log.d("lastpoints_stoooop", "yes");
            }
        });
        moveAnimator.setDuration(200);

    }

    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


        if(this.currentPoints != null){
            this.drawRect(this.currentPoints, canvas);
        }

    }

    public void clear(){
        this.lastPoints = null;
        this.currentPoints = null;
        this.backupPoints = null;
    }


    public void render(List<Point> points){
        Log.d("pointsaasdasd", String.valueOf(points));


        try {

        if(lastPoints != null && points != null){
            if(!animating){
                animating = true;
            } else {
                this.lastPoints = cloneList(this.currentPoints);
            }
            this.backupPoints = orderPointsClockwise(new ArrayList<>(points));

            moveAnimator.start();
        } else {
            Log.d("lastpoints","nothing");
            /*if(currentPoints == null && lastPoints == null){
                return;
            }*/
            moveAnimator.end();
            if(points != null){
                this.currentPoints = orderPointsClockwise(cloneList(points));
                this.lastPoints = this.cloneList(this.currentPoints);
            } else {
                this.currentPoints = null;
                this.lastPoints = null;
            }

            invalidate();
        }

        } catch (Exception e){
            Log.e("error_points", e.getMessage() + " " + Arrays.asList(e.getStackTrace()).toString());
        }


    }

    public List<Point> cloneList(List<Point> list) {
        List<Point> clone = new ArrayList<Point>(list.size());
        for (Point item : list) clone.add(item.clone());
        return clone;
    }

    private void drawRect(List<Point> points, Canvas canvas) {


        if (points != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStyle(Paint.Style.STROKE);

            paint.setColor(Color.GREEN);

            paint.setStrokeWidth(5);

            Point firstPoint = points.get(0);

            Point previousPoint = firstPoint;
            for (int i = 1; i < points.size(); i++) {
                Point point = points.get(i);

                canvas.drawLine((float) previousPoint.x, (float) previousPoint.y, (float) point.x, (float) point.y, paint);

                previousPoint = point;
            }
            canvas.drawLine((float) previousPoint.x, (float) previousPoint.y,
                    (float) firstPoint.x, (float) firstPoint.y, paint);
        }



    }

    List<Point> orderPointsClockwise(List<Point> points) {


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


        return Arrays.asList(pts);
    }


}
