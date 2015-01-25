/*******************************************************************************
 * Copyright 2014 Federico Iosue (federico.iosue@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package it.feio.android.omninotes.commons.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import it.feio.android.omninotes.commons.utils.EqualityChecker;

public class Note implements Serializable {

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
	private List<? extends Attachment> attachmentsList = new ArrayList<Attachment>();
	private List<? extends Attachment> attachmentsListOld = new ArrayList<Attachment>();


	public Note() {
		super();
		this.title = "";
		this.content = "";
		this.trashed = false;
		this.locked = false;
		this.checklist = false;
	}


	public Note(int _id, Long creation, Long lastModification, String title, String content,
			Integer trashed, String alarm, String latitude, String longitude, Category category, Integer locked,
			Integer checklist) {
		super();
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
		super();
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


	public void setCreation(String creation) {
		Long creationLong;
		try {
			creationLong = Long.parseLong(creation);
		} catch (NumberFormatException e) {
			creationLong = null;
		}
		this.creation = creationLong;
	}


	public Long getLastModification() {
		return lastModification;
	}


	public void setLastModification(Long lastModification) {
		this.lastModification = lastModification;
	}


	public void setLastModification(String lastModification) {
		Long lastModificationLong;
		try {
			lastModificationLong = Long.parseLong(lastModification);
		} catch (NumberFormatException e) {
			lastModificationLong = null;
		}
		this.lastModification = lastModificationLong;
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


	public void setLatitude(String latitude) {
		try {
			setLatitude(Double.parseDouble(latitude));
		} catch (NumberFormatException e) {
			latitude = null;
		} catch (NullPointerException e) {
			latitude = null;
		}
	}


	public Double getLongitude() {
		return longitude;
	}


	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}


	public void setLongitude(String longitude) {
		try {
			setLongitude(Double.parseDouble(longitude));
		} catch (NumberFormatException e) {
			longitude = null;
		} catch (NullPointerException e) {
			latitude = null;
		}
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


	public List<? extends Attachment> getAttachmentsList() {
		return attachmentsList;
	}


	public void setAttachmentsList(List<? extends Attachment> attachmentsList) {
		this.attachmentsList = attachmentsList;
	}


	public void backupAttachmentsList() {
		List<Attachment> attachmentsListOld = new ArrayList<Attachment>();
		for (Attachment mAttachment : getAttachmentsList()) {
			attachmentsListOld.add(mAttachment);
		}
		this.attachmentsListOld = attachmentsListOld;
	}


	public List<? extends Attachment> getAttachmentsListOld() {
		return attachmentsListOld;
	}


	public void setAttachmentsListOld(List<? extends Attachment> attachmentsListOld) {
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

		Object[] a = { get_id(), getTitle(), getContent(), getCreation(), getLastModification(),
				isTrashed(), getAlarm(), getLatitude(), getLongitude(), getAddress(), isLocked(), getCategory() };
		Object[] b = { note.get_id(), note.getTitle(), note.getContent(), note.getCreation(),
				note.getLastModification(), note.isTrashed(), note.getAlarm(), note.getLatitude(),
				note.getLongitude(), note.getAddress(), note.isLocked(), note.getCategory() };
		if (EqualityChecker.check(a, b)) {
			res = true;
		}

		return res;
	}


	public boolean isChanged(Note note) {
		boolean res = false;
		res = !equals(note) || !getAttachmentsList().equals(note.getAttachmentsList());
		return res;
	}


	public boolean isEmpty() {
		Note emptyNote = new Note();
		// Field to exclude for comparison
		emptyNote.setCreation(getCreation());
		emptyNote.setCategory(getCategory());
		// Check
		if (isChanged(emptyNote))
			return false;
		else return true;
	}


	public String toString() {
		return getTitle();
	}
}
