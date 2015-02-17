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

import android.animation.ValueAnimator;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.omninotes.async.DeleteNoteTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.PrefUtils;


public class MainActivity extends BaseActivity implements OnDateSetListener, OnTimeSetListener {

	static final int BURGER = 0;
	static final int ARROW = 1;

	public final String FRAGMENT_DRAWER_TAG = "fragment_drawer";
	public final String FRAGMENT_LIST_TAG = "fragment_list";
	public final String FRAGMENT_DETAIL_TAG = "fragment_detail";
	public final String FRAGMENT_SKETCH_TAG = "fragment_sketch";

	private static MainActivity instance;
	private FragmentManager mFragmentManager;

	public Uri sketchUri;
	private ViewGroup croutonViewContainer;
	private Toolbar toolbar;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		instance = this;

		init();
		initUI();
	}

	private void initUI() {
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	public static MainActivity getInstance() {
		return instance;
	}


	private void init() {
		mFragmentManager = getSupportFragmentManager();

		NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) mFragmentManager.findFragmentById(R.id.navigation_drawer);
		if (mNavigationDrawerFragment == null) {
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.replace(R.id.navigation_drawer, new NavigationDrawerFragment(), FRAGMENT_DRAWER_TAG).commit();
		}

		if (mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG) == null) {
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.add(R.id.fragment_container, new ListFragment(), FRAGMENT_LIST_TAG).commit();
		}

		// Handling of Intent actions
		handleIntents();
	}


	@Override
	public void onLowMemory() {

		MainApplication.getBitmapCache().evictAll();
		super.onLowMemory();
	}


	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction() == null) {
			intent.setAction(Constants.ACTION_START_APP);
		}
		setIntent(intent);
		handleIntents();

		super.onNewIntent(intent);
	}


	public MenuItem getSearchMenuItem() {
		Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
		if (f != null) {
			return ((ListFragment) f).getSearchMenuItem();
		} else {
			return null;
		}
	}

	public void editTag(Category tag) {
		Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
		if (f != null) {
			((ListFragment) f).editCategory(tag);
		}
	}

	public void initNotesList(Intent intent) {
		Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
		if (f != null) {
			((ListFragment) f).toggleSearchLabel(false);
			((ListFragment) f).initNotesList(intent);
		}
	}

	public void commitPending() {
		Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
		if (f != null) {
			((ListFragment) f).commitPending();
		}
	}

	public void initNavigationDrawer() {
		Fragment f = checkFragmentInstance(R.id.navigation_drawer, NavigationDrawerFragment.class);
		if (f != null) ((NavigationDrawerFragment) f).init();
	}


	/**
	 * Checks if allocated fragment is of the required type and then returns it or returns null
	 */
	private Fragment checkFragmentInstance(int id, Object instanceClass) {
		Fragment result = null;
		if (mFragmentManager != null) {
			Fragment fragment = mFragmentManager.findFragmentById(id);
			if (instanceClass.equals(fragment.getClass())) {
				result = fragment;
			}
		}
		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onBackPressed()
	 *
	 * Overrides the onBackPressed behavior for the attached fragments
	 */
	public void onBackPressed() {

		Fragment f;

		// SketchFragment
		f = checkFragmentInstance(R.id.fragment_container, SketchFragment.class);
		if (f != null) {
			((SketchFragment) f).save();

			// Removes forced portrait orientation for this fragment
			setRequestedOrientation(
					ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

			mFragmentManager.popBackStack();
			return;
		}

		// DetailFragment
		f = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
		if (f != null) {
			((DetailFragment) f).goBack = true;
			((DetailFragment) f).saveAndExit((DetailFragment) f);
			return;
		}

		// ListFragment
		f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
		if (f != null) {
			// Before exiting from app the navigation drawer is opened
			if (PrefUtils.getBoolean("settings_navdrawer_on_exit", false) && getDrawerLayout() != null && !getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
				getDrawerLayout().openDrawer(GravityCompat.START);
			} else if (!PrefUtils.getBoolean("settings_navdrawer_on_exit", false) && getDrawerLayout() != null && getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
				getDrawerLayout().closeDrawer(GravityCompat.START);
			} else {
				super.onBackPressed();
			}
			return;
		}
		super.onBackPressed();
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("navigationTmp", navigationTmp);
	}


	@Override
	protected void onPause() {
		super.onPause();
		Crouton.cancelAllCroutons();
	}


	void animateBurger(int targetShape) {
		if (getDrawerToggle() != null) {
			if (targetShape != BURGER && targetShape != ARROW)
				return;
			ValueAnimator anim = ValueAnimator.ofFloat((targetShape + 1) % 2, targetShape);
			anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					float slideOffset = (Float) valueAnimator.getAnimatedValue();
					getDrawerToggle().onDrawerSlide(getDrawerLayout(), slideOffset);
				}
			});
			anim.setInterpolator(new DecelerateInterpolator());
			anim.setDuration(500);
			anim.start();
		}
	}


	public DrawerLayout getDrawerLayout() {
		return ((DrawerLayout) findViewById(R.id.drawer_layout));
	}


	public ActionBarDrawerToggle getDrawerToggle() {
		if (mFragmentManager != null && mFragmentManager.findFragmentById(R.id.navigation_drawer) != null) {
			return ((NavigationDrawerFragment) mFragmentManager.findFragmentById(R.id.navigation_drawer)).mDrawerToggle;
		} else {
			return null;
		}
	}


	/**
	 * Finishes multiselection mode started by ListFragment
	 */
	public void finishActionMode() {
		ListFragment fragment = (ListFragment) mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG);
		if (fragment != null) {
			fragment.finishActionMode();
		}
	}


	Toolbar getToolbar() {
		return this.toolbar;
	}


	private void handleIntents() {
		Intent i = getIntent();

		if (i.getAction() == null) return;

		if (receivedIntent(i)) {
			Note note = i.getParcelableExtra(Constants.INTENT_NOTE);
			if (note == null) {
				note = DbHelper.getNote(i.getIntExtra(Constants.INTENT_KEY, 0));
			}
			// Checks if the same note is already opened to avoid to open again
			if (note != null && noteAlreadyOpened(note)) {
				return;
			}
			// Empty note instantiation
			if (note == null) {
				note = new Note();
			}
			switchToDetail(note);
		}

		if (Constants.ACTION_SEND_AND_EXIT.equals(i.getAction())) {
			saveAndExit(i);
		}

		// Tag search
		if (Intent.ACTION_VIEW.equals(i.getAction())) {
			switchToList();
		}
	}

	private void saveAndExit(Intent i) {
		Note note = new Note();
		note.setTitle(i.getStringExtra(Intent.EXTRA_SUBJECT));
		note.setContent(i.getStringExtra(Intent.EXTRA_TEXT));
		DbHelper.updateNote(note, true);
		showToast(getString(R.string.note_updated), Toast.LENGTH_SHORT);
		finish();
	}


	private boolean receivedIntent(Intent i) {
		return Constants.ACTION_SHORTCUT.equals(i.getAction())
				|| Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())
				|| Constants.ACTION_WIDGET.equals(i.getAction())
				|| Constants.ACTION_TAKE_PHOTO.equals(i.getAction())
				|| ((Intent.ACTION_SEND.equals(i.getAction())
				|| Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
				|| Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
				&& i.getType() != null)
				|| i.getAction().contains(Constants.ACTION_NOTIFICATION_CLICK);
	}


	private boolean noteAlreadyOpened(Note note) {
		DetailFragment detailFragment = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
		if (detailFragment != null && detailFragment.getCurrentNote().getId() == note.getId()) {
			return true;
		}
		return false;
	}


	public void switchToList() {
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		animateTransition(transaction, TRANSITION_HORIZONTAL);
		ListFragment mListFragment = new ListFragment();
		transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack(FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
		if (getDrawerToggle() != null) {
			getDrawerToggle().setDrawerIndicatorEnabled(false);
		}
		mFragmentManager.getFragments();
	}


	public void switchToDetail(Note note) {
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		animateTransition(transaction, TRANSITION_HORIZONTAL);
		DetailFragment mDetailFragment = new DetailFragment();
		Bundle b = new Bundle();
		b.putParcelable(Constants.INTENT_NOTE, note);
		mDetailFragment.setArguments(b);
		if (mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG) == null) {
			transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG).addToBackStack(FRAGMENT_LIST_TAG).commitAllowingStateLoss();
		} else {
			transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG).addToBackStack(FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
		}

		animateBurger(ARROW);
	}


	/**
	 * Notes sharing
	 */
	public void shareNote(Note note) {

		String titleText = note.getTitle();

		String contentText = titleText
				+ System.getProperty("line.separator")
				+ note.getContent();


		Intent shareIntent = new Intent();
		// Prepare sharing intent with only text
		if (note.getAttachmentsList().size() == 0) {
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
			shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

			// Intent with single image attachment
		} else if (note.getAttachmentsList().size() == 1) {
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.setType(note.getAttachmentsList().get(0).mimeType);
			shareIntent.putExtra(Intent.EXTRA_STREAM, note.getAttachmentsList().get(0).uri);
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
			shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

			// Intent with multiple images
		} else if (note.getAttachmentsList().size() > 1) {
			shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
			ArrayList<Uri> uris = new ArrayList<Uri>();
			// A check to decide the mime type of attachments to share is done here
			HashMap<String, Boolean> mimeTypes = new HashMap<String, Boolean>();
			for (Attachment attachment : note.getAttachmentsList()) {
				uris.add(attachment.uri);
				mimeTypes.put(attachment.mimeType, true);
			}
			// If many mime types are present a general type is assigned to intent
			if (mimeTypes.size() > 1) {
				shareIntent.setType("*/*");
			} else {
				shareIntent.setType((String) mimeTypes.keySet().toArray()[0]);
			}

			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
			shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);
		}

		startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_message_chooser)));
	}


	/**
	 * Single note permanent deletion
	 *
	 * @param note Note to be deleted
	 */
	public void deleteNote(Note note) {

		// Saving changes to the note
		DeleteNoteTask deleteNoteTask = new DeleteNoteTask(getApplicationContext());
		// Removed parallel computation to avoid concurrency on pool errors
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			deleteNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, note);
//		} else {
		deleteNoteTask.execute(note);
//		}
	}


	void showMessage(int messageId, Style style) {
		showMessage(getString(messageId), style);
	}

	void showMessage(String message, Style style) {
		if (croutonViewContainer == null) {
			// ViewGroup used to show Crouton keeping compatibility with the new Toolbar
			croutonViewContainer = (ViewGroup) findViewById(R.id.crouton_handle);
		}
		Crouton.makeText(this, message, style, croutonViewContainer).show();
	}


	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		DetailFragment f = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
		if (f != null && f.isAdded()) {
			f.onTimeSetListener.onTimeSet(view, hourOfDay, minute);
		}
	}


	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
						  int dayOfMonth) {
		DetailFragment f = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
		if (f != null && f.isAdded()) {
			f.onDateSetListener.onDateSet(view, year, monthOfYear, dayOfMonth);
		}
	}
}
