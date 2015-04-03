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

import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.neopixl.pixlui.components.edittext.EditText;
import com.neopixl.pixlui.components.textview.TextView;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.async.AttachmentTask;
import net.fred.taskgame.async.DeleteNoteTask;
import net.fred.taskgame.async.SaveTask;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.AttachmentAdapter;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.model.adapters.PlacesAutoCompleteAdapter;
import net.fred.taskgame.model.listeners.OnAttachingFileListener;
import net.fred.taskgame.model.listeners.OnGeoUtilResultListener;
import net.fred.taskgame.model.listeners.OnReminderPickedListener;
import net.fred.taskgame.model.listeners.OnTaskSaved;
import net.fred.taskgame.utils.ConnectionHelper;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.CroutonHelper;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Display;
import net.fred.taskgame.utils.GeocodeHelper;
import net.fred.taskgame.utils.IntentChecker;
import net.fred.taskgame.utils.KeyboardUtils;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ReminderHelper;
import net.fred.taskgame.utils.StorageHelper;
import net.fred.taskgame.utils.date.DateHelper;
import net.fred.taskgame.utils.date.ReminderPickers;
import net.fred.taskgame.view.ExpandableHeightGridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.checklistview.ChecklistManager;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.pixlui.links.TextLinkClickListener;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


