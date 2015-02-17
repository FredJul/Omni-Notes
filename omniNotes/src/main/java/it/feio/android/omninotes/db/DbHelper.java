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
package it.feio.android.omninotes.db;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.builder.ConditionQueryBuilder;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Update;

import java.util.Calendar;
import java.util.List;

import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Attachment$Table;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Category$Table;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.Note$Table;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.Navigation;
import it.feio.android.omninotes.utils.PrefUtils;
import it.feio.android.omninotes.utils.StorageManager;

public class DbHelper {

	// Inserting or updating single note
	public static Note updateNote(Note note, boolean updateLastModification) {

		note.setLastModification(updateLastModification ? Calendar
				.getInstance().getTimeInMillis() : (note.getLastModification() != null ? note.getLastModification() : Calendar
				.getInstance().getTimeInMillis()));

		note.save(true);

		return note;
	}

	/**
	 * Getting single note
	 *
	 * @param id
	 * @return
	 */
	public static Note getNote(int id) {
		return new Select().from(Note.class).where(Condition.column(Note$Table.ID).eq(id)).querySingle();
	}


	/**
	 * Getting All notes
	 *
	 * @return Notes list
	 */
	public static List<Note> getAllNotes() {
		int navigation = Navigation.getNavigation();
		switch (navigation) {
			case Navigation.NOTES:
				return getNotesActive();
			case Navigation.REMINDERS:
				return getNotesWithReminder(PrefUtils.getBoolean(PrefUtils.PREF_FILTER_PAST_REMINDERS, false));
			case Navigation.TRASH:
				return getNotesTrashed();
			case Navigation.CATEGORY:
				return getNotesByCategory(Navigation.getCategory());
			default:
				return getNotes();
		}
	}


	public static List<Note> getNotesActive() {
		return new Select().from(Note.class).where(Condition.column(Note$Table.TRASHED).isNot(1)).queryList();
	}

	public static List<Note> getNotesTrashed() {
		return new Select().from(Note.class).where(Condition.column(Note$Table.TRASHED).eq(1)).queryList();
	}


	/**
	 * Common method for notes retrieval. It accepts a query to perform and returns matching records.
	 *
	 * @return Notes list
	 */
	public static List<Note> getNotes() {
		return getNotes(null);
	}

	/**
	 * Common method for notes retrieval. It accepts a query to perform and returns matching records.
	 *
	 * @return Notes list
	 */
	public static List<Note> getNotes(ConditionQueryBuilder<Note> queryBuilder) {
		String sort_column;

		// Getting sorting criteria from preferences. Reminder screen forces sorting.
		if (Navigation.checkNavigation(Navigation.REMINDERS)) {
			sort_column = Note$Table.ALARM;
		} else {
			sort_column = PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, Note$Table.TITLE);
		}

		// In case of title sorting criteria it must be handled empty title by concatenating content
		sort_column = Note$Table.TITLE.equals(sort_column) ? Note$Table.TITLE + "||" + Note$Table.CONTENT : sort_column;

		// In case of reminder sorting criteria the empty reminder notes must be moved on bottom of results
		sort_column = Note$Table.ALARM.equals(sort_column) ? "IFNULL(" + Note$Table.ALARM + ", " + Constants.TIMESTAMP_UNIX_EPOCH + ")" : sort_column;

