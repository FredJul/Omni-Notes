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
package net.fred.taskgame.model;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.StorageHelper;

import org.parceler.Parcel;

@Parcel
@Table(database = AppDatabase.class)
public class Attachment extends IdBasedModel {

    @Column
    public long taskId = INVALID_ID;
    @Column
    public String name = "";
    @Column
    public long length;
    @Column
    public String mimeType = "";
    @Column
    public Uri uri = Uri.EMPTY;

    public Uri getThumbnailUri(Context context) {
        String thumbnailMimeType = !TextUtils.isEmpty(mimeType) ? mimeType : StorageHelper.getMimeType(uri.toString());
        if (!TextUtils.isEmpty(thumbnailMimeType)) {
            String type = thumbnailMimeType.replaceFirst("/.*", "");
            switch (type) {
                case "image":
                case "video":
                    // Nothing to do, bitmap will be retrieved from this
                    break;
                case "audio":
                    return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.play);
                default:
                    return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.files);
            }
        } else {
            return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.files);
        }

        return uri;
    }
}
