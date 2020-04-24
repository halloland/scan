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

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RectangleView extends View {

    int value = 1;

    List<Point> lastPoints = null;
    List<Point> currentPoints = null;
    List<Point> backupPoints = null;
    ValueAnimator moveAnimator = ValueAnimator.ofInt(1, 100);
    private boolean animating = false;


    public RectangleView(Context context, AttributeSet attrs) {
        super(context, attrs);



        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                if (backupPoints == null || lastPoints == null || currentPoints == null) {
                    return;
                }
                value = (int) animation.getAnimatedValue();

                for (int i = 0; i < backupPoints.size(); i++) {
                    Point backupPoint = backupPoints.get(i);
                    Point lastPoint = lastPoints.get(i);
                    Point currentPoint = currentPoints.get(i);

                    currentPoint.x = lastPoint.x - (lastPoint.x - backupPoint.x) * value / 100;
                    currentPoint.y = lastPoint.y - (lastPoint.y - backupPoint.y) * value / 100;
                }


                invalidate();
            }
        });
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
                if (currentPoints != null) {
                    lastPoints = PointsHelper.cloneListPoints(currentPoints);
                }

            }
        });
        moveAnimator.setDuration(200);

    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);
        //canvas.drawColor(Color.RED);

        if (this.currentPoints != null) {
            this.drawRect(this.currentPoints, canvas);
        }

    }

    public void clear() {
        this.lastPoints = null;
        this.currentPoints = null;
        this.backupPoints = null;
    }


    public void render(List<Point> points) {
        try {

            if (lastPoints != null && points != null) {
                Log.d("renderPoints", points.toString());

                if (!animating) {
                    animating = true;
                } else {
                    this.lastPoints = PointsHelper.cloneListPoints(this.currentPoints);
                }
                this.backupPoints = PointsHelper.orderPointsClockwise(new ArrayList<>(points));

                moveAnimator.start();
            } else {
                moveAnimator.end();
                if (points != null) {
                    this.currentPoints = PointsHelper.orderPointsClockwise(PointsHelper.cloneListPoints(points));
                    this.lastPoints = PointsHelper.cloneListPoints(this.currentPoints);
                } else {
                    this.currentPoints = null;
                    this.lastPoints = null;
                }

                invalidate();
            }

        } catch (Exception e) {
            Log.e("error_points", e.getMessage() + " " + Arrays.asList(e.getStackTrace()).toString());
        }


    }


    private void drawRect(List<Point> points, Canvas canvas) {


        if (points != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setStyle(Paint.Style.STROKE);

            paint.setColor(Color.parseColor("#FFCC33"));

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


}
