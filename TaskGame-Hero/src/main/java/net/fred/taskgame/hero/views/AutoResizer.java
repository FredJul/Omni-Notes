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

    // Text view line spacing multiplier
    private float mSpacingMult = 1.0f;

    // Text view additional line spacing
    private float mSpacingAdd = 0.0f;

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

        // Bisection method: fast & precise
        float lower = mMinTextSize;
        float upper = mMaxTextSize;
        int loopCounter = 1;
        float targetTextSize;
        int textHeight;

        while (loopCounter < BISECTION_LOOP_WATCH_DOG && upper - lower > 1) {
            targetTextSize = (lower + upper) / 2;
            textHeight = getTextHeight(newText, textPaint, widthLimit, targetTextSize);
            if (textHeight > heightLimit) {
                upper = targetTextSize;
            } else {
                lower = targetTextSize;
            }
            loopCounter++;
        }

        targetTextSize = lower;

        // Some devices try to auto adjust line spacing, so force default line spacing
        // and invalidate the layout as a side effect
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize);
    }

    /**
     * Override the set line spacing to update our internal reference values
     */
    public void setLineSpacing(float add, float mult) {
        mSpacingMult = mult;
        mSpacingAdd = add;
        resizeText();
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

    // Set the text size of the text paint object and use a static layout to render text off screen before measuring
    private int getTextHeight(CharSequence source, TextPaint originalPaint, int width, float textSize) {
        // modified: make a copy of the original TextPaint object for measuring
        // (apparently the object gets modified while measuring, see also the
        // docs for TextView.getPaint() (which states to access it read-only)
        TextPaint paint = new TextPaint(originalPaint);
        // Update the text paint object
        paint.setTextSize(textSize);
        // Measure using a static layout
        StaticLayout layout = new StaticLayout(source, paint, width, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, true);
        return layout.getHeight();
    }

}
