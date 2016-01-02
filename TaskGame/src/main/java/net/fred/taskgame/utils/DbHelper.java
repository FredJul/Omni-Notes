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

import android.database.sqlite.SQLiteDoneException;

import com.raizlabs.android.dbflow.sql.language.ConditionGroup;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.language.Update;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Attachment_Table;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Category_Table;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task_Table;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DbHelper {

    // Inserting or updating single task
    public static void updateTask(Task task, boolean updateLastModification) {
        if (task.creationDate != 0 && updateLastModification) { // If there already was a creation date, we put at least modification date
            task.lastModificationDate = Calendar.getInstance().getTimeInMillis();
        }

        task.save();

        for (Attachment attachment : task.getAttachmentsList()) {
            attachment.taskId = task.id;
            attachment.async().save();
        }
    }

    /**
     * Getting single task
     *
     * @param id
     * @return
     */
    public static Task getTask(long id) {
        return new Select().from(Task.class).where(Task_Table.id.eq(id)).querySingle();
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
        return new Select().from(Task.class).where(Task_Table.isTrashed.eq(false)).queryList();
    }

    public static List<Task> getTasksTrashed() {
        return new Select().from(Task.class).where(Task_Table.isTrashed.eq(true)).queryList();
    }


    /**
     * Common method for tasks retrieval. It accepts a query to perform and returns matching records.
     *
     * @return Tasks list
     */
    public static List<Task> getTasks() {
        return getTasks(new SQLCondition[]{});
    }

    /**
     * Common method for tasks retrieval. It accepts a query to perform and returns matching records.
     *
     * @return Tasks list
     */
    public static List<Task> getTasks(SQLCondition... conditions) {
        ArrayList<OrderBy> orderByList = new ArrayList<>();

        // Getting sorting criteria from preferences. Reminder screen forces sorting.
        if (Navigation.checkNavigation(Navigation.REMINDERS)) {
            orderByList.add(OrderBy.fromProperty(Task_Table.alarmDate).ascending());
        } else {
            String sortKey = PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, Task_Table.title.getContainerKey());

            if (sortKey.equals(Task_Table.title.getContainerKey())) {
                orderByList.add(OrderBy.fromProperty(Task_Table.title).ascending());
                orderByList.add(OrderBy.fromProperty(Task_Table.content).ascending());
            } else {
                orderByList.add(OrderBy.fromProperty(Task_Table.getProperty(sortKey)).descending());
            }
        }

        return new Select().from(Task.class).where(conditions).orderByAll(orderByList).queryList();
    }

    /**
     * Trashes/restore single task
     *
     * @param task
     */
    public static void trashTask(Task task, boolean trash) {
        task.isTrashed = trash;
        ReminderHelper.removeReminder(MainApplication.getContext(), task);
        PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, task.pointReward);
        updateTask(task, false);
    }


    /**
     * Deleting single task
     */
    public static void deleteTask(Task task) {
        // Attachment deletion from storage
        for (Attachment attachment : task.getAttachmentsList()) {
            StorageHelper.delete(MainApplication.getContext(), attachment.uri.getPath());
        }

        // Delete task's attachments
        Delete.table(Attachment.class, Attachment_Table.taskId.eq(task.id));

        task.delete();
    }

    /**
     * Deleting single attachment
     */
    public static void deleteAttachment(Attachment attachment) {
        // Attachment deletion from storage
        StorageHelper.delete(MainApplication.getContext(), attachment.uri.getPath());
        attachment.delete();
    }

    /**
     * Gets tasks matching pattern with title or content text
     *
     * @param pattern String to match with
     * @return Tasks list
     */
    public static List<Task> getTasksByPattern(String pattern) {
        ArrayList<SQLCondition> conditionList = new ArrayList<>();

        conditionList.add(Task_Table.isTrashed.is(Navigation.checkNavigation(Navigation.TRASH)));

        if (Navigation.checkNavigation(Navigation.CATEGORY)) {
            conditionList.add(Task_Table.categoryId.eq(Navigation.getCategory()));
        }

        conditionList.add(ConditionGroup.clause().and(Task_Table.title.like("%" + pattern + "%")).or(Task_Table.content.like("%" + pattern + "%")));

        return getTasks(conditionList.toArray(new SQLCondition[]{}));
    }


    /**
     * Search for tasks with reminder
     *
     * @param filterPastReminders Excludes past reminders
     * @return Tasks list
     */
    public static List<Task> getTasksWithReminder(boolean filterPastReminders) {
        ArrayList<SQLCondition> conditionList = new ArrayList<>();

        if (filterPastReminders) {
            conditionList.add(Task_Table.alarmDate.greaterThanOrEq(Calendar.getInstance().getTimeInMillis()));
        } else {
            conditionList.add(Task_Table.alarmDate.isNotNull());
        }

        conditionList.add(Task_Table.isTrashed.isNot(true));

        return getTasks(conditionList.toArray(new SQLCondition[]{}));
    }

    /**
     * Retrieves all tasks related to Category it passed as parameter
     *
     * @param categoryId Category integer identifier
     * @return List of tasks with requested category
     */
    public static List<Task> getTasksByCategory(long categoryId) {
        ArrayList<SQLCondition> conditionList = new ArrayList<>();

        conditionList.add(Task_Table.categoryId.eq(categoryId));
        conditionList.add(Task_Table.isTrashed.isNot(true));

        return getTasks(conditionList.toArray(new SQLCondition[]{}));
    }

    /**
     * Retrieves categories list from database
     *
     * @return List of categories
     */
    public static List<Category> getCategories() {
//        return new Select().from(Category.class).where().groupBy(Category_Table.id, Category_Table.name, Category_Table.description, Category_Table.color)
//                .orderBy("IFNULL(NULLIF(" + Category_Table.name.getContainerKey() + ", ''),'zzzzzzzz')").queryList();
        return new Select().from(Category.class).queryList();
    }

    public static Category getCategory(long categoryId) {
        return new Select().from(Category.class).where(Category_Table.id.eq(categoryId)).querySingle();
    }


    public static long getCategorizedCount(Category category) {
        try {
            return new Select().from(Task.class).where(Task_Table.categoryId.eq(category.id)).count();
        } catch (SQLiteDoneException e) {
            // I do not know why this happen when count=0
            return 0;
        }
    }

    public static void deleteCategoryAsync(Category category) {
        new Update(Task.class).set(Task_Table.categoryId.isNull()).where(Task_Table.categoryId.eq(category.id));
        category.async().delete();
    }
}
