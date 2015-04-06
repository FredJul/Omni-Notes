package it.feio.android.checklistview;

import android.content.Context;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.regex.Pattern;

import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.checklistview.interfaces.Constants;
import it.feio.android.checklistview.models.CheckListView;
import it.feio.android.checklistview.models.CheckListViewItem;

public class ChecklistManager {

    private static ChecklistManager instance = null;
    private final Context mContext;
    private TextWatcher mTextWatcher;
    private CheckListChangedListener mCheckListChangedListener;
    private CheckListView mCheckListView;


    private ChecklistManager(Context mContext) {
        this.mContext = mContext;
    }


    public static synchronized ChecklistManager getInstance(Context mContext) {
        if (instance == null) {
            instance = new ChecklistManager(mContext);
        }
        return instance;
    }


    /**
     * Set if keep or remove checked items when converting back from checklist to simple text. Default false.
     *
     * @param keepChecked True to keep checks, false otherwise.
     */
    public void setKeepChecked(boolean keepChecked) {
        App.getSettings().setKeepChecked(keepChecked);
    }


    /**
     * Set if show checked or unchecked sequence symbols when converting back from checklist to simple text. Default
     * false.
     */
    public void setShowChecks(boolean showChecks) {
        App.getSettings().setShowChecks(showChecks);
    }


    /**
     * If set to true when an item is checked it is moved on bottom of the list
     *
     * @param moveCheckedOnBottom
     */
    public void setMoveCheckedOnBottom(int moveCheckedOnBottom) {
        App.getSettings().setMoveCheckedOnBottom(moveCheckedOnBottom);
    }


    /**
     * Set if an empty line on bottom of the checklist must be shown or not
     *
     * @param showHintItem
     */
    public void setShowHintItem(boolean showHintItem) {
        App.getSettings().setShowHintItem(showHintItem);
    }


    /**
     * Adds a new fillable line at the end of the checklist with hint text. Set an empty string to remove.
     *
     * @param newEntryHint Hint text
     */
    public void setNewEntryHint(String newEntryHint) {
        setShowHintItem(true);
        App.getSettings().setNewEntryHint(newEntryHint);
    }


    public void setDragVibrationEnabled(boolean dragVibrationEnabled) {
        App.getSettings().setDragVibrationEnabled(dragVibrationEnabled);
    }


    public View convert(View v) {
        if (EditText.class.isAssignableFrom(v.getClass())) {
            return convert((EditText) v);
        } else if (LinearLayout.class.isAssignableFrom(v.getClass())) {
            return convert((CheckListView) v);
        } else {
            return null;
        }
    }


    /**
     * Conversion from EditText to checklist
     *
     * @param v EditText view
     * @return converted view to replace
     */
    private View convert(EditText v) {
        mCheckListView = new CheckListView(mContext);
        mCheckListView.setMoveCheckedOnBottom(App.getSettings().getMoveCheckedOnBottom());
        mCheckListView.setShowDeleteIcon(App.getSettings().getShowDeleteIcon());
        mCheckListView.setNewEntryHint(App.getSettings().getNewEntryHint());
        mCheckListView.setId(v.getId());

        // Listener for general event is propagated on bottom
        if (mCheckListChangedListener != null) {
            mCheckListView.setCheckListChangedListener(mCheckListChangedListener);
        }

        String text = v.getText().toString();

        // Parse all lines if text is not empty
        convertToChecklist(text);

        // Add new fillable line if newEntryText has some text value or showHintItem is set to true
        if (App.getSettings().getShowHintItem()) {
            mCheckListView.addHintItem();
        }

        mCheckListView.cloneStyles(v);

        return mCheckListView;
    }


    private void convertToChecklist(String text) {
        if (text.length() == 0) {
            return;
        }
        for (String line : text.split(Pattern.quote(App.getSettings().getLinesSeparator()))) {
            convertLineToChecklist(line);
        }
    }


    private void convertLineToChecklist(String line) {
        if (line.length() == 0) {
            return;
        }
        // Line text content will be now stripped from checks symbols if they're present
        // (ex. [x] Task done -> lineText="Task done", lineChecked=true)
        boolean isChecked = line.indexOf(Constants.CHECKED_SYM) == 0;
        String lineText = line.replace(Constants.CHECKED_SYM, "").replace(Constants.UNCHECKED_SYM, "");
        mCheckListView.addItem(lineText, isChecked);
    }


