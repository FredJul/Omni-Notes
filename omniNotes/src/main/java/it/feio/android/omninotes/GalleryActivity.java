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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import java.util.ArrayList;

import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.listeners.OnViewTouchedListener;
import it.feio.android.omninotes.models.views.InterceptorFrameLayout;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.FileHelper;
import it.feio.android.omninotes.utils.StorageManager;
import it.feio.android.simplegallery.models.GalleryPagerAdapter;
import it.feio.android.simplegallery.views.GalleryViewPager;

public class GalleryActivity extends ActionBarActivity {

	private GalleryViewPager mViewPager;

	private ArrayList<Attachment> images;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_gallery);
		findViewById(R.id.fullscreen_content);

		initViews();
		initData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_gallery, menu);
		return true;
	}


	private void initViews() {

		// Show the Up button in the action bar.
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowTitleEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		((InterceptorFrameLayout) findViewById(R.id.gallery_root)).setOnViewTouchedListener(screenTouches);

		mViewPager = (GalleryViewPager) findViewById(R.id.fullscreen_content);
//		mViewPager.setPageTransformer(GalleryViewPager.PAGE_TRANSFORMER_DEPTH);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				getSupportActionBar().setSubtitle("(" + (arg0 + 1) + "/" + images.size() + ")");
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}


	/**
	 * Initializes data received from note detail screen
	 */
	private void initData() {
		String title = getIntent().getStringExtra(Constants.GALLERY_TITLE);
		images = getIntent().getParcelableArrayListExtra(Constants.GALLERY_IMAGES);
		int clickedImage = getIntent().getIntExtra(Constants.GALLERY_CLICKED_IMAGE, 0);

		ArrayList<String> imagesPaths = new ArrayList<String>();
		for (Attachment mAttachment : images) {
			Uri uri = mAttachment.getUri();
			imagesPaths.add(FileHelper.getPath(this, uri));
		}

//		FilePagerAdapter  pagerAdapter = new FilePagerAdapter (this, imagesPaths);  
		GalleryPagerAdapter pagerAdapter = new GalleryPagerAdapter(this, imagesPaths);
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			mViewPager.setPageTransformer(false, new DepthPageTransformer());
//		}
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setAdapter(pagerAdapter);
		mViewPager.setCurrentItem(clickedImage);

		getSupportActionBar().setTitle(title);
		getSupportActionBar().setSubtitle("(" + (clickedImage + 1) + "/" + images.size() + ")");

		// If selected attachment is a video it will be immediately played
		if (images.get(clickedImage).getMime_type().equals(Constants.MIME_TYPE_VIDEO)) {
			viewMedia();
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.menu_gallery_share: {
				shareMedia();
				break;
			}
			case R.id.menu_gallery: {
				viewMedia();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}


	private void viewMedia() {
		Attachment attachment = images.get(mViewPager.getCurrentItem());
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(attachment.getUri(),
				StorageManager.getMimeType(this, attachment.getUri()));
		startActivity(intent);
	}


	private void shareMedia() {
		Attachment attachment = images.get(mViewPager.getCurrentItem());
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(StorageManager.getMimeType(this, attachment.getUri()));
		intent.putExtra(Intent.EXTRA_STREAM, attachment.getUri());
		startActivity(intent);
	}

	OnViewTouchedListener screenTouches = new OnViewTouchedListener() {

		private final int MOVING_THRESHOLD = 30;

		float x;
		float y;
		private boolean status_pressed = false;

		@Override
		public void onViewTouchOccurred(MotionEvent ev) {
			if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
				x = ev.getX();
				y = ev.getY();
				status_pressed = true;
			}
			if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
				float dx = Math.abs(x - ev.getX());
				float dy = Math.abs(y - ev.getY());
				double dxy = Math.sqrt(dx * dx + dy * dy);

				if (dxy >= MOVING_THRESHOLD) {
					status_pressed = false;
				}
			}
			if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
				if (status_pressed) {
					click();
					status_pressed = false;
				}
			}
		}

		private void click() {
			Attachment attachment = images.get(mViewPager.getCurrentItem());
			if (attachment.getMime_type().equals(Constants.MIME_TYPE_VIDEO)) {
				viewMedia();
			}
		}
	};
}
