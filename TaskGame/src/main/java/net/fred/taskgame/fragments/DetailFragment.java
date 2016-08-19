/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.taskgame.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.activities.CategoryActivity;
import net.fred.taskgame.activities.MainActivity;
import net.fred.taskgame.adapters.CategoryAdapter;
import net.fred.taskgame.listeners.OnReminderPickedListener;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.KeyboardUtils;
import net.fred.taskgame.utils.LoaderUtils;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.utils.date.DateHelper;
import net.fred.taskgame.utils.date.ReminderPickers;

import org.parceler.Parcels;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.feio.android.checklistview.Settings;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.checklistview.models.ChecklistManager;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.Subscriber;


public class DetailFragment extends Fragment implements OnReminderPickedListener, TextWatcher, CheckListChangedListener {

    private static final int CATEGORY_CHANGE = 1;

    @BindView(R.id.detail_title)
    EditText mTitleEditText;
    @BindView(R.id.category_marker)
    View mCategoryMarker;
    @BindView(R.id.detail_content)
    EditText mContentEditText;
    @BindView(R.id.reward_layout)
    View mRewardLayout;
    @BindView(R.id.reward_points)
    TextView mRewardPoints;
    @BindView(R.id.reward_spinner)
    Spinner mRewardSpinner;
    @BindView(R.id.content_wrapper)
    ScrollView mScrollView;
    @BindView(R.id.switch_checkviews)
    Button mSwitchCheckViewsButton;
    @BindView(R.id.reminder_date)
    Button mReminderDateButton;
    @BindView(R.id.creation_date)
    TextView mCreationDateTextView;

    private Task mTask;
    private Task mOriginalTask;
    private ChecklistManager mChecklistManager;
    // Values to print result
    private String mExitMessage;
    private UiUtils.MessageType mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
    private int mContentLineCounter = 1;
    private View mToggleChecklistView;
    private FloatingActionButton mFab;