    /**
     * Conversion from checklist view to EditText
     *
     * @param v CheckListView to be re-converted
     * @return EditText
     */
    private View convert(CheckListView v) {
        EditText returnView = new EditText(mContext);

        StringBuilder sb = new StringBuilder();
        removeChecked(v, sb);

        returnView.setText(sb.toString());
        returnView.setId(v.getId());

            returnView.setBackground(v.getBackground());

        restoreTypography(v, returnView);

        // Associating textChangedListener
        if (this.mTextWatcher != null) {
            returnView.addTextChangedListener(this.mTextWatcher);
        }

        // Reset to null the field
        mCheckListView = null;

        return returnView;
    }

    private void restoreTypography(CheckListView v, EditText returnView) {
        if (v.getEditText() != null) {
            returnView.setTypeface(v.getEditText().getTypeface());
            returnView.setTextSize(0, v.getEditText().getTextSize());
            returnView.setTextColor(v.getEditText().getTextColors());
            returnView.setLinkTextColor(v.getEditText().getLinkTextColors());
        }
    }

    private void removeChecked(CheckListView v, StringBuilder sb) {
        boolean isChecked;
        for (int i = 0; i < v.getChildCount(); i++) {
            CheckListViewItem mCheckListViewItem = v.getChildAt(i);
            if (mCheckListViewItem.isHintItem()) {
                continue;
            }
            // If item is checked it will be removed if requested
            isChecked = mCheckListViewItem.isChecked();
            if (!isChecked || (isChecked && App.getSettings().getKeepChecked())) {
                sb.append(i > 0 ? App.getSettings().getLinesSeparator() : "")
                        .append(App.getSettings().getShowChecks() ? isChecked ? Constants.CHECKED_SYM
                                : Constants.UNCHECKED_SYM : "").append(mCheckListViewItem.getText());
            }
        }
    }


    /**
     * Replace a view with another
     */
    public void replaceViews(View oldView, View newView) {
        if (oldView == null || newView == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) oldView.getParent();
        int index = parent.indexOfChild(oldView);
        parent.removeView(oldView);
        parent.addView(newView, index);
    }


    public void setCheckListChangedListener(CheckListChangedListener mCheckListChangedListener) {
        this.mCheckListChangedListener = mCheckListChangedListener;
    }


    public void addTextChangedListener(TextWatcher mTextWatcher) {
        this.mTextWatcher = mTextWatcher;
    }


    public String getText() {
        if (mCheckListView == null) {
            return "";
        }

        StringBuilder stringbuilder = new StringBuilder();
        int i = 0;
        do {
            CheckListViewItem checklistviewitem;
            if (i >= mCheckListView.getChildCount()) {
                if (stringbuilder.length() > App.getSettings().getLinesSeparator().length()) {
                    return stringbuilder.substring(App.getSettings().getLinesSeparator().length());
                } else {
                    return "";
                }
            }
            checklistviewitem = mCheckListView.getChildAt(i);
            if (!checklistviewitem.isHintItem()) {
                boolean flag = checklistviewitem.isChecked();
                if (!flag || flag && App.getSettings().getKeepChecked()) {
                    stringbuilder.append(App.getSettings().getLinesSeparator());
                    String s;
                    if (App.getSettings().getShowChecks()) {
                        if (flag) {
                            s = "[x] ";
                        } else {
                            s = "[ ] ";
                        }
                    } else {
                        s = "";
                    }
                    stringbuilder.append(s).append(checklistviewitem.getText());
                }
            }
            i++;
        } while (true);
    }


    /**
     * Counts the number of checked items in the list
     */
    public int getCheckedCount() {
        if (mCheckListView == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < mCheckListView.getChildCount(); i++) {
            CheckListViewItem mCheckListViewItem = mCheckListView.getChildAt(i);
            if (!mCheckListViewItem.isHintItem() && mCheckListViewItem.isChecked()) {
                count++;
            }
        }
        return count;
    }


    /**
     * Counts the number of items excluding the hint item
     */
    public int getCount() {
        if (mCheckListView == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < mCheckListView.getChildCount(); i++) {
            if (!mCheckListView.getChildAt(i).isHintItem()) {
                count++;
            }
        }
        return count;
    }
}
