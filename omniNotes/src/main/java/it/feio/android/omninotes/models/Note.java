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

import java.util.ArrayList;
import java.util.List;

import it.feio.android.omninotes.utils.EqualityChecker;
import it.feio.android.omninotes.utils.date.DateHelper;

public class Note implements Parcelable {
	private int _id;
	private String title;
	private String content;
	private Long creation;
	private Long lastModification;
	private Boolean trashed;
	private String alarm;
	private Double latitude;
	private Double longitude;
	private String address;
	private Category category;
	private Boolean locked;
	private Boolean checklist;
	private List<Attachment> attachmentsList = new ArrayList<Attachment>();
	private List<Attachment> attachmentsListOld = new ArrayList<Attachment>();

	public Note(int _id, Long creation, Long lastModification, String title, String content,
				Integer trashed, String alarm, Double latitude, Double longitude, Integer locked,
				Integer checklist) {
		this._id = _id;
		this.title = title;
		this.content = content;
		this.creation = creation;
		this.lastModification = lastModification;
		this.trashed = trashed == 1 ? true : false;
		this.alarm = alarm;
		setLatitude(latitude);
		setLongitude(longitude);
		setAddress(address);
		setLocked(locked == 1 ? true : false);
		setChecklist(checklist == 1 ? true : false);
	}


	public Note(Note note) {
		set_id(note.get_id());
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
		setLocked(note.isLocked());
		setChecklist(note.isChecklist());
		ArrayList<Attachment> list = new ArrayList<Attachment>();
		for (Attachment mAttachment : note.getAttachmentsList()) {
			list.add(mAttachment);
		}
		setAttachmentsList(list);
		setPasswordChecked(note.isPasswordChecked());
	}


	public Note() {
		this.title = "";
		this.content = "";
		this.trashed = false;
		this.locked = false;
		this.checklist = false;
	}

	private Note(Parcel in) {
		set_id(in.readInt());
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
		setLocked(in.readInt());
		setChecklist(in.readInt());
		in.readList(getAttachmentsList(), Attachment.class.getClassLoader());
	}

	public void set_id(int _id) {
		this._id = _id;
	}


	public int get_id() {
		return _id;
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

	public Boolean isTrashed() {
		return trashed == null || trashed == false ? false : true;
	}


	public void setTrashed(Boolean trashed) {
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

	public Category getCategory() {
		return category;
	}


	public void setCategory(Category category) {
		this.category = category;
	}


	public Boolean isLocked() {
		return locked == null || locked == false ? false : true;
	}


	public void setLocked(Boolean locked) {
		this.locked = locked;
	}


	public void setLocked(int locked) {
		this.locked = locked == 1 ? true : false;
	}


	public Boolean isChecklist() {
		return checklist == null || checklist == false ? false : true;
	}


	public void setChecklist(Boolean checklist) {
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

	public List<Attachment> getAttachmentsList() {
		return attachmentsList;
	}

	public void setAttachmentsList(List<Attachment> attachmentsList) {
		this.attachmentsList = attachmentsList;
	}

	public List<Attachment> getAttachmentsListOld() {
		return attachmentsListOld;
	}


	public void setAttachmentsListOld(List<Attachment> attachmentsListOld) {
		this.attachmentsListOld = attachmentsListOld;
	}

	public boolean equals(Object o) {
		boolean res = false;
		Note note;
		try {
			note = (Note) o;
		} catch (Exception e) {
			return res;
		}

		Object[] a = {get_id(), getTitle(), getContent(), getCreation(), getLastModification(),
				isTrashed(), getAlarm(), getLatitude(), getLongitude(), getAddress(), isLocked(), getCategory()};
		Object[] b = {note.get_id(), note.getTitle(), note.getContent(), note.getCreation(),
				note.getLastModification(), note.isTrashed(), note.getAlarm(), note.getLatitude(),
				note.getLongitude(), note.getAddress(), note.isLocked(), note.getCategory()};
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
		parcel.writeInt(get_id());
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
		parcel.writeInt(isLocked() ? 1 : 0);
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