    public static DetailFragment newInstance(Task task) {
        DetailFragment fragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Restored temp note after orientation change
        if (savedInstanceState != null) {
            mTask = Parcels.unwrap(savedInstanceState.getParcelable("mTask"));
            mOriginalTask = Parcels.unwrap(savedInstanceState.getParcelable("mOriginalTask"));
        }

        // Handling of Intent actions
        handleIntents();

        if (mOriginalTask == null) {
            mOriginalTask = Parcels.unwrap(getArguments().getParcelable(Constants.INTENT_TASK));
        }

        if (mTask == null) {
            mTask = new Task(mOriginalTask);
        }

        if (mTask.alarmDate != 0) {
            mReminderDateButton.setText(getString(R.string.alarm_set_on, DateHelper.getDateTimeShort(getContext(), mTask.alarmDate)));
        }

            getMainActivity().getSupportActionBar().setTitle(R.string.task);

        if (mTask.isFinished) {
            getMainActivity().getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.finished_tasks_actionbar_color)));
        } else {
            getMainActivity().getSupportActionBar().setBackgroundDrawable(null);
        }

        initViews();

        mFab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        mFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);

                mFab.setImageResource(R.drawable.ic_done_white_24dp);
                mFab.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveAndExit();
                    }
                });
                mFab.setOnLongClickListener(null);
                mFab.show();
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mTask.title = mTitleEditText.getText().toString();
        mTask.content = getTaskContent();
        outState.putParcelable("mTask", Parcels.wrap(mTask));
        outState.putParcelable("mOriginalTask", Parcels.wrap(mOriginalTask));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Closes keyboard on exit
        if (mToggleChecklistView != null) {
            KeyboardUtils.hideKeyboard(mToggleChecklistView);
            mContentEditText.clearFocus();
        }
    }

    private void handleIntents() {
        Intent i = getActivity().getIntent();

        // Action called from home shortcut
        if (Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())) {
            mOriginalTask = DbUtils.getTask(i.getStringExtra(Constants.INTENT_TASK_ID));
            // Checks if the note pointed from the shortcut has been deleted
            if (mOriginalTask == null) {
                UiUtils.showMessage(getActivity(), R.string.shortcut_task_deleted);
                getActivity().finish();
            } else {
                mTask = new Task(mOriginalTask);
            }
            i.setAction(null);
        }

        // Check if is launched from a widget
        if (Constants.ACTION_WIDGET.equals(i.getAction())) {

            //  with tags to set tag
            if (i.hasExtra(Constants.INTENT_WIDGET)) {
                String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
                if (widgetId != null) {
                    String categoryId = PrefUtils.getString(PrefUtils.PREF_WIDGET_PREFIX + widgetId, null);
                    if (categoryId != null) {
                        try {
                            Category cat = DbUtils.getCategory(categoryId);
                            mTask = new Task();
                            mTask.setCategory(cat);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }

            i.setAction(null);
        }


        /**
         * Handles third party apps requests of sharing
         */
        if ((Intent.ACTION_SEND.equals(i.getAction())
                || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
                || Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
                && i.getType() != null) {

            if (mTask == null) {
                mTask = new Task();
            }

            // Text title
            String title = i.getStringExtra(Intent.EXTRA_SUBJECT);
            if (title != null) {
                mTask.title = title;
            }

            // Text content
            String content = i.getStringExtra(Intent.EXTRA_TEXT);
            if (content != null) {
                mTask.content = content;
            }

            i.setAction(null);
        }

    }

    private void initViews() {
        // Color of tag marker if note is tagged a function is active in preferences
        setCategoryMarkerColor(mTask.getCategory());

        // Init title edit text
        mTitleEditText.setText(mTask.title);
        // To avoid dropping here the dragged isChecklist items
        mTitleEditText.setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return true;
            }
        });
        //When editor action is pressed focus is moved to last character in content field
        mTitleEditText.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                mContentEditText.requestFocus();
                mContentEditText.setSelection(mContentEditText.getText().length());
                return false;
            }
        });
        mTitleEditText.setMovementMethod(new LinkHandler());
        if (mTask.equals(new Task())) { // if the current task is totally empty, display the keyboard
            KeyboardUtils.showKeyboard(mTitleEditText);
        }

        // Init content edit text
        mContentEditText.setText(mTask.content);
        // Avoid focused line goes under the keyboard
        mContentEditText.addTextChangedListener(this);
        mContentEditText.setMovementMethod(new LinkHandler());
        // Restore isChecklist
        mToggleChecklistView = mContentEditText;
        if (mTask.isChecklist) {
            mTask.isChecklist = false;
            mToggleChecklistView.setAlpha(0);
            toggleChecklist();
        }

        // Preparation for reminder icon
        mReminderDateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReminderPickers reminderPicker = new ReminderPickers(getActivity(), DetailFragment.this);
                reminderPicker.pick(mTask.hasAlarmInFuture() ? mTask.alarmDate : Calendar.getInstance().getTimeInMillis());
            }
        });
        mReminderDateButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.remove_reminder)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mTask.alarmDate = 0;
                                mReminderDateButton.setText("");
                            }
                        }).show();
                return true;
            }
        });

        // Footer dates of creation
        String creation = DateHelper.getDateTimeShort(getActivity(), mTask.creationDate);
        if (!TextUtils.isEmpty(creation)) {
            String lastModification = DateHelper.getDateTimeShort(getActivity(), mTask.lastModificationDate);
            if (TextUtils.isEmpty(lastModification)) {
                mCreationDateTextView.append(getString(R.string.creation, creation));
            } else {
                mCreationDateTextView.append(getString(R.string.creation, creation) + "  —  " + getString(R.string.last_update, lastModification));
            }
        } else {
            mCreationDateTextView.setVisibility(View.GONE);
        }

            if (mTask.pointReward == Task.LOW_POINT_REWARD) {
                mRewardSpinner.setSelection(0);
            } else if (mTask.pointReward == Task.NORMAL_POINT_REWARD) {
                mRewardSpinner.setSelection(1);
            } else if (mTask.pointReward == Task.HIGH_POINT_REWARD) {
                mRewardSpinner.setSelection(2);
            } else if (mTask.pointReward == Task.VERY_HIGH_POINT_REWARD) {
                mRewardSpinner.setSelection(3);
            }
            mRewardPoints.setText(String.valueOf(mTask.pointReward));

            mRewardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0:
                            mTask.pointReward = Task.LOW_POINT_REWARD;
                            break;
                        case 1:
                            mTask.pointReward = Task.NORMAL_POINT_REWARD;
                            break;
                        case 2:
                            mTask.pointReward = Task.HIGH_POINT_REWARD;
                            break;
                        case 3:
                            mTask.pointReward = Task.VERY_HIGH_POINT_REWARD;
                            break;
                    }
                    mRewardPoints.setText(String.valueOf(mTask.pointReward));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
    }

    /**
     * Colors category marker in note's title and content elements
     */
    private void setCategoryMarkerColor(Category category) {
        // Coloring the target
        if (category != null) {
            mCategoryMarker.setBackgroundColor(category.color);
        } else {
            mCategoryMarker.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_detail, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Closes search view if left open in List fragment
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            MenuItemCompat.collapseActionView(searchMenuItem);
        }

        boolean newNote = mTask.id == null;

        // If note is isFinished only this options will be available from menu
        if (mTask.isFinished) {
            menu.findItem(R.id.menu_restore_task).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
            // Otherwise all other actions will be available
        } else {
            menu.findItem(R.id.menu_finish_task).setVisible(!newNote);
        }
    }

    public void goHome() {
        FragmentActivity activity = getActivity();
        // The activity has managed a shared intent from third party app and
        // performs a normal onBackPressed instead of returning back to ListActivity
        if (activity != null && activity.getSupportFragmentManager() != null && activity.getSupportFragmentManager().getBackStackEntryCount() > 0) {
            if (!TextUtils.isEmpty(mExitMessage) && mExitMessageStyle != null) {
                UiUtils.showMessage(getActivity(), mExitMessage, mExitMessageStyle);
            }

            activity.getSupportFragmentManager().popBackStack();
        } else if (activity != null) {
            if (!TextUtils.isEmpty(mExitMessage)) {
                Toast.makeText(activity, mExitMessage, Toast.LENGTH_SHORT).show();
            }
            activity.finish();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveAndExit();
                break;
            case R.id.menu_category:
                categorizeNote();
                break;
            case R.id.menu_share:
                shareTask();
                break;
            case R.id.menu_finish_task:
                finishTask();
                break;
            case R.id.menu_restore_task:
                restoreTask();
                break;
            case R.id.menu_discard_changes:
                goHome();
                break;
            case R.id.menu_delete:
                deleteTask();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.switch_checkviews)
    public void toggleChecklistAndKeepChecked() {

        // In case isChecklist is active a prompt will ask about many options to decide hot to convert back to simple text
        if (!mTask.isChecklist) {
            toggleChecklist();
            return;
        }

        // If isChecklist is active but no items are checked the conversion is done automatically without prompting user
        if (mChecklistManager.getCheckedCount() == 0) {
            toggleChecklist(true, false);
            return;
        }

        // Inflate the popup_layout.xml
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_remove_checklist_layout, null);

        // Retrieves options checkboxes and initialize their values
        final CheckBox keepChecked = (CheckBox) layout.findViewById(R.id.checklist_keep_checked);
        final CheckBox keepCheckmarks = (CheckBox) layout.findViewById(R.id.checklist_keep_checkmarks);
        keepChecked.setChecked(PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true));
        keepCheckmarks.setChecked(PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true));

        new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKED, keepChecked.isChecked());
                        PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked());

                        toggleChecklist();
                    }
                }).show();
    }


    /**
     * Toggles isChecklist view
     */
    private void toggleChecklist() {
        boolean keepChecked = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true);
        boolean showChecks = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true);
        toggleChecklist(keepChecked, showChecks);
    }

    private void toggleChecklist(final boolean keepChecked, final boolean showChecks) {
        int checkBehavior = Settings.CHECKED_ON_TOP_OF_CHECKED;
        switch (PrefUtils.getString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, "0")) {
            case "0":
                checkBehavior = Settings.CHECKED_ON_TOP_OF_CHECKED;
                break;
            case "1":
                checkBehavior = Settings.CHECKED_ON_BOTTOM;
                break;
            case "2":
                checkBehavior = Settings.CHECKED_HOLD;
                break;
        }

        // Get instance and set options to convert EditText to CheckListView
        mChecklistManager = ChecklistManager.getInstance(getActivity())
                .moveCheckedOnBottom(checkBehavior)
                .showCheckMarks(showChecks)
                .keepChecked(keepChecked)
                .newEntryHint(getString(R.string.checklist_item_hint))
                .dragVibrationEnabled(true);

        // Links parsing options
        mChecklistManager.addTextChangedListener(this);
        mChecklistManager.setCheckListChangedListener(this);

        // Switches the views
        View newView = null;
        try {
            newView = mChecklistManager.convert(mToggleChecklistView);
        } catch (ViewNotSupportedException e) {
            Dog.e("Error switching checklist view", e);
        }

        // Switches the views
        if (newView != null) {
            mChecklistManager.replaceViews(mToggleChecklistView, newView);
            mToggleChecklistView = newView;
            if (newView instanceof EditText) {
                mContentEditText = (EditText) newView; // not beautiful, but fix a bug
            }
//			fade(mToggleChecklistView, true);
            mToggleChecklistView.animate().alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
            mTask.isChecklist = !mTask.isChecklist;
        }

        if (!mTask.isChecklist) {
            mSwitchCheckViewsButton.setText(R.string.checklist_on);
            mSwitchCheckViewsButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_list_grey600_24dp, 0, 0, 0);
        } else {
            mSwitchCheckViewsButton.setText(R.string.checklist_off);
            mSwitchCheckViewsButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_subject_grey600_24dp, 0, 0, 0);
        }
    }


    /**
     * Categorize note choosing from a list of previously created categories
     */
    private void categorizeNote() {
        // Retrieves all available categories
        final List<Category> categories = DbUtils.getCategories();

        new AlertDialog.Builder(getActivity()).setTitle(R.string.categorize_as)
                .setAdapter(new CategoryAdapter(getActivity(), categories), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position) {
                        mTask.setCategory(categories.get(position));
                        setCategoryMarkerColor(categories.get(position));
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.add_category, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getActivity(), CategoryActivity.class);
                        intent.putExtra("noHome", true);
                        startActivityForResult(intent, CATEGORY_CHANGE);
                    }
                })
                .setNegativeButton(R.string.remove_category, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mTask.setCategory(null);
                        setCategoryMarkerColor(null);
                    }
                }).show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CATEGORY_CHANGE:
                    UiUtils.showMessage(getActivity(), R.string.category_saved);
                    Category category = Parcels.unwrap(intent.getParcelableExtra(Constants.INTENT_CATEGORY));
                    mTask.setCategory(category);
                    setCategoryMarkerColor(category);
                    break;
            }
        }
    }

    private void finishTask() {
        // Simply go back if is a new note
        if (mTask.id == null) {
            goHome();
            return;
        }

        mTask.isFinished = true;
        mExitMessage = getString(R.string.task_finished);
        mExitMessageStyle = UiUtils.MessageType.TYPE_WARN;
        mTask.cancelReminderAlarm(MainApplication.getContext());
        saveTask();
    }

    private void restoreTask() {
        // Simply go back if is a new note
        if (mTask.id == null) {
            goHome();
            return;
        }

        mTask.isFinished = false;
        mExitMessage = getString(R.string.task_restored);
        mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
        mTask.setupReminderAlarm(MainApplication.getContext());
        saveTask();
    }

    private void deleteTask() {
        // Confirm dialog creation
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_task_confirmation)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DbUtils.deleteTask(mTask);
                        UiUtils.showMessage(getActivity(), R.string.task_deleted);
                        goHome();
                    }
                }).show();
    }

    public void saveAndExit() {
            goHome();
    }


    /**
     * Save new tasks, modify them or archive
     */
    void saveTask() {
        // Changed fields
        mTask.title = mTitleEditText.getText().toString();
        mTask.content = getTaskContent();

        // Check if some text or attachments of any type have been inserted or
        // is an empty note
        if (TextUtils.isEmpty(mTask.title) && TextUtils.isEmpty(mTask.content)) {

            mExitMessage = getString(R.string.empty_task_not_saved);
            mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
            goHome();
            return;
        }

        if (saveNotNeeded()) {
            return;
        }

        LoaderUtils.startAsync(this, new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                // Note updating on database
                DbUtils.updateTask(mTask, lastModificationUpdatedNeeded());

                // Set reminder if is not passed yet
                if (mTask.hasAlarmInFuture()) {
                    mTask.setupReminderAlarm(MainApplication.getContext());
                }

                subscriber.onNext(null);
            }
        }, new RxLoaderObserver<Void>() {

            @Override
            public void onNext(Void result) {
                goHome();
            }
        });
    }


    /**
     * Checks if nothing is changed to avoid committing if possible (check)
     */
    private boolean saveNotNeeded() {
        if (mTask.equals(mOriginalTask)) {
            mExitMessage = "";
            goHome();
            return true;
        }
        return false;
    }


    /**
     * Checks if only category or finish status have been changed
     * and then force to not update last modification date*
     */
    private boolean lastModificationUpdatedNeeded() {
        Task tmpTask = new Task(mTask);
        tmpTask.setCategory(mTask.getCategory());
        tmpTask.isFinished = mTask.isFinished;
        return !tmpTask.equals(mOriginalTask);
    }

    private String getTaskContent() {
        String content = "";
        if (!mTask.isChecklist) {
            content = mContentEditText.getText().toString();
        } else {
            if (mChecklistManager != null) {
                mChecklistManager.keepChecked(true)
                        .showCheckMarks(true);
                content = mChecklistManager.getText();
            }
        }
        return content;
    }

    /**
     * Updates share intent
     */
    private void shareTask() {
        mTask.title = mTitleEditText.getText().toString();
        mTask.content = getTaskContent();
        mTask.share(getActivity());
    }

    @Override
    public void onReminderPicked(long reminder) {
        mTask.alarmDate = reminder;
        if (isAdded()) {
            mReminderDateButton.setText(getString(R.string.alarm_set_on, DateHelper.getDateTimeShort(getActivity(), reminder)));
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        scrollContent();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onCheckListChanged() {
        scrollContent();
    }

    private void scrollContent() {
        if (mTask.isChecklist) {
            if (mChecklistManager.getCount() > mContentLineCounter) {
                mScrollView.scrollBy(0, 60);
            }
            mContentLineCounter = mChecklistManager.getCount();
        } else {
            if (mContentEditText.getLineCount() > mContentLineCounter) {
                mScrollView.scrollBy(0, 60);
            }
            mContentLineCounter = mContentEditText.getLineCount();
        }
    }

    /**
     * Used to check currently opened note from activity to avoid openind multiple times the same one
     */
    public Task getCurrentTask() {
        return mTask;
    }


    public class LinkHandler implements MovementMethod {

        @Override
        public void initialize(TextView widget, Spannable text) {
        }

        @Override
        public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyOther(TextView view, Spannable text, KeyEvent event) {
            return false;
        }

        @Override
        public void onTakeFocus(TextView widget, Spannable text, int direction) {
        }

        @Override
        public boolean onTrackballEvent(TextView widget, Spannable text, MotionEvent event) {
            return false;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

                if (link.length != 0) {
                    onLinkClick(link[0].getURL());
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onGenericMotionEvent(TextView widget, Spannable text, MotionEvent event) {
            return false;
        }

        @Override
        public boolean canSelectArbitrarily() {
            return true;
        }

        private void onLinkClick(final String url) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.open_link_dialog_title)
                    .setPositiveButton(R.string.open_link, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            } catch (Exception e) {
                                UiUtils.showErrorMessage(getActivity(), R.string.app_not_found);
                            }
                        }
                    })
                    .setNegativeButton(R.string.modify_link, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Nothing to do
                        }
                    }).show();
        }
    }
}



