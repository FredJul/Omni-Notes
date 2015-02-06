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
package it.feio.android.omninotes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.checklistview.ChecklistManager;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.checklistview.models.CheckListViewItem;
import it.feio.android.omninotes.async.AttachmentTask;
import it.feio.android.omninotes.async.SaveNoteTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.models.adapters.AttachmentAdapter;
import it.feio.android.omninotes.models.adapters.NavDrawerCategoryAdapter;
import it.feio.android.omninotes.models.adapters.PlacesAutoCompleteAdapter;
import it.feio.android.omninotes.models.listeners.OnAttachingFileListener;
import it.feio.android.omninotes.models.listeners.OnGeoUtilResultListener;
import it.feio.android.omninotes.models.listeners.OnNoteSaved;
import it.feio.android.omninotes.models.listeners.OnReminderPickedListener;
import it.feio.android.omninotes.models.views.ExpandableHeightGridView;
import it.feio.android.omninotes.utils.ConnectionManager;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.Display;
import it.feio.android.omninotes.utils.FileHelper;
import it.feio.android.omninotes.utils.GeocodeHelper;
import it.feio.android.omninotes.utils.IntentChecker;
import it.feio.android.omninotes.utils.KeyboardUtils;
import it.feio.android.omninotes.utils.StorageManager;
import it.feio.android.omninotes.utils.date.DateHelper;
import it.feio.android.omninotes.utils.date.ReminderPickers;
import it.feio.android.pixlui.links.TextLinkClickListener;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


