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
package net.fred.taskgame.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.async.AttachmentTask;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.IdBasedModel;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.AttachmentAdapter;
import net.fred.taskgame.model.adapters.CategoryAdapter;
import net.fred.taskgame.model.listeners.OnAttachingFileListener;
import net.fred.taskgame.model.listeners.OnPermissionRequestedListener;
import net.fred.taskgame.model.listeners.OnReminderPickedListener;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.IntentChecker;
import net.fred.taskgame.utils.KeyboardUtils;
import net.fred.taskgame.utils.LoaderUtils;
import net.fred.taskgame.utils.PermissionsHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ReminderHelper;
import net.fred.taskgame.utils.StorageHelper;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.utils.date.DateHelper;
import net.fred.taskgame.utils.date.ReminderPickers;
import net.fred.taskgame.view.ExpandableHeightGridView;

import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import it.feio.android.checklistview.ChecklistManager;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.Subscriber;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


public class DetailFragment extends Fragment implements OnReminderPickedListener, OnAttachingFileListener, TextWatcher, CheckListChangedListener {

    private static final int TAKE_PHOTO = 1;
    private static final int TAKE_VIDEO = 2;
    private static final int CATEGORY_CHANGE = 3;
    private static final int FILES = 4;

    @Bind(R.id.reminder_date)
    TextView mReminderDateTextView;
    @Bind(R.id.gridview)
    ExpandableHeightGridView mGridView;
    @Bind(R.id.detail_title)
    EditText mTitleEditText;
    @Bind(R.id.category_marker)
    View mCategoryMarker;
    @Bind(R.id.detail_content)
    EditText mContentEditText;
    @Bind(R.id.content_wrapper)
    ScrollView mScrollView;
    @Bind(R.id.reward_layout)
    View mRewardLayout;
    @Bind(R.id.reminder_layout)
    View mReminderLayout;
    @Bind(R.id.reward_spinner)
    Spinner mRewardSpinner;
    @Bind(R.id.creation_date)
    TextView mCreationDateTextView;
    @Bind(R.id.last_modification_date)
    TextView mLastModificationDateTextView;

