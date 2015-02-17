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
package net.fred.taskgame.model.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.listeners.OnAttachingFileListener;
import net.fred.taskgame.utils.BitmapHelper;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.date.DateHelper;
import net.fred.taskgame.view.ExpandableHeightGridView;
import net.fred.taskgame.view.SquareImageView;

import java.util.ArrayList;
import java.util.List;


public class AttachmentAdapter extends BaseAdapter {

	private Activity mActivity;
	private List<Attachment> attachmentsList = new ArrayList<>();
	private LayoutInflater inflater;

	public AttachmentAdapter(Activity mActivity, List<Attachment> attachmentsList, ExpandableHeightGridView mGridView) {
		this.mActivity = mActivity;
		this.attachmentsList = attachmentsList;
		ExpandableHeightGridView mGridView1 = mGridView;
		this.inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return attachmentsList.size();
	}

	public Attachment getItem(int position) {
		return attachmentsList.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}


	public View getView(int position, View convertView, ViewGroup parent) {
		Attachment mAttachment = attachmentsList.get(position);

		AttachmentHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.gridview_item, parent, false);

			holder = new AttachmentHolder();
			holder.image = (SquareImageView) convertView.findViewById(R.id.gridview_item_picture);
			holder.text = (TextView) convertView.findViewById(R.id.gridview_item_text);
			convertView.setTag(holder);
		} else {
			holder = (AttachmentHolder) convertView.getTag();
		}

		// Draw name in case the type is an audio recording
		if (mAttachment.mimeType != null && mAttachment.mimeType.equals(Constants.MIME_TYPE_AUDIO)) {
			String text;

			if (mAttachment.length > 0) {
				// Recording duration
				text = DateHelper.formatShortTime(mActivity, mAttachment.length);
			} else {
				// Recording date otherwise
				text = DateHelper.getLocalizedDateTime(mActivity, mAttachment
								.uri.getLastPathSegment().split("\\.")[0],
						Constants.DATE_FORMAT_SORTABLE);
			}

			if (text == null) {
				text = mActivity.getString(R.string.attachment);
			}
			holder.text.setText(text);
			holder.text.setVisibility(View.VISIBLE);
		} else {
			holder.text.setVisibility(View.GONE);
		}

		// Draw name in case the type is an audio recording (or file in the future)
		if (mAttachment.mimeType != null && mAttachment.mimeType.equals(Constants.MIME_TYPE_FILES)) {
			holder.text.setText(mAttachment.name);
			holder.text.setVisibility(View.VISIBLE);
		}

		// Starts the AsyncTask to draw bitmap into ImageView
//		loadThumbnail(holder, mAttachment);
		Uri thumbnailUri = BitmapHelper.getThumbnailUri(mActivity, mAttachment);
		Glide.with(mActivity)
				.load(thumbnailUri)
				.centerCrop()
				.crossFade()
				.into(holder.image);

		return convertView;
	}

	public class AttachmentHolder {
		TextView text;
		SquareImageView image;
	}


	public void setOnErrorListener(OnAttachingFileListener listener) {
		OnAttachingFileListener mOnAttachingFileErrorListener = listener;
	}

}
