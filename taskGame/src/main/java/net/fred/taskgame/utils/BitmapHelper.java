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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class BitmapHelper {

	/**
	 * Decodifica ottimizzata per la memoria dei bitmap
	 *
	 * @param uri       URI bitmap
	 * @param reqWidth  Larghezza richiesta
	 * @param reqHeight Altezza richiesta
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap decodeSampledFromUri(Context mContext, Uri uri, int reqWidth, int reqHeight)
			throws FileNotFoundException {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri), null, options);

		// Setting decode options
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;

		// Bitmap is now decoded for real using calculated inSampleSize
		return BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri), null, options);
	}


	/**
	 * Decoding with inJustDecodeBounds=true to check sampling index without breaking memory
	 *
	 * @param mContext
	 * @param uri
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

		// Calcolo dell'inSampleSize e delle nuove dimensioni proporzionate
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	/**
	 * Creates a thumbnail of requested size by doing a first sampled decoding of the bitmap to optimize memory
	 *
	 * @param ctx
	 * @param uri
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap getThumbnail(Context mContext, Uri uri, int reqWidth, int reqHeight) {
		Bitmap srcBmp;
		Bitmap dstBmp = null;
		try {
			srcBmp = decodeSampledFromUri(mContext, uri, reqWidth, reqHeight);

			// If picture is smaller than required thumbnail
			if (srcBmp.getWidth() < reqWidth && srcBmp.getHeight() < reqHeight) {
				dstBmp = ThumbnailUtils.extractThumbnail(srcBmp, reqWidth, reqHeight);

				// Otherwise the ratio between measures is calculated to fit requested thumbnail's one
			} else {

				// Cropping
				int x = 0, y = 0, width = srcBmp.getWidth(), height = srcBmp.getHeight();
				float ratio = ((float) reqWidth / (float) reqHeight) * ((float) srcBmp.getHeight() / (float) srcBmp.getWidth());
				if (ratio < 1) {
					x = (int) (srcBmp.getWidth() - srcBmp.getWidth() * ratio) / 2;
					width = (int) (srcBmp.getWidth() * ratio);
				} else {
					y = (int) (srcBmp.getHeight() - srcBmp.getHeight() / ratio) / 2;
					height = (int) (srcBmp.getHeight() / ratio);
				}
				dstBmp = Bitmap.createBitmap(srcBmp, x, y, width, height);
			}
		} catch (FileNotFoundException e) {

		}

		return dstBmp;
	}


	/**
	 * Scales a bitmap to fit required ratio
	 *
	 * @param bmp       Image to be scaled
	 * @param reqWidth
	 * @param reqHeight
	 */
	@SuppressWarnings("unused")
	private static Bitmap scaleImage(Context mContext, Bitmap bitmap, int reqWidth, int reqHeight) {

		// Get current dimensions AND the desired bounding box
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int boundingX = dpToPx(mContext, reqWidth);
		int boundingY = dpToPx(mContext, reqHeight);

		// Determine how much to scale: the dimension requiring less scaling is
		// closer to the its side. This way the image always stays inside your
		// bounding box AND either x/y axis touches it.
		float xScale = ((float) boundingX) / width;
		float yScale = ((float) boundingY) / height;
		float scale = (xScale >= yScale) ? xScale : yScale;

		// Create a matrix for the scaling and add the scaling data
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);

		// Create a new bitmap and convert it to a format understood by the
		// ImageView
		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}

	public static InputStream getBitmapInputStream(Bitmap bitmap) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
		byte[] bitmapdata = bos.toByteArray();
		return new ByteArrayInputStream(bitmapdata);
	}


	/**
	 * Retrieves a the bitmap relative to attachment based on mime type
	 *
	 * @param mContext
	 * @param mAttachment
	 * @param width
	 * @param height
	 * @return
	 */
	public static Bitmap getBitmapFromAttachment(Context mContext, Attachment mAttachment, int width, int height) {
		Bitmap bmp = null;

		// Video
		if (Constants.MIME_TYPE_VIDEO.equals(mAttachment.mimeType)) {
			// Tries to retrieve full path from ContentResolver if is a new video
			String path = StorageManager.getRealPathFromURI(mContext,
					mAttachment.uri);
			// .. or directly from local directory otherwise
			if (path == null) {
				path = FileHelper.getPath(mContext, mAttachment.uri);
			}
			bmp = ThumbnailUtils.createVideoThumbnail(path,
					Thumbnails.MINI_KIND);
			if (bmp == null) {
				return null;
			} else {
				bmp = createVideoThumbnail(mContext, bmp, width, height);
			}

			// Image
		} else if (Constants.MIME_TYPE_IMAGE.equals(mAttachment.mimeType)
				|| Constants.MIME_TYPE_SKETCH.equals(mAttachment.mimeType)) {
			try {
				bmp = BitmapHelper.getThumbnail(mContext, mAttachment.uri, width, height);
			} catch (NullPointerException e) {
				bmp = null;
			}

			// Audio
		} else if (Constants.MIME_TYPE_AUDIO.equals(mAttachment.mimeType)) {
			bmp = ThumbnailUtils.extractThumbnail(
					decodeSampledBitmapFromResourceMemOpt(mContext.getResources().openRawResource(R.drawable.play),
							width, height), width, height);

			// File
		} else if (Constants.MIME_TYPE_FILES.equals(mAttachment.mimeType)) {
			bmp = ThumbnailUtils.extractThumbnail(
					decodeSampledBitmapFromResourceMemOpt(mContext.getResources().openRawResource(R.drawable.files),
							width, height), width, height);
		}

		return bmp;
	}


	public static Uri getThumbnailUri(Context mContext, Attachment mAttachment) {
		Uri uri = mAttachment.uri;
		String mimeType = StorageManager.getMimeType(uri.toString());
		if (!TextUtils.isEmpty(mimeType)) {
			String type = mimeType.replaceFirst("/.*", "");
			if (type.equals("image") || type.equals("video")) {
				// Nothing to do, bitmap will be retrieved from this
			} else if (type.equals("audio")) {
				uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.drawable.play);
			} else {
				uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.drawable.files);
			}
		} else {
			uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.drawable.files);
		}
		return uri;
	}


	/**
	 * Draws a watermark on ImageView to highlight videos
	 */
	public static Bitmap createVideoThumbnail(Context mContext, Bitmap video, int width, int height) {
		video = ThumbnailUtils.extractThumbnail(video, width, height);
		Bitmap mark = ThumbnailUtils.extractThumbnail(
				BitmapFactory.decodeResource(mContext.getResources(),
						R.drawable.play_no_bg), width, height);
//		Bitmap thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Bitmap thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(thumbnail);
//		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

//		canvas.drawBitmap(checkIfBroken(mContext, video, height, height), 0, 0, null);
		canvas.drawBitmap(video, 0, 0, null);
		canvas.drawBitmap(mark, 0, 0, null);

		return thumbnail;
	}

	private static int dpToPx(Context mContext, int dp) {
		float density = mContext.getResources().getDisplayMetrics().density;
		return Math.round((float) dp * density);
	}


	public static Bitmap decodeSampledBitmapFromResourceMemOpt(InputStream inputStream, int reqWidth, int reqHeight) {

		byte[] byteArr = new byte[0];
		byte[] buffer = new byte[1024];
		int len;
		int count = 0;

//		int[] pids = { android.os.Process.myPid() };
//        MemoryInfo myMemInfo = mAM.getProcessMemoryInfo(pids)[0];

		try {
			while ((len = inputStream.read(buffer)) > -1) {
				if (len != 0) {
					if (count + len > byteArr.length) {
						byte[] newbuf = new byte[(count + len) * 2];
						System.arraycopy(byteArr, 0, newbuf, 0, count);
						byteArr = newbuf;
					}

					System.arraycopy(buffer, 0, byteArr, count, len);
					count += len;
				}
			}

			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(byteArr, 0, count, options);

			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

//			int[] pids = { android.os.Process.myPid() };
//			MemoryInfo myMemInfo = mAM.getProcessMemoryInfo(pids)[0];
//

			return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}


	public static int getDominantColor(Bitmap source) {
		return getDominantColor(source, true);
	}


	public static int getDominantColor(Bitmap source, boolean applyThreshold) {
		if (source == null)
			return Color.argb(255, 255, 255, 255);

		// Keep track of how many times a hue in a given bin appears in the image.
		// Hue values range [0 .. 360), so dividing by 10, we get 36 bins.
		int[] colorBins = new int[36];

		// The bin with the most colors. Initialize to -1 to prevent accidentally
		// thinking the first bin holds the dominant color.
		int maxBin = -1;

		// Keep track of sum hue/saturation/value per hue bin, which we'll use to
		// compute an average to for the dominant color.
		float[] sumHue = new float[36];
		float[] sumSat = new float[36];
		float[] sumVal = new float[36];
		float[] hsv = new float[3];

		int height = source.getHeight();
		int width = source.getWidth();
		int[] pixels = new int[width * height];
		source.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int row = 0; row < height; row += 2) {
			for (int col = 0; col < width; col += 2) {
				int c = pixels[col + row * width];
				// Ignore pixels with a certain transparency.
//                if (Color.alpha(c) < 128)
//                    continue;

				Color.colorToHSV(c, hsv);

				// If a threshold is applied, ignore arbitrarily chosen values for "white" and "black".
				if (applyThreshold && (hsv[1] <= 0.05f || hsv[2] <= 0.35f))
					continue;

				// We compute the dominant color by putting colors in bins based on their hue.
				int bin = (int) Math.floor(hsv[0] / 10.0f);

				// Update the sum hue/saturation/value for this bin.
				sumHue[bin] = sumHue[bin] + hsv[0];
				sumSat[bin] = sumSat[bin] + hsv[1];
				sumVal[bin] = sumVal[bin] + hsv[2];

				// Increment the number of colors in this bin.
				colorBins[bin]++;

				// Keep track of the bin that holds the most colors.
				if (maxBin < 0 || colorBins[bin] > colorBins[maxBin])
					maxBin = bin;
			}
		}

		// maxBin may never get updated if the image holds only transparent and/or black/white pixels.
		if (maxBin < 0)
			return Color.argb(255, 255, 255, 255);

		// Return a color with the average hue/saturation/value of the bin with the most colors.
		hsv[0] = sumHue[maxBin] / colorBins[maxBin];
		hsv[1] = sumSat[maxBin] / colorBins[maxBin];
		hsv[2] = sumVal[maxBin] / colorBins[maxBin];
		return Color.HSVToColor(hsv);
	}

}