		boolean isAsc = Note$Table.TITLE.equals(sort_column) || Note$Table.ALARM.equals(sort_column);
		return new Select().from(Note.class).where(queryBuilder).orderBy(isAsc, sort_column).queryList();
	}

	/**
	 * Trashes/restore single note
	 *
	 * @param note
	 */
	public static void trashNote(Note note, boolean trash) {
		note.setTrashed(trash);
		updateNote(note, false);
	}


	/**
	 * Deleting single note
	 *
	 * @param note
	 */
	public static void deleteNote(Context context, Note note) {
		// Attachment deletion from storage
		for (Attachment mAttachment : note.getAttachmentsList()) {
			StorageManager.deleteExternalStoragePrivateFile(context, mAttachment.uri.getLastPathSegment());
		}

		// Delete note's attachments
		Delete.table(Attachment.class, Condition.column(Attachment$Table.NOTEID).eq(note.id));

		note.delete(true);
	}

	/**
	 * Gets notes matching pattern with title or content text
	 *
	 * @param pattern String to match with
	 * @return Notes list
	 */
	public static List<Note> getNotesByPattern(String pattern) {
		ConditionQueryBuilder<Note> queryBuilder = new ConditionQueryBuilder<>(Note.class);

		if (Navigation.checkNavigation(Navigation.TRASH)) {
			queryBuilder.putCondition(Condition.column(Note$Table.TRASHED).is(1));
		} else {
			queryBuilder.putCondition(Condition.column(Note$Table.TRASHED).isNot(1));
		}

		if (Navigation.checkNavigation(Navigation.CATEGORY)) {
			queryBuilder.putCondition(Condition.column(Note$Table.CATEGORYID).eq(Navigation.getCategory()));
		}

		queryBuilder.putCondition(Condition.column(Note$Table.TITLE).like("%" + pattern + "%")).or(Condition.column(Note$Table.CONTENT).like("%" + pattern + "%"));

		return getNotes(queryBuilder);
	}


	/**
	 * Search for notes with reminder
	 *
	 * @param filterPastReminders Excludes past reminders
	 * @return Notes list
	 */
	public static List<Note> getNotesWithReminder(boolean filterPastReminders) {
		ConditionQueryBuilder<Note> queryBuilder = new ConditionQueryBuilder<>(Note.class);
		if (filterPastReminders) {
			queryBuilder.putCondition(Note$Table.ALARM, ">=", Calendar.getInstance().getTimeInMillis());
		} else {
			queryBuilder.putCondition(Condition.column(Note$Table.ALARM).isNotNull());
		}

		queryBuilder.putCondition(Condition.column(Note$Table.TRASHED).isNot(1));
		return getNotes(queryBuilder);
	}

	/**
	 * Retrieves all notes related to Category it passed as parameter
	 *
	 * @param categoryId Category integer identifier
	 * @return List of notes with requested category
	 */
	public static List<Note> getNotesByCategory(String categoryId) {
		ConditionQueryBuilder<Note> queryBuilder = new ConditionQueryBuilder<>(Note.class);
		try {
			int id = Integer.parseInt(categoryId);
			queryBuilder.putCondition(Condition.column(Note$Table.CATEGORYID).eq(id));
			queryBuilder.putCondition(Condition.column(Note$Table.TRASHED).isNot(1));
			return getNotes(queryBuilder);
		} catch (NumberFormatException e) {
			return getAllNotes();
		}
	}

	/**
	 * Retrieves categories list from database
	 *
	 * @return List of categories
	 */
	public static List<Category> getCategories() {
		QueryBuilder groupBy = new QueryBuilder().appendArray(Category$Table.ID, Category$Table.NAME, Category$Table.DESCRIPTION, Category$Table.COLOR);
		return new Select().from(Category.class).where().groupBy(groupBy).orderBy("IFNULL(NULLIF(" + Category$Table.NAME + ", ''),'zzzzzzzz')").queryList();
	}

	public static Category getCategory(int categoryId) {
		return new Select().from(Category.class).where(Condition.column(Category$Table.ID).eq(categoryId)).querySingle();
	}


	public static long getCategorizedCount(Category category) {
		return Select.count(Note.class, Condition.column(Note$Table.CATEGORYID).eq(category.id));
	}

	public static void deleteCategory(Category category) {
		new Update().table(Note.class).set(Condition.column(Note$Table.CATEGORYID).is(null)).where(Condition.column(Note$Table.CATEGORYID).eq(category.id));
		category.delete(true);
	}
}
