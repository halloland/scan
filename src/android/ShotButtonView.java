package halloland.scan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.Button;

public class ShotButtonView extends Button {
    int angle = 1;

    public ShotButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ValueAnimator va = ValueAnimator.ofInt(1, 360);
        int mDuration = 3000; //in millis
        va.setDuration(mDuration);

        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                angle = (int) animation.getAnimatedValue();
                invalidate();
            }
        });

        va.setStartDelay(0);
        va.setInterpolator(new LinearInterpolator());


        va.setRepeatCount(ValueAnimator.INFINITE);
        va.start();

    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#FFCC33"));
        paint.setStrokeWidth(10);
        canvas.drawCircle((this.getWidth()) / 2, (this.getHeight()) / 2, (this.getWidth() - 15) / 2, paint);

        final RectF oval = new RectF();
        oval.set((this.getWidth()) / 2 - 30, (this.getHeight()) / 2 - 30, (this.getWidth()) / 2 + 30, (this.getHeight()) / 2 + 30);

        Path myPath = new Path();
        myPath.arcTo(oval, angle, 240, false);

        final RectF oval2 = new RectF();
        oval2.set((this.getWidth()) / 2 - 50, (this.getHeight()) / 2 - 50, (this.getWidth()) / 2 + 50, (this.getHeight()) / 2 + 50);

        Path myPath2 = new Path();
        myPath2.arcTo(oval2, 360 - angle, 240, false);

        paint.setStrokeWidth(5);
        canvas.drawPath(myPath, paint);
        canvas.drawPath(myPath2, paint);
    }
}
