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
package it.feio.android.omninotes.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

import it.feio.android.omninotes.utils.EqualityChecker;
import it.feio.android.omninotes.utils.date.DateHelper;

@Table
public class Note extends BaseModel implements Parcelable {
	@Column(columnType = Column.PRIMARY_KEY_AUTO_INCREMENT)
	public int id;
	@Column
	public String title = "";
	@Column
	public String content = "";
	@Column
	public Long creation;
	@Column
	public Long lastModification;
	@Column
	public boolean trashed;
	@Column
	public String alarm;
	@Column
	public Double latitude;
	@Column
	public Double longitude;
	@Column
	public String address;
	@Column
	public boolean checklist;
	@Column
	int categoryId;

	private Category mCategory;
	private List<Attachment> mAttachmentsList;
	private List<Attachment> mAttachmentsListOld;

	public Note() {
	}

	public Note(Note note) {
		setId(note.getId());
		setTitle(note.getTitle());
		setContent(note.getContent());
		setCreation(note.getCreation());
		setLastModification(note.getLastModification());
		setTrashed(note.isTrashed());
		setAlarm(note.getAlarm());
		setLatitude(note.getLatitude());
		setLongitude(note.getLongitude());
		setAddress(note.getAddress());
		setCategory(note.getCategory());
		setChecklist(note.isChecklist());
		setAttachmentsList(note.getAttachmentsList());
		setPasswordChecked(note.isPasswordChecked());
	}

	private Note(Parcel in) {
		setId(in.readInt());
		setCreation(in.readLong());
		setLastModification(in.readLong());
		setTitle(in.readString());
		setContent(in.readString());
		setTrashed(in.readInt());
		setAlarm(in.readString());
		setLatitude(in.readDouble());
		setLongitude(in.readDouble());
		setAddress(in.readString());
		setCategory((Category) in.readParcelable(Category.class.getClassLoader()));
		setChecklist(in.readInt());
		in.readList(getAttachmentsList(), Attachment.class.getClassLoader());
	}

	public List<Attachment> getAttachmentsList() {
		if (mAttachmentsList == null) {
			mAttachmentsList = Select.all(Attachment.class,
					Condition.column(Attachment$Table.NOTEID).is(id));
		}

		return mAttachmentsList;
	}

	public void setAttachmentsList(List<Attachment> attachmentsList) {
		mAttachmentsList = attachmentsList;
	}

	public List<Attachment> getAttachmentsListOld() {
		if (mAttachmentsListOld == null) {
			mAttachmentsListOld = Select.all(Attachment.class,
					Condition.column(Attachment$Table.NOTEID).is(id));
		}

		return mAttachmentsListOld;
	}


	public void setAttachmentsListOld(List<Attachment> attachmentsListOld) {
		mAttachmentsListOld = attachmentsListOld;
	}

	public Category getCategory() {
		if (categoryId == 0) {
			mCategory = null;
			return null;
		}

		if (mCategory == null) {
			mCategory = Select.byId(Category.class, categoryId);
		}

		return mCategory;
	}


	public void setCategory(Category category) {
		categoryId = category != null ? category.id : 0;
		mCategory = category;
	}

	public void setId(int id) {
		this.id = id;
	}


	public int getId() {
		return id;
	}


	public String getTitle() {
		if (title == null) return "";
		return title;
	}


	public void setTitle(String title) {
		this.title = title == null ? "" : title;
	}


	public String getContent() {
		if (content == null) return "";
		return content;
	}


	public void setContent(String content) {
		this.content = content == null ? "" : content;
	}


	public Long getCreation() {
		return creation;
	}


	public void setCreation(Long creation) {
		this.creation = creation;
	}

	public Long getLastModification() {
		return lastModification;
	}


	public void setLastModification(Long lastModification) {
		this.lastModification = lastModification;
	}

	public boolean isTrashed() {
		return trashed == false ? false : true;
	}


	public void setTrashed(boolean trashed) {
		this.trashed = trashed;
	}


	public void setTrashed(int trashed) {
		this.trashed = trashed == 1 ? true : false;
	}


	public String getAlarm() {
		return alarm;
	}


	public void setAlarm(String alarm) {
		this.alarm = alarm;
	}


	public void setAlarm(long alarm) {
		this.alarm = String.valueOf(alarm);
	}


	public Double getLatitude() {
		return latitude;
	}


	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}


	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}


	public boolean isChecklist() {
		return checklist == false ? false : true;
	}


	public void setChecklist(boolean checklist) {
		this.checklist = checklist;
	}

	public void setChecklist(int checklist) {
		this.checklist = checklist == 1 ? true : false;
	}

	public String getAddress() {
		return address;
	}


	public void setAddress(String address) {
		this.address = address;
	}

	public boolean equals(Object o) {
		boolean res = false;
		Note note;
		try {
			note = (Note) o;
		} catch (Exception e) {
			return res;
		}

		Object[] a = {getId(), getTitle(), getContent(), getCreation(), getLastModification(),
				isTrashed(), getAlarm(), getLatitude(), getLongitude(), getAddress(), getCategory()};
		Object[] b = {note.getId(), note.getTitle(), note.getContent(), note.getCreation(),
				note.getLastModification(), note.isTrashed(), note.getAlarm(), note.getLatitude(),
				note.getLongitude(), note.getAddress(), note.getCategory()};
		if (EqualityChecker.check(a, b)) {
			res = true;
		}

		return res;
	}


	public boolean isChanged(Note note) {
		boolean res;
		res = !equals(note) || !getAttachmentsList().equals(note.getAttachmentsList());
		return res;
	}

	public String toString() {
		return getTitle();
	}

	// Not saved in DB
	private boolean passwordChecked = false;


	public String getCreationShort(Context mContext) {
		return DateHelper.getDateTimeShort(mContext, getCreation());
	}


	public String getLastModificationShort(Context mContext) {
		return DateHelper.getDateTimeShort(mContext, getLastModification());
	}


	public String getAlarmShort(Context mContext) {
		if (getAlarm() == null) return "";
		return DateHelper.getDateTimeShort(mContext, Long.parseLong(getAlarm()));
	}

	public boolean isPasswordChecked() {
		return passwordChecked;
	}


	public void setPasswordChecked(boolean passwordChecked) {
		this.passwordChecked = passwordChecked;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(getId());
		parcel.writeLong(getCreation());
		parcel.writeLong(getLastModification());
		parcel.writeString(getTitle());
		parcel.writeString(getContent());
		parcel.writeInt(isTrashed() ? 1 : 0);
		parcel.writeString(getAlarm());
		parcel.writeDouble(getLatitude());
		parcel.writeDouble(getLongitude());
		parcel.writeString(getAddress());
		parcel.writeParcelable(getCategory(), 0);
		parcel.writeInt(isChecklist() ? 1 : 0);
		parcel.writeList(getAttachmentsList());
	}

	/*
	 * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
	 * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
	 */
	public static final Parcelable.Creator<Note> CREATOR = new Parcelable.Creator<Note>() {

		public Note createFromParcel(Parcel in) {
			return new Note(in);
		}


		public Note[] newArray(int size) {
			return new Note[size];
		}
	};
}
