
package com.example.gymtrackerpro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;

public class GoldShineDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix shaderMatrix = new Matrix();

    private final float cornerRadius;
    private final float strokeWidth;

    private float rotationDegrees;

    public GoldShineDrawable(Context context) {

        float density = context.getResources()
                .getDisplayMetrics().density;

        cornerRadius = 16f * density;
        strokeWidth = 1.8f * density;

        fillPaint.setColor(Color.parseColor("#17243A"));
        fillPaint.setStyle(Paint.Style.FILL);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);

        int[] goldColors = {
                Color.parseColor("#805515"),
                Color.parseColor("#C99A36"),
                Color.parseColor("#F5D77A"),
                Color.parseColor("#FFF4C2"),
                Color.parseColor("#F5D77A"),
                Color.parseColor("#C99A36"),
                Color.parseColor("#805515"),
                Color.parseColor("#C99A36")
        };

        float[] positions = {
                0f, 0.18f, 0.34f, 0.40f,
                0.47f, 0.68f, 0.84f, 1f
        };

        borderPaint.setShader(
                new SweepGradient(
                        0f,
                        0f,
                        goldColors,
                        positions
                )
        );
    }

    public void setRotationDegrees(float degrees) {
        rotationDegrees = degrees;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);

        rect.set(
                bounds.left + strokeWidth / 2f,
                bounds.top + strokeWidth / 2f,
                bounds.right - strokeWidth / 2f,
                bounds.bottom - strokeWidth / 2f
        );

        updateShader();
    }

    private void updateShader() {

        if (rect.isEmpty()) {
            return;
        }

        float centerX = rect.centerX();
        float centerY = rect.centerY();

        shaderMatrix.setRotate(
                rotationDegrees,
                centerX,
                centerY
        );

        Shader shader = borderPaint.getShader();

        if (shader != null) {
            shader.setLocalMatrix(shaderMatrix);
        }
    }

    @Override
    public void draw(Canvas canvas) {

        if (rect.isEmpty()) {
            return;
        }

        canvas.drawRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                fillPaint
        );

        canvas.drawRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                borderPaint
        );
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        borderPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        borderPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}