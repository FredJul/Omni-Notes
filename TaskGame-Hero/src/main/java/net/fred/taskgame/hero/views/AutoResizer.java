package net.fred.taskgame.hero.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import net.fred.taskgame.hero.R;

public class AutoResizer {

    // Minimum text size for this text view
    public static final float MIN_TEXT_SIZE = 25;

    // Minimum text size for this text view
    public static final float MAX_TEXT_SIZE = 128;

    private static final int BISECTION_LOOP_WATCH_DOG = 30;

    private final TextView mTextView;

    // Temporary upper bounds on the starting text size
    private float mMaxTextSize = MAX_TEXT_SIZE;

    // Lower bounds for text size
    private float mMinTextSize = MIN_TEXT_SIZE;

    public AutoResizer(TextView textView) {
        mTextView = textView;
    }

    public void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoResizeTextView);
        mMaxTextSize = a.getDimension(R.styleable.AutoResizeTextView_maxTextSize, MAX_TEXT_SIZE);
        mMinTextSize = a.getDimension(R.styleable.AutoResizeTextView_minTextSize, MIN_TEXT_SIZE);
        a.recycle();
    }

    /**
     * Resize the text size with default width and height
     */
    public void resizeText() {
        int heightLimit = mTextView.getHeight() - mTextView.getPaddingBottom() - mTextView.getPaddingTop();
        int widthLimit = mTextView.getWidth() - mTextView.getPaddingLeft() - mTextView.getPaddingRight();

        CharSequence newText = mTextView.getText();

        // Do not resize if the view does not have dimensions or there is no text
        if (newText == null || newText.length() == 0 || heightLimit <= 0 || widthLimit <= 0) {
            return;
        }

        // Get the text view's paint object
        TextPaint textPaint = mTextView.getPaint();
        float originalPaintTextSize = textPaint.getTextSize();

        // Bisection method: fast & precise
        float lower = mMinTextSize;
        float upper = mMaxTextSize;
        int loopCounter = 1;
        float targetTextSize;
        int textHeight;

        while (loopCounter < BISECTION_LOOP_WATCH_DOG && upper - lower > 1) {
            targetTextSize = (lower + upper) / 2;

            // Update the text paint object
            textPaint.setTextSize(targetTextSize);
            // Measure using a static layout
            StaticLayout layout = new StaticLayout(newText, textPaint, widthLimit, Alignment.ALIGN_NORMAL, mTextView.getLineSpacingMultiplier(), mTextView.getLineSpacingExtra(), true);
            textHeight = layout.getHeight();

            if (textHeight > heightLimit) {
                upper = targetTextSize;
            } else {
                lower = targetTextSize;
            }
            loopCounter++;
        }

        textPaint.setTextSize(originalPaintTextSize); // need to restore the initial one to avoid graphical issues

        targetTextSize = lower;

        // Some devices try to auto adjust line spacing, so force default line spacing
        // and invalidate the layout as a side effect
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize);
    }

    /**
     * Set the upper text size limit and invalidate the view in PX
     *
     * @param maxTextSize
     */
    public void setMaxTextSize(float maxTextSize) {
        mMaxTextSize = maxTextSize;
        resizeText();
    }

    /**
     * Return upper text size limit in PX
     *
     * @return
     */
    public float getMaxTextSize() {
        return mMaxTextSize;
    }

    /**
     * Set the lower text size limit and invalidate the view in PX
     *
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        mMinTextSize = minTextSize;
        resizeText();
    }

    /**
     * Return lower text size limit in PX
     *
     * @return
     */
    public float getMinTextSize() {
        return mMinTextSize;
    }

}