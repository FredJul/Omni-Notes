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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.ConditionGroup;
import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.models.AppDatabase;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.models.Category_Table;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.models.Task_Table;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbUtils {

    public static String FIREBASE_USERS_NODE_NAME = "users";
    public static String FIREBASE_TASKS_NODE_NAME = "tasks";
    public static String FIREBASE_CATEGORIES_NODE_NAME = "categories";
    public static String FIREBASE_CURRENT_POINTS_NODE_NAME = "currentPoints";

    public static DatabaseReference getFirebaseCurrentUserNode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return FirebaseDatabase.getInstance().getReference().child(FIREBASE_USERS_NODE_NAME).child(user.getUid());
        }

        return null;
    }

    public static DatabaseReference getFirebaseTasksNode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return FirebaseDatabase.getInstance().getReference().child(FIREBASE_USERS_NODE_NAME).child(user.getUid()).child(FIREBASE_TASKS_NODE_NAME);
        }

        return null;
    }

    public static DatabaseReference getFirebaseCategoriesNode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return FirebaseDatabase.getInstance().getReference().child(FIREBASE_USERS_NODE_NAME).child(user.getUid()).child(FIREBASE_CATEGORIES_NODE_NAME);
        }

        return null;
    }

    public static long getCurrentPoints() {
        return PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0);
    }

    public static void updateCurrentPoints(long newPoints) {
        PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, newPoints);
        DatabaseReference firebase = getFirebaseCurrentUserNode();
        if (firebase != null) {
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put(FIREBASE_CURRENT_POINTS_NODE_NAME, newPoints);
            firebase.updateChildren(childUpdates);
        }
    }

    // Inserting or updating single task
    public static void updateTask(Task task, boolean updateLastModification) {
        if (task.creationDate != 0 && updateLastModification) { // If there already was a creation date, we put at least modification date
            task.lastModificationDate = Calendar.getInstance().getTimeInMillis();
        }

        task.save();
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
    public static List<Task> getTasksFromCurrentNavigation() {
        long navigation = NavigationUtils.getNavigation();
        if (navigation == NavigationUtils.TASKS) {
            return getActiveTasks();
        } else if (navigation == NavigationUtils.FINISHED_TASKS) {
            return getFinishedTasks();
        } else {
            return getActiveTasksByCategory(navigation);
        }
    }


    public static List<Task> getActiveTasks() {
        return getTasks(Task_Table.isFinished.eq(false));
    }

    public static List<Task> getFinishedTasks() {
        return getTasks(Task_Table.isFinished.eq(true));
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
        orderByList.add(OrderBy.fromProperty(Task_Table.displayPriority).ascending());
        orderByList.add(OrderBy.fromProperty(Task_Table.creationDate).descending());

        return new Select().from(Task.class).where(conditions).orderByAll(orderByList).queryList();
    }

    public static void finishTask(Task task) {
        task.isFinished = true;
        task.cancelReminderAlarm(MainApplication.getContext());
        updateCurrentPoints(getCurrentPoints() + task.pointReward);
        updateTask(task, false);
    }

    public static void restoreTask(Task task) {
        task.isFinished = false;
        updateTask(task, false);
    }


    /**
     * Deleting single task
     */
    public static void deleteTask(Task task) {
        task.cancelReminderAlarm(MainApplication.getContext());

        task.delete();
    }

    public static void deleteTasks(List<Task> tasks) {
        for (Task task : tasks) {
            task.cancelReminderAlarm(MainApplication.getContext());
        }

        ArrayList<Model> objectsToDelete = new ArrayList<>();
        objectsToDelete.addAll(tasks);
        FlowManager.getDatabase(AppDatabase.class).beginTransactionAsync(new ProcessModelTransaction.Builder<>(objectsToDelete,
                new ProcessModelTransaction.ProcessModel<Model>() {
                    @Override
                    public void processModel(Model model) {
                        model.delete();
                    }
                }).build()).build().execute();
    }

    /**
     * Gets tasks matching pattern with title or content text
     *
     * @param pattern String to match with
     * @return Tasks list
     */
    public static List<Task> getTasksByPattern(String pattern) {
        ArrayList<SQLCondition> conditionList = new ArrayList<>();

        conditionList.add(Task_Table.isFinished.is(NavigationUtils.getNavigation() == NavigationUtils.FINISHED_TASKS));

        if (NavigationUtils.isDisplayingACategory()) {
            conditionList.add(Task_Table.categoryId.eq(NavigationUtils.getNavigation()));
        }

        conditionList.add(ConditionGroup.clause().and(Task_Table.title.like("%" + pattern + "%")).or(Task_Table.content.like("%" + pattern + "%")));

        return getTasks(conditionList.toArray(new SQLCondition[conditionList.size()]));
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

        conditionList.add(Task_Table.isFinished.isNot(true));

        return getTasks(conditionList.toArray(new SQLCondition[conditionList.size()]));
    }

    /**
     * Retrieves all tasks related to Category it passed as parameter
     *
     * @param categoryId Category integer identifier
     * @return List of tasks with requested category
     */
    public static List<Task> getActiveTasksByCategory(long categoryId) {
        ArrayList<SQLCondition> conditionList = new ArrayList<>();

        conditionList.add(Task_Table.categoryId.eq(categoryId));
        conditionList.add(Task_Table.isFinished.isNot(true));

        return getTasks(conditionList.toArray(new SQLCondition[conditionList.size()]));
    }

    /**
     * Retrieves categories list from database
     *
     * @return List of categories
     */
    public static List<Category> getCategories() {
        return new Select().from(Category.class).orderBy(Category_Table.creationDate, false).queryList();
    }

    public static Category getCategory(long categoryId) {
        return new Select().from(Category.class).where(Category_Table.id.eq(categoryId)).querySingle();
    }

    public static long getActiveTaskCount() {
        return new Select(Method.count()).from(Task.class).where(Task_Table.isFinished.eq(false)).count();
    }

    public static long getFinishedTaskCount() {
        return new Select(Method.count()).from(Task.class).where(Task_Table.isFinished.eq(true)).count();
    }

    public static long getActiveTaskCountByCategory(Category category) {
        return new Select(Method.count()).from(Task.class).where(Task_Table.isFinished.eq(false), Task_Table.categoryId.eq(category.id)).count();
    }

    public static void deleteCategoryAsync(Category category) {
        // DO NOT USE the below commented solution: it will break firebase sync
        //new Update(Task.class).set(Task_Table.categoryId.isNull()).where(Task_Table.categoryId.eq(category.id));

        for (Task task : getTasks(Task_Table.categoryId.eq(category.id))) {
            task.categoryId = Task.INVALID_ID;
            task.async().save();
        }

        category.async().delete();
    }
}
