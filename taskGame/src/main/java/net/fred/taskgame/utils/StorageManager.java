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
package net.fred.taskgame.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class StorageManager {

	public static boolean checkStorage() {
		boolean mExternalStorageAvailable;
		boolean mExternalStorageWriteable;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageAvailable && mExternalStorageWriteable;
	}

	/**
	 * Create a path where we will place our private file on external
	 *
	 * @param mContext
	 * @param uri
	 * @return
	 */
	public static File createExternalStoragePrivateFile(Context mContext, Uri uri, String extension) {

		// Checks for external storage availability
		if (!checkStorage()) {
			Toast.makeText(mContext, mContext.getString(R.string.storage_not_available), Toast.LENGTH_SHORT).show();
			return null;
		}
		File file = createNewAttachmentFile(mContext, extension);

		InputStream is;
		OutputStream os;
		try {
			is = mContext.getContentResolver().openInputStream(uri);
			os = new FileOutputStream(file);
			copyFile(is, os);
		} catch (IOException e) {
			try {
//				InputStream is = new FileInputStream(uri.getPath());
				is = new FileInputStream(FileHelper.getPath(mContext, uri));
				os = new FileOutputStream(file);
				copyFile(is, os);
				// It's a path!!
			} catch (NullPointerException e1) {
				try {
					is = new FileInputStream(uri.getPath());
					os = new FileOutputStream(file);
					copyFile(is, os);
				} catch (FileNotFoundException e2) {

					file = null;
				}
			} catch (FileNotFoundException e2) {

				file = null;
			}
		}
		return file;
	}

	public static boolean deleteExternalStoragePrivateFile(Context mContext, String name) {
		boolean res = false;

		// Checks for external storage availability
		if (!checkStorage()) {
			Toast.makeText(mContext, mContext.getString(R.string.storage_not_available), Toast.LENGTH_SHORT).show();
			return res;
		}

		File file = new File(mContext.getExternalFilesDir(null), name);
		if (file != null) {
			file.delete();
			res = true;
		}

		return res;
	}

	public static boolean copyFile(InputStream is, OutputStream os) {
		boolean res = false;
		byte[] data = new byte[1024];
		int len;
		try {
			while ((len = is.read(data)) > 0) {
				os.write(data, 0, len);
			}
			is.close();
			os.close();
			res = true;
		} catch (IOException e) {

		}
		return res;
	}

	public static boolean delete(Context mContext, String name) {
		boolean res = false;

		// Checks for external storage availability
		if (!checkStorage()) {
			Toast.makeText(mContext, mContext.getString(R.string.storage_not_available), Toast.LENGTH_SHORT).show();
			return res;
		}

		File file = new File(name);
		if (file != null) {
			if (file.isFile()) {
				res = file.delete();
			} else if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (File file2 : files) {
					res = delete(mContext, file2.getAbsolutePath());
				}
				res = file.delete();
			}
		}

		return res;
	}


	public static String getRealPathFromURI(Context mContext, Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};
		Cursor cursor = null;
		try {
			mContext.getContentResolver().query(contentUri, proj, null, null, null);
		} catch (Exception e) {
		}
		if (cursor == null) {
			return null;
		}
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}


	public static File createNewAttachmentFile(Context mContext, String extension) {
		File f = null;
		if (checkStorage()) {
			f = new File(mContext.getExternalFilesDir(null), createNewAttachmentName(extension));
		}
		return f;
	}


	public static String createNewAttachmentName(String extension) {
		Calendar now = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_SORTABLE);
		String name = sdf.format(now.getTime());
		name += extension != null ? extension : "";
		return name;
	}

	/**
	 * Retrieves uri mime-type using ContentResolver
	 *
	 * @param mContext
	 * @param uri
	 * @return
	 */
	public static String getMimeType(Context mContext, Uri uri) {
		ContentResolver cR = mContext.getContentResolver();
		String mimeType = cR.getType(uri);
		if (mimeType == null) {
			mimeType = getMimeType(uri.toString());
		}
		return mimeType;
	}


	/**
	 * Tries to retrieve mime types from file extension
	 *
	 * @param url
	 * @return
	 */
	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension);
		}
		return type;
	}


	/**
	 * Retrieves uri mime-type between the ones managed by application
	 *
	 * @param mContext
	 * @param uri
	 * @return
	 */
	public static String getMimeTypeInternal(Context mContext, Uri uri) {
		String mimeType = getMimeType(mContext, uri);
		mimeType = getMimeTypeInternal(mimeType);
		return mimeType;
	}

	/**
	 * Retrieves mime-type between the ones managed by application from given string
	 */
	public static String getMimeTypeInternal(String mimeType) {
		if (mimeType != null) {
			if (mimeType.contains("image/")) {
				mimeType = Constants.MIME_TYPE_IMAGE;
			} else if (mimeType.contains("audio/")) {
				mimeType = Constants.MIME_TYPE_AUDIO;
			} else if (mimeType.contains("video/")) {
				mimeType = Constants.MIME_TYPE_VIDEO;
			} else {
				mimeType = Constants.MIME_TYPE_FILES;
			}
		}
		return mimeType;
	}


	/**
	 * Creates a new attachment file copying data from source file
	 *
	 * @param mContext
	 * @param uri
	 * @return
	 */
	public static Attachment createAttachmentFromUri(Context mContext, Uri uri) {
		return createAttachmentFromUri(mContext, uri, false);
	}


	/**
	 * @param mContext
	 * @param uri
	 * @return
	 */
	public static Attachment createAttachmentFromUri(Context mContext, Uri uri, boolean moveSource) {
		String name = FileHelper.getNameFromUri(mContext, uri);
		String extension = FileHelper.getFileExtension(FileHelper.getNameFromUri(mContext, uri)).toLowerCase(
				Locale.getDefault());
		File f;
		if (moveSource) {
			f = createNewAttachmentFile(mContext, extension);
			try {
				FileUtils.moveFile(new File(uri.getPath()), f);
			} catch (IOException e) {

			}
		} else {
			f = StorageManager.createExternalStoragePrivateFile(mContext, uri, extension);
		}
		Attachment mAttachment = null;
		if (f != null) {
			mAttachment = new Attachment();
			mAttachment.uri = Uri.fromFile(f);
			mAttachment.mimeType = StorageManager.getMimeTypeInternal(mContext, uri);
			mAttachment.name = name;
			mAttachment.size = f.length();
		}
		return mAttachment;
	}
}