    private Task mTask;
    private Task mOriginalTask;
    // Audio recording
    private String mRecordName;
    private MediaPlayer mPlayer = null;
    private boolean mIsRecording = false;
    private View mIsPlayingView = null;
    private Bitmap mRecordingBitmap;
    private ChecklistManager mChecklistManager;
    // Values to print result
    private String mExitMessage;
    private UiUtils.MessageType mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
    private long mAudioRecordingTimeStart;
    private long mAudioRecordingTime;
    private Attachment mSketchEdited;
    private int mContentLineCounter = 1;
    private MediaRecorder mRecorder = null;
    private Uri mAttachmentUri;
    private AttachmentAdapter mAttachmentAdapter;
    private PopupWindow mAttachmentDialog;
    private View mToggleChecklistView;

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
            mAttachmentUri = savedInstanceState.getParcelable("mAttachmentUri");
        }

        // Added the sketched image if present returning from SketchFragment
        if (getMainActivity().sketchUri != null) {
            Attachment attachment = new Attachment();
            attachment.uri = getMainActivity().sketchUri;
            attachment.mimeType = Constants.MIME_TYPE_SKETCH;
            mTask.getAttachmentsList().add(attachment);
            getMainActivity().sketchUri = null;
            // Removes previous version of edited image
            if (mSketchEdited != null) {
                mTask.getAttachmentsList().remove(mSketchEdited);
                mSketchEdited = null;
            }
        }

        getMainActivity().getSupportActionBar().setTitle("");

        // Handling of Intent actions
        handleIntents();

        if (mOriginalTask == null) {
            mOriginalTask = Parcels.unwrap(getArguments().getParcelable(Constants.INTENT_TASK));
        }

        if (mTask == null) {
            mTask = new Task(mOriginalTask);
        }

        if (mTask.alarmDate != 0) {
            mReminderDateTextView.setText(getString(R.string.alarm_set_on, DateHelper.getDateTimeShort(getContext(), mTask.alarmDate)));
        }

        initViews();

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mTask.title = mTitleEditText.getText().toString();
        mTask.content = getTaskContent();
        outState.putParcelable("mTask", Parcels.wrap(mTask));
        outState.putParcelable("mOriginalTask", Parcels.wrap(mOriginalTask));
        outState.putParcelable("mAttachmentUri", mAttachmentUri);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        // Closes keyboard on exit
        if (mToggleChecklistView != null) {
            KeyboardUtils.hideKeyboard(mToggleChecklistView);
            mContentEditText.clearFocus();
        }
    }

    private void handleIntents() {
        Intent i = getActivity().getIntent();

        // Action called from home shortcut
        if (Constants.ACTION_SHORTCUT.equals(i.getAction())
                || Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())) {
            mOriginalTask = DbHelper.getTask(i.getLongExtra(Constants.INTENT_KEY, 0));
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
        if (Constants.ACTION_WIDGET.equals(i.getAction())
                || Constants.ACTION_TAKE_PHOTO.equals(i.getAction())) {

            //  with tags to set tag
            if (i.hasExtra(Constants.INTENT_WIDGET)) {
                String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
                if (widgetId != null) {
                    long categoryId = PrefUtils.getLong(PrefUtils.PREF_WIDGET_PREFIX + widgetId, -1);
                    if (categoryId != -1) {
                        try {
                            Category cat = DbHelper.getCategory(categoryId);
                            mTask = new Task();
                            mTask.setCategory(cat);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }

            // Sub-action is to take a photo
            if (Constants.ACTION_TAKE_PHOTO.equals(i.getAction())) {
                takePhoto();
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

            // Single attachment data
            Uri uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
            // Due to the fact that Google Now passes intent as text but with
            // audio recording attached the case must be handled in specific way
            if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(i.getAction())) {
//		    	String mimeType = StorageHelper.getMimeTypeInternal(((MainActivity)getActivity()), i.getType());
//		    	Attachment mAttachment = new Attachment(uri, mimeType);
//		    	if (Constants.MIME_TYPE_FILES.equals(mimeType)) {
//			    	mAttachment.setName(uri.getLastPathSegment());
//		    	}
//		    	noteTmp.addAttachment(mAttachment);
                AttachmentTask task = new AttachmentTask(this, uri, this);
                task.execute();
            }

            // Multiple attachment data
            ArrayList<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                for (Uri uriSingle : uris) {
                    AttachmentTask task = new AttachmentTask(this, uriSingle, this);
                    task.execute();
                }
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

        // Some fields can be filled by third party application and are always shown
        mAttachmentAdapter = new AttachmentAdapter(getActivity(), mTask.getAttachmentsList());
        mAttachmentAdapter.setOnErrorListener(this);

        // Initialization of gridview for images
        mGridView.setAdapter(mAttachmentAdapter);
        mGridView.autoresize();

        // Click events for images in gridview (zooms image)
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Attachment attachment = (Attachment) parent.getAdapter().getItem(position);
                if (Constants.MIME_TYPE_AUDIO.equals(attachment.mimeType)) {
                    playback(v, attachment.uri);
                } else {
                    Intent attachmentIntent = new Intent(Intent.ACTION_VIEW);
                    attachmentIntent.setDataAndType(attachment.uri, StorageHelper.getMimeType(getActivity(), attachment.uri));
                    attachmentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (IntentChecker.isAvailable(getActivity().getApplicationContext(), attachmentIntent, null)) {
                        try {
                            startActivity(attachmentIntent);
                        } catch (Exception ignored) {
                        }
                    } else {
                        UiUtils.showWarningMessage(getActivity(), R.string.feature_not_available_on_this_device);
                    }
                }

            }
        });

        // Long click events for images in gridview (removes image)
        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, final int position, long id) {

                // To avoid deleting audio attachment during playback
                if (mPlayer != null) {
                    return false;
                }

                // If is an image user could want to sketch it!
                if (Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).mimeType)) {
                    new AlertDialog.Builder(getActivity()).setMessage(R.string.delete_selected_image)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mTask.getAttachmentsList().remove(position);
                                    mAttachmentAdapter.notifyDataSetChanged();
                                    mGridView.autoresize();
                                }
                            })
                            .setNegativeButton(R.string.edit, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mSketchEdited = mAttachmentAdapter.getItem(position);
                                    takeSketch(mSketchEdited);
                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(getActivity()).setMessage(R.string.delete_selected_image)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mTask.getAttachmentsList().remove(position);
                                    mAttachmentAdapter.notifyDataSetChanged();
                                    mGridView.autoresize();
                                }
                            }).show();
                }

                return true;
            }
        });


        // Preparation for reminder icon
        mReminderLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int pickerType = PrefUtils.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP : ReminderPickers.TYPE_GOOGLE;
                ReminderPickers reminderPicker = new ReminderPickers(getActivity(), DetailFragment.this, pickerType);
                reminderPicker.pick(mTask.alarmDate);
            }
        });
        mReminderLayout.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.remove_reminder)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mTask.alarmDate = 0;
                                mReminderDateTextView.setText("");
                            }
                        }).show();
                return true;
            }
        });

        // Footer dates of creation...
        String creation = DateHelper.getDateTimeShort(getActivity(), mTask.creationDate);
        mCreationDateTextView.append(creation.length() > 0 ? getString(R.string.creation, creation) : "");
        if (mCreationDateTextView.getText().length() == 0)
            mCreationDateTextView.setVisibility(View.GONE);

        // ... and last modification
        String lastModification = DateHelper.getDateTimeShort(getActivity(), mTask.lastModificationDate);
        mLastModificationDateTextView.append(lastModification.length() > 0 ? getString(R.string.last_update, lastModification) : "");
        if (mLastModificationDateTextView.getText().length() == 0)
            mLastModificationDateTextView.setVisibility(View.GONE);

        if (TextUtils.isEmpty(mTask.questId)) {
            if (mTask.pointReward == Task.LOW_POINT_REWARD) {
                mRewardSpinner.setSelection(0);
            } else if (mTask.pointReward == Task.NORMAL_POINT_REWARD) {
                mRewardSpinner.setSelection(1);
            } else if (mTask.pointReward == Task.HIGH_POINT_REWARD) {
                mRewardSpinner.setSelection(2);
            } else if (mTask.pointReward == Task.VERY_HIGH_POINT_REWARD) {
                mRewardSpinner.setSelection(3);
            }

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
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else { // For a quest: do not display or allow edition of some fields
            mTitleEditText.setEnabled(false);
            mContentEditText.setEnabled(false);
            mRewardLayout.setVisibility(View.GONE);
        }
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
        if (TextUtils.isEmpty(mTask.questId)) { // no menu for quests
            inflater.inflate(R.menu.menu_detail, menu);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        if (!TextUtils.isEmpty(mTask.questId)) { // no menu for quests
            return;
        }

        // Closes search view if left open in List fragment
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            MenuItemCompat.collapseActionView(searchMenuItem);
        }

        boolean newNote = mTask.id == IdBasedModel.INVALID_ID;

        menu.findItem(R.id.menu_checklist_on).setVisible(!mTask.isChecklist);
        menu.findItem(R.id.menu_checklist_off).setVisible(mTask.isChecklist);
        // If note is isFinished only this options will be available from menu
        if (mTask.isFinished) {
            menu.findItem(R.id.menu_restore_task).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
            // Otherwise all other actions will be available
        } else {
            menu.findItem(R.id.menu_finish_task).setVisible(!newNote);
        }
    }

    public boolean goHome() {
        stopPlaying();

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

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveAndExit();
                break;
            case R.id.menu_attachment:
                showPopup(getActivity().findViewById(R.id.menu_attachment));
                break;
            case R.id.menu_category:
                categorizeNote();
                break;
            case R.id.menu_share:
                shareTask();
                break;
            case R.id.menu_checklist_on:
                toggleChecklistAndKeepChecked();
                break;
            case R.id.menu_checklist_off:
                toggleChecklistAndKeepChecked();
                break;
            case R.id.menu_finish_task:
                finishTask();
                break;
            case R.id.menu_restore_task:
                restoreTask();
                break;
            case R.id.menu_discard_changes:
                discard();
                break;
            case R.id.menu_delete:
                deleteTask();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleChecklistAndKeepChecked() {

        // In case isChecklist is active a prompt will ask about many options
        // to decide hot to convert back to simple text
        if (!mTask.isChecklist) {
            toggleChecklist();
            return;
        }

        // If isChecklist is active but no items are checked the conversion in done automatically
        // without prompting user
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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
        // Get instance and set options to convert EditText to CheckListView
        mChecklistManager = ChecklistManager.getInstance(getActivity());
        mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(PrefUtils.getString("settings_checked_items_behavior",
                String.valueOf(it.feio.android.checklistview.Settings.CHECKED_HOLD))));
        mChecklistManager.setShowChecks(true);
        mChecklistManager.setNewEntryHint(getString(R.string.checklist_item_hint));

        // Links parsing options
        mChecklistManager.addTextChangedListener(this);
        mChecklistManager.setCheckListChangedListener(this);

        // Options for converting back to simple text
        mChecklistManager.setKeepChecked(keepChecked);
        mChecklistManager.setShowChecks(showChecks);

        // Vibration
        mChecklistManager.setDragVibrationEnabled(true);

        // Switches the views
        View newView = mChecklistManager.convert(mToggleChecklistView);

        // Switches the views
        if (newView != null) {
            mChecklistManager.replaceViews(mToggleChecklistView, newView);
            mToggleChecklistView = newView;
            if (newView instanceof EditText) {
                mContentEditText = (EditText) newView; // not beautiful, but fix a bug
            }
//			fade(mToggleChecklistView, true);
            animate(mToggleChecklistView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
            mTask.isChecklist = !mTask.isChecklist;
        }

        getActivity().supportInvalidateOptionsMenu();
    }


    /**
     * Categorize note choosing from a list of previously created categories
     */
    private void categorizeNote() {
        // Retrieves all available categories
        final List<Category> categories = DbHelper.getCategories();

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


    // The method that displays the popup.
    private void showPopup(View anchor) {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Inflate the popup_layout.xml
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.attachment_dialog, null);

        // Creating the PopupWindow
        mAttachmentDialog = new PopupWindow(getActivity());
        mAttachmentDialog.setContentView(layout);
        mAttachmentDialog.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mAttachmentDialog.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mAttachmentDialog.setFocusable(true);
        mAttachmentDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mIsRecording) {
                    mIsRecording = false;
                    stopRecording();
                }
            }
        });

        // Clear the default translucent background
        mAttachmentDialog.setBackgroundDrawable(new BitmapDrawable());

        // Camera
        android.widget.TextView cameraSelection = (android.widget.TextView) layout.findViewById(R.id.camera);
        cameraSelection.setOnClickListener(new AttachmentOnClickListener());
        // Audio recording
        android.widget.TextView recordingSelection = (android.widget.TextView) layout.findViewById(R.id.recording);
        recordingSelection.setOnClickListener(new AttachmentOnClickListener());
        // Video recording
        android.widget.TextView videoSelection = (android.widget.TextView) layout.findViewById(R.id.video);
        videoSelection.setOnClickListener(new AttachmentOnClickListener());
        // Files
        android.widget.TextView filesSelection = (android.widget.TextView) layout.findViewById(R.id.files);
        filesSelection.setOnClickListener(new AttachmentOnClickListener());
        // Sketch
        android.widget.TextView sketchSelection = (android.widget.TextView) layout.findViewById(R.id.sketch);
        sketchSelection.setOnClickListener(new AttachmentOnClickListener());

        try {
            mAttachmentDialog.showAsDropDown(anchor);
        } catch (Exception e) {
            Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void takePhoto() {
        // Checks for camera app available
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (!IntentChecker.isAvailable(getActivity(), intent, new String[]{PackageManager.FEATURE_CAMERA})) {
            UiUtils.showWarningMessage(getActivity(), R.string.feature_not_available_on_this_device);
            return;
        }
        // Checks for created file validity
        File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_IMAGE_EXT);
        if (f == null) {
            UiUtils.showErrorMessage(getActivity(), R.string.error);
            return;
        }
        // Launches intent
        mAttachmentUri = Uri.fromFile(f);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mAttachmentUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    private void takeVideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (!IntentChecker.isAvailable(getActivity(), takeVideoIntent, new String[]{PackageManager.FEATURE_CAMERA})) {
            UiUtils.showWarningMessage(getActivity(), R.string.feature_not_available_on_this_device);
            return;
        }
        // File is stored in custom ON folder to speedup the attachment
        File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_VIDEO_EXT);
        if (f == null) {
            UiUtils.showErrorMessage(getActivity(), R.string.error);
            return;
        }
        mAttachmentUri = Uri.fromFile(f);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mAttachmentUri);

        String maxVideoSizeStr = "".equals(PrefUtils.getString("settings_max_video_size", "")) ? "0" : PrefUtils.getString("settings_max_video_size", "");
        int maxVideoSize = Integer.parseInt(maxVideoSizeStr);
        takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.valueOf(maxVideoSize * 1024 * 1024));
        startActivityForResult(takeVideoIntent, TAKE_VIDEO);
    }

    private void takeSketch(Attachment attachment) {
        File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_SKETCH_EXT);
        if (f == null) {
            UiUtils.showErrorMessage(getActivity(), R.string.error);
            return;
        }
        mAttachmentUri = Uri.fromFile(f);

        // Forces portrait orientation to this fragment only
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Fragments replacing
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TRANSITION_HORIZONTAL);
        SketchFragment sketchFragment = new SketchFragment();
        Bundle b = new Bundle();
        b.putParcelable(MediaStore.EXTRA_OUTPUT, mAttachmentUri);
        if (attachment != null) {
            b.putParcelable("base", attachment.uri);
        }
        sketchFragment.setArguments(b);
        transaction.replace(R.id.fragment_container, sketchFragment, getMainActivity().FRAGMENT_SKETCH_TAG).addToBackStack(getMainActivity().FRAGMENT_DETAIL_TAG).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Fetch uri from activities, store into adapter and refresh adapter
        Attachment attachment = new Attachment();
        attachment.uri = mAttachmentUri;

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case TAKE_PHOTO:
                    attachment.mimeType = Constants.MIME_TYPE_IMAGE;
                    mTask.getAttachmentsList().add(attachment);
                    mAttachmentAdapter.notifyDataSetChanged();
                    mGridView.autoresize();
                    break;
                case TAKE_VIDEO:
                    attachment.mimeType = Constants.MIME_TYPE_VIDEO;
                    mTask.getAttachmentsList().add(attachment);
                    mAttachmentAdapter.notifyDataSetChanged();
                    mGridView.autoresize();
                    break;
                case FILES:
                    onActivityResultManageReceivedFiles(intent);
                    break;
                case CATEGORY_CHANGE:
                    UiUtils.showMessage(getActivity(), R.string.category_saved);
                    Category category = Parcels.unwrap(intent.getParcelableExtra(Constants.INTENT_CATEGORY));
                    mTask.setCategory(category);
                    setCategoryMarkerColor(category);
                    break;
            }
        }
    }

    private void onActivityResultManageReceivedFiles(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent.getClipData() != null) {
            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                uris.add(intent.getClipData().getItemAt(i).getUri());
            }
        } else {
            uris.add(intent.getData());
        }
        for (Uri uri : uris) {
            new AttachmentTask(this, uri, this).execute();
        }
    }


    /**
     * Discards changes done to the note and eventually delete new attachments
     */
    private void discard() {
        // Checks if some new files have been attached and must be removed
        if (!mTask.getAttachmentsList().equals(mOriginalTask.getAttachmentsList())) {
            for (Attachment newAttachment : mTask.getAttachmentsList()) {
                if (!mOriginalTask.getAttachmentsList().contains(newAttachment)) {
                    StorageHelper.delete(getActivity(), newAttachment.uri.getPath());
                }
            }
        }

        goHome();
    }

    private void finishTask() {
        // Simply go back if is a new note
        if (mTask.id == IdBasedModel.INVALID_ID) {
            goHome();
            return;
        }

        mTask.isFinished = true;
        mExitMessage = getString(R.string.task_finished);
        mExitMessageStyle = UiUtils.MessageType.TYPE_WARN;
        ReminderHelper.removeReminder(MainApplication.getContext(), mTask);
        saveTask();
    }

    private void restoreTask() {
        // Simply go back if is a new note
        if (mTask.id == IdBasedModel.INVALID_ID) {
            goHome();
            return;
        }

        mTask.isFinished = false;
        mExitMessage = getString(R.string.task_restored);
        mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
        ReminderHelper.addReminder(MainApplication.getContext(), mTask);
        saveTask();
    }

    private void deleteTask() {
        // Confirm dialog creation
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_task_confirmation)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DbHelper.deleteTask(mTask);
                        UiUtils.showMessage(getActivity(), R.string.task_deleted);
                        goHome();
                    }
                }).show();
    }

    public void saveAndExit() {
        if (TextUtils.isEmpty(mTask.questId)) { // do not modify any quests
            mExitMessage = getString(R.string.task_updated);
            mExitMessageStyle = UiUtils.MessageType.TYPE_INFO;
            saveTask();
        } else {
            goHome();
        }
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
        if (TextUtils.isEmpty(mTask.title) && TextUtils.isEmpty(mTask.content)
                && mTask.getAttachmentsList().size() == 0) {

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
                // purge attachments
                for (Attachment oldAttachment : mOriginalTask.getAttachmentsList()) {
                    boolean stillHere = false;
                    for (Attachment currentAttachment : mTask.getAttachmentsList()) {
                        if (currentAttachment.id != IdBasedModel.INVALID_ID && currentAttachment.id == oldAttachment.id) {
                            stillHere = true;
                            break;
                        }
                    }

                    if (!stillHere) {
                        DbHelper.deleteAttachment(oldAttachment);
                    }
                }

                // Note updating on database
                DbHelper.updateTask(mTask, lastModificationUpdatedNeeded());

                // Set reminder if is not passed yet
                long now = Calendar.getInstance().getTimeInMillis();
                if (mTask.alarmDate >= now) {
                    ReminderHelper.addReminder(MainApplication.getContext(), mTask);
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
                mChecklistManager.setKeepChecked(true);
                mChecklistManager.setShowChecks(true);
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

    /**
     * Audio recordings playback
     */
    private void playback(View v, Uri uri) {
        // Some recording is playing right now
        if (mPlayer != null && mPlayer.isPlaying()) {
            // If the audio actually played is NOT the one from the click view the last one is played
            if (mIsPlayingView != v) {
                stopPlaying();
                mIsPlayingView = v;
                startPlaying(uri);
                mRecordingBitmap = ((BitmapDrawable) ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable()).getBitmap();
                ((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageResource(R.drawable.stop);
                // Otherwise just stops playing
            } else {
                stopPlaying();
            }
            // If nothing is playing audio just plays
        } else {
            mIsPlayingView = v;
            startPlaying(uri);
            Drawable d = ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable();
            if (BitmapDrawable.class.isAssignableFrom(d.getClass())) {
                mRecordingBitmap = ((BitmapDrawable) d).getBitmap();
            } else {
                mRecordingBitmap = ((GlideBitmapDrawable) d.getCurrent()).getBitmap();
            }
            ((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageResource(R.drawable.stop);
        }
    }

    private void startPlaying(Uri uri) {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        try {
            mPlayer.setDataSource(getActivity(), uri);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayer = null;
                    ((ImageView) mIsPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(mRecordingBitmap);
                    mRecordingBitmap = null;
                    mIsPlayingView = null;
                }
            });
        } catch (IOException e) {
            Dog.e("prepare() failed", e);
            UiUtils.showErrorMessage(getActivity(), R.string.error);
        }
    }

    private void stopPlaying() {
        if (mPlayer != null) {
            ((ImageView) mIsPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(mRecordingBitmap);
            mIsPlayingView = null;
            mRecordingBitmap = null;
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void startRecording(final View v) {
        PermissionsHelper.requestPermission(getActivity(), Manifest.permission.RECORD_AUDIO,
                R.string.permission_audio_recording, new OnPermissionRequestedListener() {
                    @Override
                    public void onPermissionGranted() {
                        mIsRecording = true;
                        android.widget.TextView mTextView = (android.widget.TextView) v;
                        mTextView.setText(getString(R.string.stop));
                        mTextView.setTextColor(Color.parseColor("#ff0000"));

                        File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_AUDIO_EXT);
                        if (f == null) {
                            UiUtils.showErrorMessage(getActivity(), R.string.error);
                            return;
                        }

                        if (mRecorder == null) {
                            mRecorder = new MediaRecorder();
                            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
                            mRecorder.setAudioEncodingBitRate(16);
                            mRecorder.setAudioSamplingRate(44100);
                        }
                        mRecordName = f.getAbsolutePath();
                        mRecorder.setOutputFile(mRecordName);

                        try {
                            mAudioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
                            mRecorder.prepare();
                            mRecorder.start();
                        } catch (IOException | IllegalStateException e) {
                            Dog.e("prepare() failed", e);
                            UiUtils.showErrorMessage(getActivity(), R.string.error);
                        }
                    }
                });
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mAudioRecordingTime = Calendar.getInstance().getTimeInMillis() - mAudioRecordingTimeStart;
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public void onAttachingFileErrorOccurred(Attachment mAttachment) {
        UiUtils.showErrorMessage(getActivity(), R.string.error_saving_attachments);
        if (mTask.getAttachmentsList().contains(mAttachment)) {
            mTask.getAttachmentsList().remove(mAttachment);
            mAttachmentAdapter.notifyDataSetChanged();
            mGridView.autoresize();
        }
    }

    @Override
    public void onAttachingFileFinished(Attachment mAttachment) {
        mTask.getAttachmentsList().add(mAttachment);
        mAttachmentAdapter.notifyDataSetChanged();
        mGridView.autoresize();
    }

    @Override
    public void onReminderPicked(long reminder) {
        mTask.alarmDate = reminder;
        if (isAdded()) {
            mReminderDateTextView.setText(getString(R.string.alarm_set_on, DateHelper.getDateTimeShort(getActivity(), reminder)));
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        scrollContent();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
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

    /**
     * Manages clicks on attachment dialog
     */
    private class AttachmentOnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                // Photo from camera
                case R.id.camera:
                    takePhoto();
                    mAttachmentDialog.dismiss();
                    break;
                case R.id.recording:
                    if (!mIsRecording) {
                        mIsRecording = true;
                        android.widget.TextView mTextView = (android.widget.TextView) v;
                        mTextView.setText(getString(R.string.stop));
                        mTextView.setTextColor(Color.parseColor("#ff0000"));
                        startRecording(v);
                    } else {
                        mIsRecording = false;
                        stopRecording();
                        Attachment attachment = new Attachment();
                        attachment.uri = Uri.parse(mRecordName);
                        attachment.mimeType = Constants.MIME_TYPE_AUDIO;
                        attachment.length = mAudioRecordingTime;
                        mTask.getAttachmentsList().add(attachment);
                        mAttachmentAdapter.notifyDataSetChanged();
                        mGridView.autoresize();
                        mAttachmentDialog.dismiss();
                    }
                    break;
                case R.id.video:
                    takeVideo();
                    mAttachmentDialog.dismiss();
                    break;
                case R.id.files:
                    Intent filesIntent;
                    filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    filesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    filesIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    filesIntent.setType("*/*");
                    startActivityForResult(filesIntent, FILES);
                    mAttachmentDialog.dismiss();
                    break;
                case R.id.sketch:
                    takeSketch(null);
                    mAttachmentDialog.dismiss();
                    break;
            }
        }
    }

    public class LinkHandler extends LinkMovementMethod {

        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP)
                return super.onTouchEvent(widget, buffer, event);

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
            }
            return true;
        }

        public void onLinkClick(final String url) {
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



