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

import android.os.Parcel;
import android.os.Parcelable;

public class Category implements Parcelable {
	private Integer id;
	private String name;
	private String description;
	private String color;
	private int count;

	public Category() {
	}

	private Category(Parcel in) {
		setId(in.readInt());
		setName(in.readString());
		setDescription(in.readString());
		setColor(in.readString());
	}

	public Category(Integer id, String title, String description, String color) {
		super();
		this.id = id;
		this.name = title;
		this.description = description;
		this.color = color;
	}


	public Category(Integer id, String title, String description, String color, int count) {
		super();
		this.id = id;
		this.name = title;
		this.description = description;
		this.color = color;
		this.count = count;
	}

	public Integer getId() {
		return id;
	}


	public void setId(Integer id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}


	public void setName(String title) {
		this.name = title;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getColor() {
		return color;
	}


	public void setColor(String color) {
		this.color = color;
	}


	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(getId());
		parcel.writeString(getName());
		parcel.writeString(getDescription());
		parcel.writeString(getColor());
	}


	@Override
	public String toString() {
		return getName();
	}

	/*
	 * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
	 * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
	 */
	public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {

		public Category createFromParcel(Parcel in) {
			return new Category(in);
		}


		public Category[] newArray(int size) {
			return new Category[size];
		}
	};
}