public class DetailFragment extends Fragment implements
		OnReminderPickedListener, TextLinkClickListener, OnTouchListener, OnAttachingFileListener, TextWatcher, CheckListChangedListener, OnNoteSaved, OnGeoUtilResultListener {

	private static final int TAKE_PHOTO = 1;
	private static final int TAKE_VIDEO = 2;
	private static final int SKETCH = 4;
	private static final int TAG = 5;
	private static final int DETAIL = 6;
	private static final int FILES = 7;
	public OnDateSetListener onDateSetListener;
	public OnTimeSetListener onTimeSetListener;
	public boolean goBack = false;
	MediaRecorder mRecorder = null;
	// Toggle checklist view
	View toggleChecklistView;
	private LinearLayout reminder_layout;
	private TextView datetime;
	private Uri attachmentUri;
	private AttachmentAdapter mAttachmentAdapter;
	private ExpandableHeightGridView mGridView;
	private PopupWindow attachmentDialog;
	private EditText title, content;
	private TextView locationTextView;
	private Note note;
	private Note noteTmp;
	private Note noteOriginal;
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
	private Style exitCroutonStyle = ONStyle.CONFIRM;
	// Flag to check if after editing it will return to ListActivity or not
	// and in the last case a Toast will be shown instead than Crouton
	private boolean afterSavedReturnsToList = true;
	private boolean swiping;
	private ViewGroup root;
	private int startSwipeX;
	private SharedPreferences prefs;
	private View timestampsView;
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
		prefs = getMainActivity().prefs;
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
			noteTmp = savedInstanceState.getParcelable("noteTmp");
			note = savedInstanceState.getParcelable("note");
			noteOriginal = savedInstanceState.getParcelable("noteOriginal");
			attachmentUri = savedInstanceState.getParcelable("attachmentUri");
			orientationChanged = savedInstanceState.getBoolean("orientationChanged");
		}

		// Added the sketched image if present returning from SketchFragment
		if (getMainActivity().sketchUri != null) {
			Attachment mAttachment = new Attachment(getMainActivity().sketchUri, Constants.MIME_TYPE_SKETCH);
			noteTmp.getAttachmentsList().add(mAttachment);
			getMainActivity().sketchUri = null;
			// Removes previous version of edited image
			if (sketchEdited != null) {
				noteTmp.getAttachmentsList().remove(sketchEdited);
				sketchEdited = null;
			}
		}

		init();

		setHasOptionsMenu(true);
		setRetainInstance(false);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());
		outState.putParcelable("noteTmp", noteTmp);
		outState.putParcelable("note", note);
		outState.putParcelable("noteOriginal", noteOriginal);
		outState.putParcelable("attachmentUri", attachmentUri);
		outState.putBoolean("orientationChanged", orientationChanged);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();

		// Checks "goBack" value to avoid performing a double saving
		if (!goBack) {
			saveNote(this);
		}

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

		if (noteOriginal == null) {
			noteOriginal = getArguments().getParcelable(Constants.INTENT_NOTE);
		}

		if (note == null) {
			note = new Note(noteOriginal);
		}

		if (noteTmp == null) {
			noteTmp = new Note(note);
		}

		if (noteTmp.getAlarm() != null) {
			dateTimeText = initReminder(Long.parseLong(noteTmp.getAlarm()));
		}

		initViews();
	}

	private void handleIntents() {
		Intent i = getActivity().getIntent();

		if (Constants.ACTION_MERGE.equals(i.getAction())) {
			noteOriginal = new Note();
			note = new Note(noteOriginal);
			noteTmp = getArguments().getParcelable(Constants.INTENT_NOTE);
			i.setAction(null);
		}

		// Action called from home shortcut
		if (Constants.ACTION_SHORTCUT.equals(i.getAction())
				|| Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())) {
			afterSavedReturnsToList = false;
			noteOriginal = DbHelper.getInstance(getActivity()).getNote(i.getIntExtra(Constants.INTENT_KEY, 0));
			// Checks if the note pointed from the shortcut has been deleted
			if (noteOriginal == null) {
				getMainActivity().showToast(getText(R.string.shortcut_note_deleted), Toast.LENGTH_LONG);
				getActivity().finish();
			}
			note = new Note(noteOriginal);
			noteTmp = new Note(noteOriginal);
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
					String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
					String pattern = DbHelper.KEY_CATEGORY + " = ";
					if (sqlCondition.lastIndexOf(pattern) != -1) {
						String tagId = sqlCondition.substring(sqlCondition.lastIndexOf(pattern) + pattern.length()).trim();
						Category tag;
						try {
							tag = DbHelper.getInstance(getActivity()).getCategory(Integer.parseInt(tagId));
							noteTmp = new Note();
							noteTmp.setCategory(tag);
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

			if (noteTmp == null) noteTmp = new Note();

			// Text title
			String title = i.getStringExtra(Intent.EXTRA_SUBJECT);
			if (title != null) {
				noteTmp.setTitle(title);
			}

			// Text content
			String content = i.getStringExtra(Intent.EXTRA_TEXT);
			if (content != null) {
				noteTmp.setContent(content);
			}

			// Single attachment data
			Uri uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
			// Due to the fact that Google Now passes intent as text but with
			// audio recording attached the case must be handled in specific way
			if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(i.getAction())) {
//		    	String mimeType = StorageManager.getMimeTypeInternal(((MainActivity)getActivity()), i.getType());
//		    	Attachment mAttachment = new Attachment(uri, mimeType);
//		    	if (Constants.MIME_TYPE_FILES.equals(mimeType)) {
//			    	mAttachment.setName(uri.getLastPathSegment());
//		    	}
//		    	noteTmp.addAttachment(mAttachment);
				String name = FileHelper.getNameFromUri(getActivity(), uri);
				AttachmentTask task = new AttachmentTask(this, uri, name, this);
				task.execute();
			}

			// Multiple attachment data
			ArrayList<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (uris != null) {
				for (Uri uriSingle : uris) {
					String name = FileHelper.getNameFromUri(getActivity(), uriSingle);
					AttachmentTask task = new AttachmentTask(this, uriSingle, name, this);
					task.execute();
				}
			}

			i.setAction(null);
		}

	}

	private void initViews() {

		// Sets onTouchListener to the whole activity to swipe notes
		root = (ViewGroup) getView().findViewById(R.id.detail_root);
		root.setOnTouchListener(this);

		// ScrollView container
		scrollView = (ScrollView) getView().findViewById(R.id.content_wrapper);

		// Color of tag marker if note is tagged a function is active in preferences
		setTagMarkerColor(noteTmp.getCategory());

		// Sets links clickable in title and content Views
		title = initTitle();
		requestFocus(title);

		content = initContent();

		// Initialization of location TextView
		locationTextView = (TextView) getView().findViewById(R.id.location);

		if (isNoteLocationValid()) {
			if (!TextUtils.isEmpty(noteTmp.getAddress())) {
				locationTextView.setVisibility(View.VISIBLE);
				locationTextView.setText(noteTmp.getAddress());
			} else {
				GeocodeHelper.getAddressFromCoordinates(getActivity(), noteTmp.getLatitude(), noteTmp.getLongitude(), mFragment);
			}
		} else {
		}

		locationTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String uriString = "geo:" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude()
						+ "?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude();
				Intent locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
				if (!IntentChecker.isAvailable(getActivity(), locationIntent, null)) {
					uriString = "http://maps.google.com/maps?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude();
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
						noteTmp.setLatitude("");
						noteTmp.setLongitude("");
						fade(locationTextView, false);
					}
				});
				MaterialDialog dialog = builder.build();
				dialog.show();
				return true;
			}
		});


		// Some fields can be filled by third party application and are always
		// shown
		mGridView = (ExpandableHeightGridView) getView().findViewById(R.id.gridview);
		mAttachmentAdapter = new AttachmentAdapter(getActivity(), noteTmp.getAttachmentsList(), mGridView);
		mAttachmentAdapter.setOnErrorListener(this);

		// Initialzation of gridview for images
		mGridView.setAdapter(mAttachmentAdapter);
		mGridView.autoresize();

		// Click events for images in gridview (zooms image)
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Attachment attachment = (Attachment) parent.getAdapter().getItem(position);
				Uri uri = attachment.getUri();
				if (Constants.MIME_TYPE_AUDIO.equals(attachment.getMime_type())) {
					playback(v, uri);
				}
				else {
					Intent attachmentIntent = new Intent(Intent.ACTION_VIEW);
					attachmentIntent.setDataAndType(uri, StorageManager.getMimeType(getActivity(), attachment.getUri()));
					attachmentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					if (IntentChecker.isAvailable(getActivity().getApplicationContext(), attachmentIntent, null)) {
						startActivity(attachmentIntent);
					} else {
						getMainActivity().showMessage(R.string.feature_not_available_on_this_device, ONStyle.WARN);
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
				if (Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).getMime_type())) {
					dialogBuilder
							.content(R.string.delete_selected_image)
							.negativeText(R.string.edit)
							.callback(new MaterialDialog.Callback() {
								@Override
								public void onPositive(MaterialDialog materialDialog) {
									noteTmp.getAttachmentsList().remove(position);
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
									noteTmp.getAttachmentsList().remove(position);
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
		reminder_layout = (LinearLayout) getView().findViewById(R.id.reminder_layout);
		reminder_layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int pickerType = prefs.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP : ReminderPickers.TYPE_GOOGLE;
				ReminderPickers reminderPicker = new ReminderPickers(getActivity(), mFragment, pickerType);
				Long presetDateTime = noteTmp.getAlarm() != null ? Long.parseLong(noteTmp.getAlarm()) : null;
				reminderPicker.pick(presetDateTime);
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
								noteTmp.setAlarm(null);
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
		timestampsView = getActivity().findViewById(R.id.detail_timestamps);
		// Bottom padding set for translucent navbar in Kitkat
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int navBarHeight = Display.getNavigationBarHeightKitkat(getActivity());
			int timestampsViewPaddingBottom = navBarHeight > 0 ? navBarHeight - 22 : timestampsView.getPaddingBottom();
			timestampsView.setPadding(timestampsView.getPaddingStart(), timestampsView.getPaddingTop(),
					timestampsView.getPaddingEnd(), timestampsViewPaddingBottom);
		}

		// Footer dates of creation...
		TextView creationTextView = (TextView) getView().findViewById(R.id.creation);
		String creation = noteTmp.getCreationShort(getActivity());
		creationTextView.append(creation.length() > 0 ? getString(R.string.creation) + " "
				+ creation : "");
		if (creationTextView.getText().length() == 0)
			creationTextView.setVisibility(View.GONE);

		// ... and last modification
		TextView lastModificationTextView = (TextView) getView().findViewById(R.id.last_modification);
		String lastModification = noteTmp.getLastModificationShort(getActivity());
		lastModificationTextView.append(lastModification.length() > 0 ? getString(R.string.last_update) + " "
				+ lastModification : "");
		if (lastModificationTextView.getText().length() == 0)
			lastModificationTextView.setVisibility(View.GONE);
	}

	private EditText initTitle() {
		EditText title = (EditText) getView().findViewById(R.id.detail_title);
		title.setText(noteTmp.getTitle());
		title.gatherLinksForText();
		title.setOnTextLinkClickListener(this);
		// To avoid dropping here the  dragged checklist items
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
		content.setText(noteTmp.getContent());
		content.gatherLinksForText();
		content.setOnTextLinkClickListener(this);
		// Avoids focused line goes under the keyboard
		content.addTextChangedListener(this);

		// Restore checklist
		toggleChecklistView = content;
		if (noteTmp.isChecklist()) {
			noteTmp.setChecklist(false);
			toggleChecklistView.setAlpha(0);
			toggleChecklist2();
		}

		return content;
	}


	/**
	 * Force focus and shows soft keyboard
	 */
	private void requestFocus(final EditText view) {
		if (note.get_id() == 0 && !noteTmp.isChanged(note)) {
			KeyboardUtils.showKeyboard(view);
		}
	}


	/**
	 * Colors tag marker in note's title and content elements
	 */
	private void setTagMarkerColor(Category tag) {

		String colorsPref = prefs.getString("settings_colors_app", Constants.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!colorsPref.equals("disabled")) {

			// Choosing target view depending on another preference
			ArrayList<View> target = new ArrayList<View>();
			if (colorsPref.equals("complete")) {
				target.add(getView().findViewById(R.id.title_wrapper));
				target.add(getView().findViewById(R.id.content_wrapper));
			} else {
				target.add(getView().findViewById(R.id.tag_marker));
			}

			// Coloring the target
			if (tag != null && tag.getColor() != null) {
				for (View view : target) {
					view.setBackgroundColor(Integer.parseInt(tag.getColor()));
				}
			} else {
				for (View view : target) {
					view.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}
	}

	private void setAddress() {
		if (!ConnectionManager.internetAvailable(getActivity())) {
			noteTmp.setLatitude(getMainActivity().currentLatitude);
			noteTmp.setLongitude(getMainActivity().currentLongitude);
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
							noteTmp.setLatitude(lat);
							noteTmp.setLongitude(lon);
							GeocodeHelper.getAddressFromCoordinates(getActivity(), noteTmp.getLatitude(),
									noteTmp.getLongitude(), mFragment);
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
			if (!isNoteLocationValid()) {
				getMainActivity().showMessage(R.string.location_not_found, ONStyle.ALERT);
				return;
			}
			address = noteTmp.getLatitude() + ", " + noteTmp.getLongitude();
		}
		if (!GeocodeHelper.areCoordinates(address)) {
			noteTmp.setAddress(address);
		}
		locationTextView.setVisibility(View.VISIBLE);
		locationTextView.setText(address);
		fade(locationTextView, true);
	}


	@Override
	public void onCoordinatesResolved(double[] coords) {
		if (coords != null) {
			noteTmp.setLatitude(coords[0]);
			noteTmp.setLongitude(coords[1]);
			GeocodeHelper.getAddressFromCoordinates(getActivity(), coords[0], coords[1], new OnGeoUtilResultListener() {
				@Override
				public void onAddressResolved(String address) {
					if (!GeocodeHelper.areCoordinates(address)) {
						noteTmp.setAddress(address);
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
			getMainActivity().showMessage(R.string.location_not_found, ONStyle.ALERT);
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

		boolean newNote = noteTmp.get_id() == 0;

		menu.findItem(R.id.menu_checklist_on).setVisible(!noteTmp.isChecklist());
		menu.findItem(R.id.menu_checklist_off).setVisible(noteTmp.isChecklist());
		// If note is trashed only this options will be available from menu
		if (noteTmp.isTrashed()) {
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
			getMainActivity().animateBurger(getMainActivity().BURGER);
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
				shareNote();
				break;
			case R.id.menu_checklist_on:
				toggleChecklist();
				break;
			case R.id.menu_checklist_off:
				toggleChecklist();
				break;
			case R.id.menu_trash:
				trashNote(true);
				break;
			case R.id.menu_untrash:
				trashNote(false);
				break;
			case R.id.menu_discard_changes:
				discard();
				break;
			case R.id.menu_delete:
				deleteNote();
				break;
		}
		return super.onOptionsItemSelected(item);
	}


	private void navigateUp() {
		afterSavedReturnsToList = true;
		saveAndExit(this);
	}

	private void toggleChecklist() {

		// In case checklist is active a prompt will ask about many options
		// to decide hot to convert back to simple text
		if (!noteTmp.isChecklist()) {
			toggleChecklist2();
			return;
		}

		// If checklist is active but no items are checked the conversion in done automatically
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
		keepChecked.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true));
		keepCheckmarks.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true));

		new MaterialDialog.Builder(getActivity())
				.customView(layout)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						prefs.edit()
								.putBoolean(Constants.PREF_KEEP_CHECKED, keepChecked.isChecked())
								.putBoolean(Constants.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked())
								.commit();

						toggleChecklist2();
					}
				}).build().show();
	}


	/**
	 * Toggles checklist view
	 */
	private void toggleChecklist2() {
		boolean keepChecked = prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true);
		boolean showChecks = prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true);
		toggleChecklist2(keepChecked, showChecks);
	}

	private void toggleChecklist2(final boolean keepChecked, final boolean showChecks) {
		// Get instance and set options to convert EditText to CheckListView
		mChecklistManager = ChecklistManager.getInstance(getActivity());
		mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(prefs.getString("settings_checked_items_behavior",
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
			noteTmp.setChecklist(!noteTmp.isChecklist());
		}
	}


	/**
	 * Categorize note choosing from a list of previously created categories
	 */
	private void categorizeNote() {
		// Retrieves all available categories
		final ArrayList<Category> categories = DbHelper.getInstance(getActivity()).getCategories();

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
						startActivityForResult(intent, TAG);
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						noteTmp.setCategory(null);
						setTagMarkerColor(null);
					}
				})
				.build();

		dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				noteTmp.setCategory(categories.get(position));
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
			getMainActivity().showMessage(R.string.error, ONStyle.ALERT);

		}
	}

	private void takePhoto() {
		// Checks for camera app available
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (!IntentChecker.isAvailable(getActivity(), intent, new String[]{PackageManager.FEATURE_CAMERA})) {
			getMainActivity().showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// Checks for created file validity
		File f = StorageManager.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_IMAGE_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, ONStyle.ALERT);
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
			getMainActivity().showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// File is stored in custom ON folder to speedup the attachment
			File f = StorageManager.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_VIDEO_EXT);
			if (f == null) {
				getMainActivity().showMessage(R.string.error, ONStyle.ALERT);

				return;
			}
			attachmentUri = Uri.fromFile(f);
			takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);

		String maxVideoSizeStr = "".equals(prefs.getString("settings_max_video_size", "")) ? "0" : prefs.getString("settings_max_video_size", "");
		int maxVideoSize = Integer.parseInt(maxVideoSizeStr);
		takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.valueOf(maxVideoSize * 1024 * 1024));
		startActivityForResult(takeVideoIntent, TAKE_VIDEO);
	}

	private void takeSketch(Attachment attachment) {

		File f = StorageManager.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_SKETCH_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		attachmentUri = Uri.fromFile(f);

		// Forces potrait orientation to this fragment only
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Fragments replacing
		FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
		getMainActivity().animateTransition(transaction, getMainActivity().TRANSITION_HORIZONTAL);
		SketchFragment mSketchFragment = new SketchFragment();
		Bundle b = new Bundle();
		b.putParcelable(MediaStore.EXTRA_OUTPUT, attachmentUri);
		if (attachment != null) {
			b.putParcelable("base", attachment.getUri());
		}
		mSketchFragment.setArguments(b);
		transaction.replace(R.id.fragment_container, mSketchFragment, getMainActivity().FRAGMENT_SKETCH_TAG).addToBackStack(getMainActivity().FRAGMENT_DETAIL_TAG).commit();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Fetch uri from activities, store into adapter and refresh adapter
		Attachment attachment;
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case TAKE_PHOTO:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_IMAGE);
					noteTmp.getAttachmentsList().add(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case TAKE_VIDEO:
						attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_VIDEO);
					noteTmp.getAttachmentsList().add(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case FILES:
					onActivityResultManageReceivedFiles(intent);
					break;
				case SKETCH:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_SKETCH);
					noteTmp.getAttachmentsList().add(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case TAG:
					getMainActivity().showMessage(R.string.category_saved, ONStyle.CONFIRM);
					Category tag = intent.getParcelableExtra("tag");
					noteTmp.setCategory(tag);
					setTagMarkerColor(tag);
					break;
				case DETAIL:
					getMainActivity().showMessage(R.string.note_updated, ONStyle.CONFIRM);
					break;
			}
		}
	}

	private void onActivityResultManageReceivedFiles(Intent intent) {
		List<Uri> uris = new ArrayList<Uri>();
		if (intent.getClipData() != null) {
			for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
				uris.add(intent.getClipData().getItemAt(i).getUri());
			}
		} else {
			uris.add(intent.getData());
		}
		for (Uri uri : uris) {
			String name = FileHelper.getNameFromUri(getActivity(), uri);
			new AttachmentTask(this, uri, name, this).execute();
		}
	}


	/**
	 * Discards changes done to the note and eventually delete new attachments
	 */
	private void discard() {
		// Checks if some new files have been attached and must be removed
		if (!noteTmp.getAttachmentsList().equals(note.getAttachmentsList())) {
			for (Attachment newAttachment : noteTmp.getAttachmentsList()) {
				if (!note.getAttachmentsList().contains(newAttachment)) {
					StorageManager.delete(getActivity(), newAttachment.getUri().getPath());
				}
			}
		}

		goBack = true;

		if (!noteTmp.equals(noteOriginal)) {
			// Restore original status of the note
			if (noteOriginal.get_id() == 0) {
				getMainActivity().deleteNote(noteTmp);
				goHome();
			} else {
				SaveNoteTask saveNoteTask = new SaveNoteTask(this, this, false);
					saveNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteOriginal);
			}
			MainActivity.notifyAppWidgets(getActivity());
		} else {
			goHome();
		}
	}

	private void trashNote(boolean trash) {
		// Simply go back if is a new note
		if (noteTmp.get_id() == 0) {
			goHome();
			return;
		}

		noteTmp.setTrashed(trash);
		goBack = true;
		exitMessage = trash ? getString(R.string.note_trashed) : getString(R.string.note_untrashed);
		exitCroutonStyle = trash ? ONStyle.WARN : ONStyle.INFO;
		saveNote(this);
	}

	private void deleteNote() {
		// Confirm dialog creation
		new MaterialDialog.Builder(getActivity())
				.content(R.string.delete_note_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.SimpleCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						getMainActivity().deleteNote(noteTmp);

						getMainActivity().showMessage(R.string.note_deleted, ONStyle.ALERT);
						MainActivity.notifyAppWidgets(getActivity());
						goHome();
					}
				}).build().show();
	}

	public void saveAndExit(OnNoteSaved mOnNoteSaved) {
		exitMessage = getString(R.string.note_updated);
		exitCroutonStyle = ONStyle.CONFIRM;
		goBack = true;
		saveNote(mOnNoteSaved);
	}


	/**
	 * Save new notes, modify them or archive
	 */
	void saveNote(OnNoteSaved mOnNoteSaved) {

		// Changed fields
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());

		// Check if some text or attachments of any type have been inserted or
		// is an empty note
		if (goBack && TextUtils.isEmpty(noteTmp.getTitle()) && TextUtils.isEmpty(noteTmp.getContent())
				&& noteTmp.getAttachmentsList().size() == 0) {

			exitMessage = getString(R.string.empty_note_not_saved);
			exitCroutonStyle = ONStyle.INFO;
			goHome();
			return;
		}

		if (saveNotNeeded()) return;

		noteTmp.setAttachmentsListOld(note.getAttachmentsList());

		// Saving changes to the note
		SaveNoteTask saveNoteTask = new SaveNoteTask(this, mOnNoteSaved, lastModificationUpdatedNeeded());
		saveNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteTmp);
	}


	/**
	 * Checks if nothing is changed to avoid committing if possible (check)
	 */
	private boolean saveNotNeeded() {
		if (!noteTmp.isChanged(note)) {
			exitMessage = "";
			onNoteSaved(noteTmp);
			return true;
		}
		return false;
	}


	/**
	 * Checks if only tag, archive or trash status have been changed
	 * and then force to not update last modification date*
	 */
	private boolean lastModificationUpdatedNeeded() {
		note.setCategory(noteTmp.getCategory());
		note.setTrashed(noteTmp.isTrashed());
		note.setLocked(noteTmp.isLocked());
		return noteTmp.isChanged(note);
	}


	@Override
	public void onNoteSaved(Note noteSaved) {
		MainActivity.notifyAppWidgets(OmniNotes.getAppContext());
		note = new Note(noteSaved);
		if (goBack) {
			goHome();
		}
	}

	private String getNoteTitle() {
		String res;
		if (getActivity() != null && getActivity().findViewById(R.id.detail_title) != null) {
			Editable editableTitle = ((EditText) getActivity().findViewById(R.id.detail_title)).getText();
			res = TextUtils.isEmpty(editableTitle) ? "" : editableTitle.toString();
		} else {
			res = title.getText() != null ? title.getText().toString() : "";
		}
		return res;
	}

	private String getNoteContent() {
		String content = "";
		if (!noteTmp.isChecklist()) {
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
	private void shareNote() {
		Note sharedNote = new Note(noteTmp);
		sharedNote.setTitle(getNoteTitle());
		sharedNote.setContent(getNoteContent());
		getMainActivity().shareNote(sharedNote);
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
				recordingBitmap = ((GlideBitmapDrawable)d.getCurrent()).getBitmap();
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
		File f = StorageManager.createNewAttachmentFile(getActivity(), Constants.MIME_TYPE_AUDIO_EXT);
		if (f == null) {
			getMainActivity().showMessage(R.string.error, ONStyle.ALERT);

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
							getMainActivity().showMessage(R.string.no_application_can_perform_this_action, ONStyle.ALERT);

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
		int y = (int) event.getY();

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
						b.putParcelable(Constants.INTENT_NOTE, new Note());
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
		getMainActivity().showMessage(R.string.error_saving_attachments, ONStyle.ALERT);
		if (noteTmp.getAttachmentsList().contains(mAttachment)) {
			noteTmp.getAttachmentsList().remove(mAttachment);
			mAttachmentAdapter.notifyDataSetChanged();
			mGridView.autoresize();
		}
	}

	@Override
	public void onAttachingFileFinished(Attachment mAttachment) {
		noteTmp.getAttachmentsList().add(mAttachment);
		mAttachmentAdapter.notifyDataSetChanged();
		mGridView.autoresize();
	}

	@Override
	public void onReminderPicked(long reminder) {
		noteTmp.setAlarm(reminder);
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
		if (noteTmp.isChecklist()) {
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
	public Note getCurrentNote() {
		return note;
	}

	private boolean isNoteLocationValid() {
		return noteTmp.getLatitude() != null
				&& noteTmp.getLatitude() != 0
				&& noteTmp.getLongitude() != null
				&& noteTmp.getLongitude() != 0;
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
						Attachment attachment = new Attachment(Uri.parse(recordName), Constants.MIME_TYPE_AUDIO);
						attachment.setLength(audioRecordingTime);
						noteTmp.getAttachmentsList().add(attachment);
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



