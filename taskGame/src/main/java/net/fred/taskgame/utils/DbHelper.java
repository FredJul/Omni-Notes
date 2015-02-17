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

import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.builder.ConditionQueryBuilder;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Update;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Attachment$Table;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Category$Table;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task$Table;

import java.util.Calendar;
import java.util.List;

public class DbHelper {

	// Inserting or updating single task
	public static Task updateTask(Task task, boolean updateLastModification) {

		task.setLastModification(updateLastModification ? Calendar
				.getInstance().getTimeInMillis() : (task.getLastModification() != null ? task.getLastModification() : Calendar
				.getInstance().getTimeInMillis()));

		task.save(true);

		return task;
	}

	/**
	 * Getting single task
	 *
	 * @param id
	 * @return
	 */
	public static Task getTask(int id) {
		return new Select().from(Task.class).where(Condition.column(Task$Table.ID).eq(id)).querySingle();
	}


	/**
	 * Getting All tasks
	 *
	 * @return Tasks list
	 */
	public static List<Task> getAllTasks() {
		int navigation = Navigation.getNavigation();
		switch (navigation) {
			case Navigation.TASKS:
				return getTasksActive();
			case Navigation.REMINDERS:
				return getTasksWithReminder(PrefUtils.getBoolean(PrefUtils.PREF_FILTER_PAST_REMINDERS, false));
			case Navigation.TRASH:
				return getTasksTrashed();
			case Navigation.CATEGORY:
				return getTasksByCategory(Navigation.getCategory());
			default:
				return getTasks();
		}
	}


	public static List<Task> getTasksActive() {
		return new Select().from(Task.class).where(Condition.column(Task$Table.TRASHED).isNot(1)).queryList();
	}

	public static List<Task> getTasksTrashed() {
		return new Select().from(Task.class).where(Condition.column(Task$Table.TRASHED).eq(1)).queryList();
	}


	/**
	 * Common method for tasks retrieval. It accepts a query to perform and returns matching records.
	 *
	 * @return Tasks list
	 */
	public static List<Task> getTasks() {
		return getTasks(null);
	}

	/**
	 * Common method for tasks retrieval. It accepts a query to perform and returns matching records.
	 *
	 * @return Tasks list
	 */
	public static List<Task> getTasks(ConditionQueryBuilder<Task> queryBuilder) {
		String sort_column;

		// Getting sorting criteria from preferences. Reminder screen forces sorting.
		if (Navigation.checkNavigation(Navigation.REMINDERS)) {
			sort_column = Task$Table.ALARM;
		} else {
			sort_column = PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, Task$Table.TITLE);
		}

		// In case of title sorting criteria it must be handled empty title by concatenating content
		sort_column = Task$Table.TITLE.equals(sort_column) ? Task$Table.TITLE + "||" + Task$Table.CONTENT : sort_column;

		// In case of reminder sorting criteria the empty reminder tasks must be moved on bottom of results
		sort_column = Task$Table.ALARM.equals(sort_column) ? "IFNULL(" + Task$Table.ALARM + ", " + Constants.TIMESTAMP_UNIX_EPOCH + ")" : sort_column;

		boolean isAsc = Task$Table.TITLE.equals(sort_column) || Task$Table.ALARM.equals(sort_column);
		return new Select().from(Task.class).where(queryBuilder).orderBy(isAsc, sort_column).queryList();
	}

	/**
	 * Trashes/restore single task
	 *
	 * @param task
	 */
	public static void trashTask(Task task, boolean trash) {
		task.setTrashed(trash);
		ReminderHelper.removeReminder(MainApplication.getContext(), task);
		updateTask(task, false);
	}


	/**
	 * Deleting single task
	 *
	 * @param task
	 */
	public static void deleteTask(Context context, Task task) {
		// Attachment deletion from storage
		for (Attachment mAttachment : task.getAttachmentsList()) {
			StorageManager.deleteExternalStoragePrivateFile(context, mAttachment.uri.getLastPathSegment());
		}

		// Delete task's attachments
		Delete.table(Attachment.class, Condition.column(Attachment$Table.TASKID).eq(task.id));

		task.delete(true);
	}

	/**
	 * Gets tasks matching pattern with title or content text
	 *
	 * @param pattern String to match with
	 * @return Tasks list
	 */
	public static List<Task> getTasksByPattern(String pattern) {
		ConditionQueryBuilder<Task> queryBuilder = new ConditionQueryBuilder<>(Task.class);

		if (Navigation.checkNavigation(Navigation.TRASH)) {
			queryBuilder.putCondition(Condition.column(Task$Table.TRASHED).is(1));
		} else {
			queryBuilder.putCondition(Condition.column(Task$Table.TRASHED).isNot(1));
		}

		if (Navigation.checkNavigation(Navigation.CATEGORY)) {
			queryBuilder.putCondition(Condition.column(Task$Table.CATEGORYID).eq(Navigation.getCategory()));
		}

		queryBuilder.putCondition(Condition.column(Task$Table.TITLE).like("%" + pattern + "%")).or(Condition.column(Task$Table.CONTENT).like("%" + pattern + "%"));

		return getTasks(queryBuilder);
	}


	/**
	 * Search for tasks with reminder
	 *
	 * @param filterPastReminders Excludes past reminders
	 * @return Tasks list
	 */
	public static List<Task> getTasksWithReminder(boolean filterPastReminders) {
		ConditionQueryBuilder<Task> queryBuilder = new ConditionQueryBuilder<>(Task.class);
		if (filterPastReminders) {
			queryBuilder.putCondition(Task$Table.ALARM, ">=", Calendar.getInstance().getTimeInMillis());
		} else {
			queryBuilder.putCondition(Condition.column(Task$Table.ALARM).isNotNull());
		}

		queryBuilder.putCondition(Condition.column(Task$Table.TRASHED).isNot(1));
		return getTasks(queryBuilder);
	}

	/**
	 * Retrieves all tasks related to Category it passed as parameter
	 *
	 * @param categoryId Category integer identifier
	 * @return List of tasks with requested category
	 */
	public static List<Task> getTasksByCategory(String categoryId) {
		ConditionQueryBuilder<Task> queryBuilder = new ConditionQueryBuilder<>(Task.class);
		try {
			int id = Integer.parseInt(categoryId);
			queryBuilder.putCondition(Condition.column(Task$Table.CATEGORYID).eq(id));
			queryBuilder.putCondition(Condition.column(Task$Table.TRASHED).isNot(1));
			return getTasks(queryBuilder);
		} catch (NumberFormatException e) {
			return getAllTasks();
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
		return Select.count(Task.class, Condition.column(Task$Table.CATEGORYID).eq(category.id));
	}

	public static void deleteCategory(Category category) {
		new Update().table(Task.class).set(Condition.column(Task$Table.CATEGORYID).is(null)).where(Condition.column(Task$Table.CATEGORYID).eq(category.id));
		category.delete(true);
	}
}
