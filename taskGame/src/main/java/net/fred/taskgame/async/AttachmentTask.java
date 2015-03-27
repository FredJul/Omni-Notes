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

package net.fred.taskgame.async;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.listeners.OnAttachingFileListener;
import net.fred.taskgame.utils.StorageHelper;

import java.lang.ref.WeakReference;


public class AttachmentTask extends AsyncTask<Void, Void, Attachment> {

	private final WeakReference<Fragment> mFragmentWeakReference;
	private final Activity mActivity;
	private OnAttachingFileListener mOnAttachingFileListener;
	private Uri uri;

	public AttachmentTask(Fragment mFragment, Uri uri, OnAttachingFileListener mOnAttachingFileListener) {
		mFragmentWeakReference = new WeakReference<>(mFragment);
		this.uri = uri;
		this.mOnAttachingFileListener = mOnAttachingFileListener;
		this.mActivity = mFragment.getActivity();
	}


	@Override
	protected Attachment doInBackground(Void... params) {
		return StorageHelper.createAttachmentFromUri(mActivity, uri);
	}


	@Override
	protected void onPostExecute(Attachment attachment) {
		if (isAlive()) {
			if (attachment != null) {
				mOnAttachingFileListener.onAttachingFileFinished(attachment);
			} else {
				mOnAttachingFileListener.onAttachingFileErrorOccurred(attachment);
			}
		} else {
			if (attachment != null) {
				StorageHelper.delete(mActivity, attachment.uri.getPath());
			}
		}
	}


	private boolean isAlive() {
		return mFragmentWeakReference != null
				&& mFragmentWeakReference.get() != null
				&& mFragmentWeakReference.get().isAdded()
				&& mFragmentWeakReference.get().getActivity() != null
				&& !mFragmentWeakReference.get().getActivity().isFinishing();
	}

}