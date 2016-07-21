package net.fred.taskgame.hero.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class AutoResizeTextView extends TextView {

    private final AutoResizer mResizer = new AutoResizer(this);

    // Default constructor override
    public AutoResizeTextView(Context context) {
        this(context, null);
    }

    // Default constructor when inflating from XML file
    public AutoResizeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        if (attrs != null) {
            mResizer.initAttrs(context, attrs);
        }
    }

    // Default constructor override
    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            mResizer.initAttrs(context, attrs);
        }
    }

    /**
     * When text changes, set the force resize flag to true and reset the text size.
     */
    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        if (mResizer != null) {
            mResizer.resizeText();
        }
    }

    /**
     * If the text view size changed, set the force resize flag to true
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            mResizer.resizeText();
        }
    }

    /**
     * Override the set line spacing to update our internal reference values
     */
    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        mResizer.resizeText();
    }

    /**
     * Set the upper text size limit and invalidate the view
     *
     * @param maxTextSize
     */
    public void setMaxTextSize(float maxTextSize) {
        mResizer.setMaxTextSize(maxTextSize);
    }

    /**
     * Return upper text size limit
     *
     * @return
     */
    public float getMaxTextSize() {
        return mResizer.getMaxTextSize();
    }

    /**
     * Set the lower text size limit and invalidate the view
     *
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        mResizer.setMinTextSize(minTextSize);
    }

    /**
     * Return lower text size limit
     *
     * @return
     */
    public float getMinTextSize() {
        return mResizer.getMinTextSize();
    }

}