public class DetailFragment extends Fragment implements
		OnReminderPickedListener, TextLinkClickListener, OnTouchListener, OnAttachingFileListener, TextWatcher, CheckListChangedListener, OnTaskSaved, OnGeoUtilResultListener {

	private static final int TAKE_PHOTO = 1;
	private static final int TAKE_VIDEO = 2;
	private static final int CATEGORY_CHANGE = 3;
	private static final int FILES = 4;

	public OnDateSetListener onDateSetListener;
	public OnTimeSetListener onTimeSetListener;
	MediaRecorder mRecorder = null;
    // Toggle isChecklist view
    View toggleChecklistView;
	private TextView datetime;
	private Uri attachmentUri;
	private AttachmentAdapter mAttachmentAdapter;
	private ExpandableHeightGridView mGridView;
	private PopupWindow attachmentDialog;
	private EditText title, content;
	private TextView locationTextView;
	private Task mTask;
	private Task mOriginalTask;
	// Reminder
	private String reminderDate = "", reminderTime = "";
	private String dateTimeText = "";
	// Audio recording
	private String recordName;
	private MediaPlayer mPlayer = null;
	private boolean isRecording = false;
	private View isPlayingView = null;
	private Bitmap recordingBitmap;
	private ChecklistManager mChecklistManager;
	// Values to print result
	private String exitMessage;
	private Style exitCroutonStyle = CroutonHelper.CONFIRM;
	// Flag to check if after editing it will return to ListActivity or not
	// and in the last case a Toast will be shown instead than Crouton
	private boolean afterSavedReturnsToList = true;
	private boolean swiping;
	private int startSwipeX;
	private boolean orientationChanged;
	private long audioRecordingTimeStart;
	private long audioRecordingTime;
	private DetailFragment mFragment;
	private Attachment sketchEdited;
	private ScrollView scrollView;
	private int contentLineCounter = 1;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFragment = this;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_detail, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getMainActivity().getSupportActionBar().setDisplayShowTitleEnabled(false);
		getMainActivity().getToolbar().setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				navigateUp();
			}
		});

		// Force the navigation drawer to stay closed
		getMainActivity().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		// Restored temp note after orientation change
		if (savedInstanceState != null) {
			mTask = savedInstanceState.getParcelable("note");
			mOriginalTask = savedInstanceState.getParcelable("noteOriginal");
			attachmentUri = savedInstanceState.getParcelable("attachmentUri");
			orientationChanged = savedInstanceState.getBoolean("orientationChanged");
		}

		// Added the sketched image if present returning from SketchFragment
		if (getMainActivity().sketchUri != null) {

//			attachment.mimeType = Constants.MIME_TYPE_SKETCH;
//			mTask.getAttachmentsList().add(attachment);
//			mAttachmentAdapter.notifyDataSetChanged();
//			mGridView.autoresize();

			Attachment attachment = new Attachment();
			attachment.uri = getMainActivity().sketchUri;
			attachment.mimeType = Constants.MIME_TYPE_SKETCH;
			mTask.getAttachmentsList().add(attachment);
			getMainActivity().sketchUri = null;
			// Removes previous version of edited image
			if (sketchEdited != null) {
				mTask.getAttachmentsList().remove(sketchEdited);
				sketchEdited = null;
			}
		}

		init();

		setHasOptionsMenu(true);
		setRetainInstance(false);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		mTask.title = getTaskTitle();
		mTask.content = getTaskContent();
		outState.putParcelable("note", mTask);
		outState.putParcelable("noteOriginal", mOriginalTask);
		outState.putParcelable("attachmentUri", attachmentUri);
		outState.putBoolean("orientationChanged", orientationChanged);
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
		if (toggleChecklistView != null) {
			KeyboardUtils.hideKeyboard(toggleChecklistView);
			content.clearFocus();
		}
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (getResources().getConfiguration().orientation != newConfig.orientation) {
			orientationChanged = true;
		}
	}


	private void init() {

		// Handling of Intent actions
		handleIntents();

		if (mOriginalTask == null) {
			mOriginalTask = getArguments().getParcelable(Constants.INTENT_TASK);
		}

		if (mTask == null) {
			mTask = new Task(mOriginalTask);
		}

		if (mTask.alarmDate != 0) {
			dateTimeText = initReminder(mTask.alarmDate);
		}

		initViews();
	}

	private void handleIntents() {
		Intent i = getActivity().getIntent();

		// Action called from home shortcut
		if (Constants.ACTION_SHORTCUT.equals(i.getAction())
				|| Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())) {
			afterSavedReturnsToList = false;
			mOriginalTask = DbHelper.getTask(i.getIntExtra(Constants.INTENT_KEY, 0));
			// Checks if the note pointed from the shortcut has been deleted
			if (mOriginalTask == null) {
				getMainActivity().showToast(getText(R.string.shortcut_task_deleted), Toast.LENGTH_LONG);
				getActivity().finish();
			}
			mTask = new Task(mOriginalTask);
			i.setAction(null);
		}

		// Check if is launched from a widget
		if (Constants.ACTION_WIDGET.equals(i.getAction())
				|| Constants.ACTION_TAKE_PHOTO.equals(i.getAction())) {

			afterSavedReturnsToList = false;

			//  with tags to set tag
			if (i.hasExtra(Constants.INTENT_WIDGET)) {
				String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
				if (widgetId != null) {
					int categoryId = PrefUtils.getInt(PrefUtils.PREF_WIDGET_PREFIX + widgetId, -1);
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

			afterSavedReturnsToList = false;

			if (mTask == null) mTask = new Task();

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

		// Sets onTouchListener to the whole activity to swipe tasks
		ViewGroup root = (ViewGroup) getView().findViewById(R.id.detail_root);
		root.setOnTouchListener(this);

		// ScrollView container
		scrollView = (ScrollView) getView().findViewById(R.id.content_wrapper);

		// Color of tag marker if note is tagged a function is active in preferences
		setTagMarkerColor(mTask.getCategory());

		// Sets links clickable in title and content Views
		title = initTitle();
		requestFocus(title);

		content = initContent();

		// Initialization of location TextView
		locationTextView = (TextView) getView().findViewById(R.id.location);

		if (isTaskLocationValid()) {
			if (!TextUtils.isEmpty(mTask.address)) {
				locationTextView.setVisibility(View.VISIBLE);
				locationTextView.setText(mTask.address);
			} else {
				GeocodeHelper.getAddressFromCoordinates(getActivity(), mTask.latitude, mTask.longitude, mFragment);
			}
		}

		locationTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String uriString = "geo:" + mTask.latitude + ',' + mTask.longitude
						+ "?q=" + mTask.latitude + ',' + mTask.longitude;
				Intent locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
				if (!IntentChecker.isAvailable(getActivity(), locationIntent, null)) {
					uriString = "http://maps.google.com/maps?q=" + mTask.latitude + ',' + mTask.longitude;
					locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
				}
				startActivity(locationIntent);
			}
		});
		locationTextView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
				builder.content(R.string.remove_location);
				builder.positiveText(R.string.ok);
				builder.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						mTask.latitude = 0.;
						mTask.longitude = 0.;
						fade(locationTextView, false);
					}
				});
				MaterialDialog dialog = builder.build();
				dialog.show();
				return true;
			}
		});


		// Some fields can be filled by third party application and are always shown
		mGridView = (ExpandableHeightGridView) getView().findViewById(R.id.gridview);
		mAttachmentAdapter = new AttachmentAdapter(getActivity(), mTask.getAttachmentsList(), mGridView);
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
						startActivity(attachmentIntent);
					} else {
						getMainActivity().showMessage(R.string.feature_not_available_on_this_device, CroutonHelper.WARN);
					}
				}

			}
		});

		// Long click events for images in gridview (removes image)
		mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, final int position, long id) {

				// To avoid deleting audio attachment during playback
				if (mPlayer != null) return false;

				MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getActivity())
						.positiveText(R.string.delete);

				// If is an image user could want to sketch it!
				if (Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).mimeType)) {
					dialogBuilder
							.content(R.string.delete_selected_image)
							.negativeText(R.string.edit)
							.callback(new MaterialDialog.Callback() {
								@Override
								public void onPositive(MaterialDialog materialDialog) {
									mTask.getAttachmentsList().remove(position);
									mAttachmentAdapter.notifyDataSetChanged();
									mGridView.autoresize();
								}

								@Override
								public void onNegative(MaterialDialog materialDialog) {
									sketchEdited = mAttachmentAdapter.getItem(position);
									takeSketch(sketchEdited);
								}
							});
				} else {
					dialogBuilder
							.content(R.string.delete_selected_image)
							.callback(new MaterialDialog.SimpleCallback() {
								@Override
								public void onPositive(MaterialDialog materialDialog) {
									mTask.getAttachmentsList().remove(position);
									mAttachmentAdapter.notifyDataSetChanged();
									mGridView.autoresize();
								}
							});
				}

				dialogBuilder.build().show();
				return true;
			}
		});


		// Preparation for reminder icon
		LinearLayout reminder_layout = (LinearLayout) getView().findViewById(R.id.reminder_layout);
		reminder_layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int pickerType = PrefUtils.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP : ReminderPickers.TYPE_GOOGLE;
				ReminderPickers reminderPicker = new ReminderPickers(getActivity(), mFragment, pickerType);
				reminderPicker.pick(mTask.alarmDate);
				onDateSetListener = reminderPicker;
				onTimeSetListener = reminderPicker;
			}
		});
		reminder_layout.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
						.content(R.string.remove_reminder)
						.positiveText(R.string.ok)
						.callback(new MaterialDialog.SimpleCallback() {
							@Override
							public void onPositive(MaterialDialog materialDialog) {
								reminderDate = "";
								reminderTime = "";
								mTask.alarmDate = 0;
								datetime.setText("");
							}
						}).build();
				dialog.show();
				return true;
			}
		});


		// Reminder
		datetime = (TextView) getView().findViewById(R.id.datetime);
		datetime.setText(dateTimeText);

		// Timestamps view
		View timestampsView = getActivity().findViewById(R.id.detail_timestamps);
		// Bottom padding set for translucent navbar in Kitkat
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int navBarHeight = Display.getNavigationBarHeightKitkat(getActivity());
			int timestampsViewPaddingBottom = navBarHeight > 0 ? navBarHeight - 22 : timestampsView.getPaddingBottom();
			timestampsView.setPadding(timestampsView.getPaddingStart(), timestampsView.getPaddingTop(),
					timestampsView.getPaddingEnd(), timestampsViewPaddingBottom);
		}

		// Footer dates of creation...
		TextView creationTextView = (TextView) getView().findViewById(R.id.creation);
		String creation = mTask.getCreationShort(getActivity());
		creationTextView.append(creation.length() > 0 ? getString(R.string.creation) + " "
				+ creation : "");
		if (creationTextView.getText().length() == 0)
			creationTextView.setVisibility(View.GONE);

		// ... and last modification
		TextView lastModificationTextView = (TextView) getView().findViewById(R.id.last_modification);
		String lastModification = mTask.getLastModificationShort(getActivity());
		lastModificationTextView.append(lastModification.length() > 0 ? getString(R.string.last_update) + " "
				+ lastModification : "");
		if (lastModificationTextView.getText().length() == 0)
			lastModificationTextView.setVisibility(View.GONE);
	}

	private EditText initTitle() {
		EditText title = (EditText) getView().findViewById(R.id.detail_title);
		title.setText(mTask.title);
		title.gatherLinksForText();
		title.setOnTextLinkClickListener(this);
        // To avoid dropping here the dragged isChecklist items
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			title.setOnDragListener(new OnDragListener() {
				@Override
				public boolean onDrag(View v, DragEvent event) {
//					((View)event.getLocalState()).setVisibility(View.VISIBLE);
					return true;
				}
			});
		}
		//When editor action is pressed focus is moved to last character in content field
		title.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
				content.requestFocus();
				content.setSelection(content.getText().length());
				return false;
			}
		});
		return title;
	}

	private EditText initContent() {
		EditText content = (EditText) getView().findViewById(R.id.detail_content);
		content.setText(mTask.content);
		content.gatherLinksForText();
		content.setOnTextLinkClickListener(this);
		// Avoid focused line goes under the keyboard
		content.addTextChangedListener(this);

        // Restore isChecklist
        toggleChecklistView = content;
		if (mTask.isChecklist) {
			mTask.isChecklist = false;
			toggleChecklistView.setAlpha(0);
			toggleChecklist2();
		}

		return content;
	}


	/**
	 * Force focus and shows soft keyboard
	 */
	private void requestFocus(final EditText view) {
		if (mTask.equals(new Task())) { // if the current task is totally empty
			KeyboardUtils.showKeyboard(view);
		}
	}


	/**
	 * Colors tag marker in note's title and content elements
	 */
	private void setTagMarkerColor(Category tag) {

		String colorsPref = PrefUtils.getString("settings_colors_app", PrefUtils.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!colorsPref.equals("disabled")) {

			// Choosing target view depending on another preference
			ArrayList<View> target = new ArrayList<>();
			if (colorsPref.equals("complete")) {
				target.add(getView().findViewById(R.id.title_wrapper));
				target.add(getView().findViewById(R.id.content_wrapper));
			} else {
				target.add(getView().findViewById(R.id.tag_marker));
			}

			// Coloring the target
			if (tag != null && tag.color != null) {
				for (View view : target) {
					view.setBackgroundColor(Integer.parseInt(tag.color));
				}
			} else {
				for (View view : target) {
					view.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}
	}

	private void setAddress() {
		if (!ConnectionHelper.isInternetAvailable(getActivity())) {
			mTask.latitude = getMainActivity().currentLatitude;
			mTask.longitude = getMainActivity().currentLongitude;
			onAddressResolved("");
			return;
		}
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_location, null);
		final AutoCompleteTextView autoCompView = (AutoCompleteTextView) v.findViewById(R.id.auto_complete_location);
		autoCompView.setHint(getString(R.string.search_location));
		autoCompView.setAdapter(new PlacesAutoCompleteAdapter(getActivity(), R.layout.simple_text_layout));
		final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
				.customView(autoCompView, false)
				.positiveText(R.string.use_current_location)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						if (TextUtils.isEmpty(autoCompView.getText().toString())) {
							double lat = getMainActivity().currentLatitude;
							double lon = getMainActivity().currentLongitude;
							mTask.latitude = lat;
							mTask.longitude = lon;
							GeocodeHelper.getAddressFromCoordinates(getActivity(), mTask.latitude,
									mTask.longitude, mFragment);
						} else {
							GeocodeHelper.getCoordinatesFromAddress(getActivity(), autoCompView.getText().toString(),
									mFragment);
						}
					}
				})
				.build();
		autoCompView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}


			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() != 0) {
					dialog.setActionButton(DialogAction.POSITIVE, getString(R.string.confirm));
				} else {
					dialog.setActionButton(DialogAction.POSITIVE, getString(R.string.use_current_location));
				}
			}


			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		dialog.show();
	}


	private MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}


	@Override
	public void onAddressResolved(String address) {
		if (TextUtils.isEmpty(address)) {
			if (!isTaskLocationValid()) {
				getMainActivity().showMessage(R.string.location_not_found, CroutonHelper.ALERT);
				return;
			}
			address = mTask.latitude + ", " + mTask.longitude;
		}
		if (!GeocodeHelper.areCoordinates(address)) {
			mTask.address = address;
		}
		locationTextView.setVisibility(View.VISIBLE);
		locationTextView.setText(address);
		fade(locationTextView, true);
	}


	@Override
	public void onCoordinatesResolved(double[] coords) {
		if (coords != null) {
			mTask.latitude = coords[0];
			mTask.longitude = coords[1];
			GeocodeHelper.getAddressFromCoordinates(getActivity(), coords[0], coords[1], new OnGeoUtilResultListener() {
				@Override
				public void onAddressResolved(String address) {
					if (!GeocodeHelper.areCoordinates(address)) {
						mTask.address = address;
					}
					locationTextView.setVisibility(View.VISIBLE);
					locationTextView.setText(address);
					fade(locationTextView, true);
				}

				@Override
				public void onCoordinatesResolved(double[] coords) {
				}
			});
		} else {
			getMainActivity().showMessage(R.string.location_not_found, CroutonHelper.ALERT);
		}
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

		boolean newNote = mTask.id == 0;

		menu.findItem(R.id.menu_checklist_on).setVisible(!mTask.isChecklist);
		menu.findItem(R.id.menu_checklist_off).setVisible(mTask.isChecklist);
		// If note is isTrashed only this options will be available from menu
		if (mTask.isTrashed) {
			menu.findItem(R.id.menu_untrash).setVisible(true);
			menu.findItem(R.id.menu_delete).setVisible(true);
			// Otherwise all other actions will be available
		} else {
			menu.findItem(R.id.menu_trash).setVisible(!newNote);
		}
	}

	public boolean goHome() {
		stopPlaying();

		// The activity has managed a shared intent from third party app and
		// performs a normal onBackPressed instead of returning back to ListActivity
		if (!afterSavedReturnsToList) {
			if (!TextUtils.isEmpty(exitMessage)) {
				getMainActivity().showToast(exitMessage, Toast.LENGTH_SHORT);
			}
			getActivity().finish();
			return true;
		} else {
			if (!TextUtils.isEmpty(exitMessage) && exitCroutonStyle != null) {
				getMainActivity().showMessage(exitMessage, exitCroutonStyle);
			}
		}

		// Otherwise the result is passed to ListActivity
		if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
			getActivity().getSupportFragmentManager().popBackStack();
			if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 1) {
				getMainActivity().getSupportActionBar().setDisplayShowTitleEnabled(true);
			}
			if (getMainActivity().getDrawerToggle() != null) {
				getMainActivity().getDrawerToggle().setDrawerIndicatorEnabled(true);
			}
		}

		if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 1) {
			getMainActivity().animateBurger(MainActivity.BURGER);
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUp();
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
				toggleChecklist();
				break;
			case R.id.menu_checklist_off:
				toggleChecklist();
				break;
			case R.id.menu_trash:
				trashTask(true);
				break;
			case R.id.menu_untrash:
				trashTask(false);
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


	private void navigateUp() {
		afterSavedReturnsToList = true;
		saveAndExit(this);
	}

	private void toggleChecklist() {

        // In case isChecklist is active a prompt will ask about many options
        // to decide hot to convert back to simple text
		if (!mTask.isChecklist) {
			toggleChecklist2();
			return;
		}

        // If isChecklist is active but no items are checked the conversion in done automatically
        // without prompting user
		if (mChecklistManager.getCheckedCount() == 0) {
			toggleChecklist2(true, false);
			return;
		}

		// Inflate the popup_layout.xml
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.dialog_remove_checklist_layout, (ViewGroup) getView().findViewById(R.id.layout_root));

		// Retrieves options checkboxes and initialize their values
		final CheckBox keepChecked = (CheckBox) layout.findViewById(R.id.checklist_keep_checked);
		final CheckBox keepCheckmarks = (CheckBox) layout.findViewById(R.id.checklist_keep_checkmarks);
		keepChecked.setChecked(PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true));
		keepCheckmarks.setChecked(PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true));

		new MaterialDialog.Builder(getActivity())
				.customView(layout)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKED, keepChecked.isChecked());
						PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked());

						toggleChecklist2();
					}
				}).build().show();
	}


	/**
     * Toggles isChecklist view
     */
	private void toggleChecklist2() {
		boolean keepChecked = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true);
		boolean showChecks = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true);
		toggleChecklist2(keepChecked, showChecks);
	}

	private void toggleChecklist2(final boolean keepChecked, final boolean showChecks) {
		// Get instance and set options to convert EditText to CheckListView
		mChecklistManager = ChecklistManager.getInstance(getActivity());
		mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(PrefUtils.getString("settings_checked_items_behavior",
				String.valueOf(it.feio.android.checklistview.Settings.CHECKED_HOLD))));
		mChecklistManager.setShowChecks(true);
		mChecklistManager.setNewEntryHint(getString(R.string.checklist_item_hint));

		// Links parsing options
		mChecklistManager.setOnTextLinkClickListener(mFragment);
		mChecklistManager.addTextChangedListener(mFragment);
		mChecklistManager.setCheckListChangedListener(mFragment);

		// Options for converting back to simple text
		mChecklistManager.setKeepChecked(keepChecked);
		mChecklistManager.setShowChecks(showChecks);

		// Vibration
		mChecklistManager.setDragVibrationEnabled(true);

		// Switches the views
		View newView = null;
		try {
			newView = mChecklistManager.convert(toggleChecklistView);
		} catch (ViewNotSupportedException e) {

		}

		// Switches the views
		if (newView != null) {
			mChecklistManager.replaceViews(toggleChecklistView, newView);
			toggleChecklistView = newView;
//			fade(toggleChecklistView, true);
			animate(toggleChecklistView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
			mTask.isChecklist = !mTask.isChecklist;
		}
	}


	/**
	 * Categorize note choosing from a list of previously created categories
	 */
	private void categorizeNote() {
		// Retrieves all available categories
		final List<Category> categories = DbHelper.getCategories();

		final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
				.title(R.string.categorize_as)
				.adapter(new NavDrawerCategoryAdapter(getActivity(), categories))
				.positiveText(R.string.add_category)
				.negativeText(R.string.remove_category)
				.callback(new MaterialDialog.Callback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						Intent intent = new Intent(getActivity(), CategoryActivity.class);
						intent.putExtra("noHome", true);
						startActivityForResult(intent, CATEGORY_CHANGE);
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						mTask.setCategory(null);
						setTagMarkerColor(null);
					}
				})
				.build();

		dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mTask.setCategory(categories.get(position));
				setTagMarkerColor(categories.get(position));
				dialog.dismiss();
			}
		});

		dialog.show();
	}


	// The method that displays the popup.
	private void showPopup(View anchor) {
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// Inflate the popup_layout.xml
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.attachment_dialog, null);

		// Creating the PopupWindow
		attachmentDialog = new PopupWindow(getActivity());
		attachmentDialog.setContentView(layout);
		attachmentDialog.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setFocusable(true);
		attachmentDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				if (isRecording) {
					isRecording = false;
					stopRecording();
				}
			}
		});

		// Clear the default translucent background
		attachmentDialog.setBackgroundDrawable(new BitmapDrawable());

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
		// Location
		android.widget.TextView locationSelection = (android.widget.TextView) layout.findViewById(R.id.location);
		locationSelection.setOnClickListener(new AttachmentOnClickListener());

		try {
			attachmentDialog.showAsDropDown(anchor);
		} catch (Exception e) {
			getMainActivity().showMessage(R.string.error, CroutonHelper.ALERT);

		}
	}

	private void takePhoto() {
		// Checks for camera app available
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (!IntentChecker.isAvailable(getActivity(), intent, new String[]{PackageManager.FEATURE_CAMERA})) {
			getMainActivity().showMessage(R.string.feature_not_available_on_this_device, CroutonHelper.ALERT);

			return;
		}
		// Checks for created file validity
		File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_IMAGE_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, CroutonHelper.ALERT);
			return;
		}
		// Launches intent
		attachmentUri = Uri.fromFile(f);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		startActivityForResult(intent, TAKE_PHOTO);
	}

	private void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!IntentChecker.isAvailable(getActivity(), takeVideoIntent, new String[]{PackageManager.FEATURE_CAMERA})) {
			getMainActivity().showMessage(R.string.feature_not_available_on_this_device, CroutonHelper.ALERT);

			return;
		}
		// File is stored in custom ON folder to speedup the attachment
		File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_VIDEO_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, CroutonHelper.ALERT);

			return;
		}
		attachmentUri = Uri.fromFile(f);
		takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);

		String maxVideoSizeStr = "".equals(PrefUtils.getString("settings_max_video_size", "")) ? "0" : PrefUtils.getString("settings_max_video_size", "");
		int maxVideoSize = Integer.parseInt(maxVideoSizeStr);
		takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.valueOf(maxVideoSize * 1024 * 1024));
		startActivityForResult(takeVideoIntent, TAKE_VIDEO);
	}

	private void takeSketch(Attachment attachment) {
		File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_SKETCH_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, CroutonHelper.ALERT);
			return;
		}
		attachmentUri = Uri.fromFile(f);

		// Forces portrait orientation to this fragment only
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Fragments replacing
		FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
		getMainActivity().animateTransition(transaction, getMainActivity().TRANSITION_HORIZONTAL);
		SketchFragment sketchFragment = new SketchFragment();
		Bundle b = new Bundle();
		b.putParcelable(MediaStore.EXTRA_OUTPUT, attachmentUri);
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
		attachment.uri = attachmentUri;

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
					getMainActivity().showMessage(R.string.category_saved, CroutonHelper.CONFIRM);
					Category category = intent.getParcelableExtra(Constants.INTENT_CATEGORY);
					mTask.setCategory(category);
					setTagMarkerColor(category);
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

	private void trashTask(boolean trash) {
		// Simply go back if is a new note
		if (mTask.id == 0) {
			goHome();
			return;
		}

		mTask.isTrashed = trash;
		exitMessage = trash ? getString(R.string.task_trashed) : getString(R.string.task_untrashed);
		exitCroutonStyle = trash ? CroutonHelper.WARN : CroutonHelper.INFO;
		if (trash) {
			ReminderHelper.removeReminder(MainApplication.getContext(), mTask);
		} else {
			ReminderHelper.addReminder(MainApplication.getContext(), mTask);
		}
		saveTask(this);
	}

	private void deleteTask() {
		// Confirm dialog creation
		new MaterialDialog.Builder(getActivity())
				.content(R.string.delete_task_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						DeleteNoteTask deleteNoteTask = new DeleteNoteTask(MainApplication.getContext());
						deleteNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mTask);

						getMainActivity().showMessage(R.string.task_deleted, CroutonHelper.ALERT);
						MainActivity.notifyAppWidgets(getActivity());
						goHome();
					}
				}).build().show();
	}

	public void saveAndExit(OnTaskSaved mOnTaskSaved) {
		exitMessage = getString(R.string.task_updated);
		exitCroutonStyle = CroutonHelper.CONFIRM;
		saveTask(mOnTaskSaved);
	}


	/**
	 * Save new tasks, modify them or archive
	 */
	void saveTask(OnTaskSaved mOnTaskSaved) {
		// Changed fields
		mTask.title = getTaskTitle();
		mTask.content = getTaskContent();

		// Check if some text or attachments of any type have been inserted or
		// is an empty note
		if (TextUtils.isEmpty(mTask.title) && TextUtils.isEmpty(mTask.content)
				&& mTask.getAttachmentsList().size() == 0) {

			exitMessage = getString(R.string.empty_task_not_saved);
			exitCroutonStyle = CroutonHelper.INFO;
			goHome();
			return;
		}

		if (saveNotNeeded()) return;

		// Saving changes to the note
		SaveTask saveTask = new SaveTask(this, mTask, mOriginalTask.getAttachmentsList(), mOnTaskSaved, lastModificationUpdatedNeeded());
		saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	/**
	 * Checks if nothing is changed to avoid committing if possible (check)
	 */
	private boolean saveNotNeeded() {
		if (mTask.equals(mOriginalTask)) {
			exitMessage = "";
			goHome();
			return true;
		}
		return false;
	}


	/**
	 * Checks if only tag, archive or trash status have been changed
	 * and then force to not update last modification date*
	 */
	private boolean lastModificationUpdatedNeeded() {
		Task tmpTask = new Task(mTask);
		tmpTask.setCategory(mTask.getCategory());
		tmpTask.isTrashed = mTask.isTrashed;
		return !tmpTask.equals(mOriginalTask);
	}


	@Override
	public void onTaskSaved(Task taskSaved) {
		MainActivity.notifyAppWidgets(MainApplication.getContext());
		goHome();
	}

	private String getTaskTitle() {
		String res;
		if (getActivity() != null && getActivity().findViewById(R.id.detail_title) != null) {
			Editable editableTitle = ((EditText) getActivity().findViewById(R.id.detail_title)).getText();
			res = TextUtils.isEmpty(editableTitle) ? "" : editableTitle.toString();
		} else {
			res = title.getText() != null ? title.getText().toString() : "";
		}
		return res;
	}

	private String getTaskContent() {
		String content = "";
		if (!mTask.isChecklist) {
			try {
				try {
					content = ((EditText) getActivity().findViewById(R.id.detail_content)).getText().toString();
				} catch (ClassCastException e) {
					content = ((android.widget.EditText) getActivity().findViewById(R.id.detail_content)).getText().toString();
				}
			} catch (NullPointerException e) {
			}
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
		mTask.title = getTaskTitle();
		mTask.content = getTaskContent();
		getMainActivity().shareTaskNote(mTask);
	}

	/**
	 * Used to set actual reminder state when initializing a note to be edited
	 */
	private String initReminder(long reminderDateTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(reminderDateTime);
		reminderDate = DateHelper.onDateSet(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH), Constants.DATE_FORMAT_SHORT_DATE);
		reminderTime = DateHelper.getTimeShort(getActivity(), cal.getTimeInMillis());
		return getString(R.string.alarm_set_on) + " " + reminderDate + " " + getString(R.string.at_time)
				+ " " + reminderTime;
	}

	/**
	 * Audio recordings playback
	 */
	private void playback(View v, Uri uri) {
		// Some recording is playing right now
		if (mPlayer != null && mPlayer.isPlaying()) {
			// If the audio actually played is NOT the one from the click view the last one is played
			if (isPlayingView != v) {
				stopPlaying();
				isPlayingView = v;
				startPlaying(uri);
				recordingBitmap = ((BitmapDrawable) ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable()).getBitmap();
				((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
				// Otherwise just stops playing
			} else {
				stopPlaying();
			}
			// If nothing is playing audio just plays
		} else {
			isPlayingView = v;
			startPlaying(uri);
			Drawable d = ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable();
			if (BitmapDrawable.class.isAssignableFrom(d.getClass())) {
				recordingBitmap = ((BitmapDrawable) d).getBitmap();
			} else {
				recordingBitmap = ((GlideBitmapDrawable) d.getCurrent()).getBitmap();
			}
			((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
		}
	}

	private void startPlaying(Uri uri) {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(getActivity(), uri);
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					mPlayer = null;
					((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
					recordingBitmap = null;
					isPlayingView = null;
				}
			});
		} catch (IOException e) {
		}
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
			isPlayingView = null;
			recordingBitmap = null;
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void startRecording() {
		File f = StorageHelper.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_AUDIO_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, CroutonHelper.ALERT);

			return;
		}
		recordName = f.getAbsolutePath();
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setAudioEncodingBitRate(16);
		mRecorder.setOutputFile(recordName);

		try {
			mRecorder.prepare();
			audioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
			mRecorder.start();
		} catch (IOException e) {
		}
	}

	private void stopRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
			audioRecordingTime = Calendar.getInstance().getTimeInMillis() - audioRecordingTimeStart;
			mRecorder.release();
			mRecorder = null;
		}
	}

	private void fade(final View v, boolean fadeIn) {

		int anim = R.animator.fade_out_support;
		int visibilityTemp = View.GONE;

		if (fadeIn) {
			anim = R.animator.fade_in_support;
			visibilityTemp = View.VISIBLE;
		}

		final int visibility = visibilityTemp;

		// Checks if user has left the app
		if (getActivity() != null) {
			Animation mAnimation = AnimationUtils.loadAnimation(getActivity(), anim);
			mAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					v.setVisibility(visibility);
				}
			});
			v.startAnimation(mAnimation);
		}
	}

	/* (non-Javadoc)
	 * @see com.neopixl.pixlui.links.TextLinkClickListener#onTextLinkClick(android.view.View, java.lang.String, java.lang.String)
	 *
	 * Receives onClick from links in EditText and shows a dialog to open link or copy content
	 */
	@Override
	public void onTextLinkClick(View view, final String clickedString, final String url) {
		MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
				.content(clickedString)
				.positiveText(R.string.open)
				.negativeText(R.string.copy)
				.callback(new MaterialDialog.Callback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						boolean error = false;
						Intent intent = null;
						try {
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
							intent.addCategory(Intent.CATEGORY_BROWSABLE);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						} catch (NullPointerException e) {
							error = true;
						}

						if (intent == null
								|| error
								|| !IntentChecker
								.isAvailable(
										getActivity(),
										intent,
										new String[]{PackageManager.FEATURE_CAMERA})) {
							getMainActivity().showMessage(R.string.no_application_can_perform_this_action, CroutonHelper.ALERT);

						} else {
							startActivity(intent);
						}
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						// Creates a new text clip to put on the clipboard
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
							android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity()
									.getSystemService(Activity.CLIPBOARD_SERVICE);
							clipboard.setText("text to clip");
						} else {
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity()
									.getSystemService(Activity.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText("text label", clickedString);
							clipboard.setPrimaryClip(clip);
						}
					}
				}).build();

		dialog.show();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int x = (int) event.getX();

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				int w;

				Point displaySize = Display.getUsableSize(getActivity());
				w = displaySize.x;

				if (x < Constants.SWIPE_MARGIN || x > w - Constants.SWIPE_MARGIN) {
					swiping = true;
					startSwipeX = x;
				}

				break;

			case MotionEvent.ACTION_UP:
				if (swiping)
					swiping = false;
				break;

			case MotionEvent.ACTION_MOVE:
				if (swiping) {

					if (Math.abs(x - startSwipeX) > Constants.SWIPE_OFFSET) {
						swiping = false;
						FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
						getMainActivity().animateTransition(transaction, getMainActivity().TRANSITION_VERTICAL);
						DetailFragment mDetailFragment = new DetailFragment();
						Bundle b = new Bundle();
						b.putParcelable(Constants.INTENT_TASK, new Task());
						mDetailFragment.setArguments(b);
						transaction.replace(R.id.fragment_container, mDetailFragment, getMainActivity().FRAGMENT_DETAIL_TAG).addToBackStack(getMainActivity().FRAGMENT_DETAIL_TAG).commit();
					}
				}
				break;
		}

		return true;
	}

	@Override
	public void onAttachingFileErrorOccurred(Attachment mAttachment) {
		getMainActivity().showMessage(R.string.error_saving_attachments, CroutonHelper.ALERT);
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
		if (mFragment.isAdded()) {
			datetime.setText(getString(R.string.alarm_set_on) + " " + DateHelper.getDateTimeShort(getActivity(), reminder));
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
			if (mChecklistManager.getCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = mChecklistManager.getCount();
		} else {
			if (content.getLineCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = content.getLineCount();
		}
	}

	/**
	 * Used to check currently opened note from activity to avoid openind multiple times the same one
	 */
	public Task getCurrentTask() {
		return mTask;
	}

	private boolean isTaskLocationValid() {
		return mTask.latitude != 0
				&& mTask.longitude != 0;
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
					attachmentDialog.dismiss();
					break;
				case R.id.recording:
					if (!isRecording) {
						isRecording = true;
						android.widget.TextView mTextView = (android.widget.TextView) v;
						mTextView.setText(getString(R.string.stop));
						mTextView.setTextColor(Color.parseColor("#ff0000"));
						startRecording();
					} else {
						isRecording = false;
						stopRecording();
						Attachment attachment = new Attachment();
						attachment.uri = Uri.parse(recordName);
						attachment.mimeType = Constants.MIME_TYPE_AUDIO;
						attachment.length = audioRecordingTime;
						mTask.getAttachmentsList().add(attachment);
						mAttachmentAdapter.notifyDataSetChanged();
						mGridView.autoresize();
						attachmentDialog.dismiss();
					}
					break;
				case R.id.video:
					takeVideo();
					attachmentDialog.dismiss();
					break;
				case R.id.files:
					Intent filesIntent;
					filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
					filesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					filesIntent.addCategory(Intent.CATEGORY_OPENABLE);
					filesIntent.setType("*/*");
					startActivityForResult(filesIntent, FILES);
					attachmentDialog.dismiss();
					break;
				case R.id.sketch:
					takeSketch(null);
					attachmentDialog.dismiss();
					break;
				case R.id.location:
					setAddress();
					attachmentDialog.dismiss();
					break;
			}
		}
	}
}



