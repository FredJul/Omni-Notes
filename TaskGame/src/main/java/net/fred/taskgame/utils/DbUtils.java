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

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.models.Task;
import net.frju.androidquery.gen.Q;
import net.frju.androidquery.operation.condition.Condition;
import net.frju.androidquery.operation.condition.Where;
import net.frju.androidquery.operation.keyword.OrderBy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;

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

        Q.Task.saveViaContentProvider(task).query();
        task.saveInFirebase();
    }

    /**
     * Getting single task
     *
     * @param id
     * @return
     */
    public static Task getTask(String id) {
        return Q.Task.select().where(Where.where(Q.Task.ID, Where.Op.IS, id)).querySingle();
    }


    /**
     * Getting All tasks
     *
     * @return Tasks list
     */
    public static List<Task> getTasksFromCurrentNavigation() {
        String navigation = NavigationUtils.getNavigation();
        if (NavigationUtils.TASKS.equals(navigation)) {
            return getActiveTasks();
        } else if (NavigationUtils.FINISHED_TASKS.equals(navigation)) {
            return getFinishedTasks();
        } else {
            return getActiveTasksByCategory(navigation);
        }
    }


    public static List<Task> getActiveTasks() {
        return getTasks(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, false));
    }

    public static List<Task> getFinishedTasks() {
        return getTasks(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, true));
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
    public static List<Task> getTasks(Condition... conditions) {
        OrderBy[] orderBy = new OrderBy[2];
        orderBy[0] = new OrderBy(Q.Task.DISPLAY_PRIORITY, OrderBy.Order.ASC);
        orderBy[1] = new OrderBy(Q.Task.CREATION_DATE, OrderBy.Order.DESC);

        return Q.Task.select().where(conditions).orderBy(orderBy).query().toList();
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
     * Gets tasks matching pattern with title or content text
     *
     * @param pattern String to match with
     * @return Tasks list
     */
    public static List<Task> getTasksByPattern(String pattern) {
        ArrayList<Condition> Conditions = new ArrayList<>();

        Conditions.add(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, NavigationUtils.FINISHED_TASKS.equals(NavigationUtils.getNavigation())));

        if (NavigationUtils.isDisplayingACategory()) {
            Conditions.add(Condition.where(Q.Task.CATEGORY_ID, Where.Op.IS, NavigationUtils.getNavigation()));
        }

        Conditions.add(Condition.where(Q.Task.CATEGORY_ID, Where.Op.IS, NavigationUtils.getNavigation()));

        Conditions.add(Condition.or(Condition.where(Q.Task.TITLE, Where.Op.LIKE, "%" + pattern + "%"), Condition.where(Q.Task.CONTENT, Where.Op.LIKE, "%" + pattern + "%")));

        return getTasks(Conditions.toArray(new Condition[Conditions.size()]));
    }


    /**
     * Search for tasks with reminder
     *
     * @param filterPastReminders Excludes past reminders
     * @return Tasks list
     */
    public static List<Task> getTasksWithReminder(boolean filterPastReminders) {
        ArrayList<Condition> conditions = new ArrayList<>();

        if (filterPastReminders) {
            conditions.add(Condition.where(Q.Task.ALARM_DATE, Where.Op.MORE_THAN_OR_EQUAL, Calendar.getInstance().getTimeInMillis()));
        } else {
            conditions.add(Condition.where(Q.Task.ALARM_DATE, Where.Op.IS_NOT, null));
        }

        conditions.add(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS_NOT, true));

        return getTasks(conditions.toArray(new Condition[conditions.size()]));
    }

    /**
     * Retrieves all tasks related to Category it passed as parameter
     *
     * @param categoryId Category integer identifier
     * @return List of tasks with requested category
     */
    public static List<Task> getActiveTasksByCategory(String categoryId) {
        ArrayList<Condition> conditions = new ArrayList<>();

        conditions.add(Condition.where(Q.Task.CATEGORY_ID, Where.Op.IS, categoryId));
        conditions.add(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS_NOT, true));

        return getTasks(conditions.toArray(new Condition[conditions.size()]));
    }

    /**
     * Retrieves categories list from database
     *
     * @return List of categories
     */
    public static List<Category> getCategories() {
        return Q.Category.select().orderBy(Q.Category.CREATION_DATE, OrderBy.Order.DESC).query().toList();
    }

    public static Category getCategory(String categoryId) {
        return Q.Category.select().where(Condition.where(Q.Category.ID, Where.Op.IS, categoryId)).querySingle();
    }

    public static long getActiveTaskCount() {
        return Q.Task.count().where(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, false)).query();
    }

    public static long getFinishedTaskCount() {
        return Q.Task.count().where(Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, true)).query();
    }

    public static long getActiveTaskCountByCategory(Category category) {
        return Q.Task.count().where(
                Condition.and(
                        Condition.where(Q.Task.IS_FINISHED, Where.Op.IS, false),
                        Condition.where(Q.Task.CATEGORY_ID, Where.Op.IS, category.id)
                )
        ).query();
    }

    public static void deleteCategoryAsync(Category category) {
        // DO NOT USE the below commented solution: it will break firebase sync
        //new Update(Task.class).set(Task_Table.categoryId.isNull()).where(Task_Table.categoryId.eq(category.id));

        for (Task task : getTasks(Condition.where(Q.Task.CATEGORY_ID, Where.Op.IS, category.id))) {
            task.categoryId = null;
            Q.Task.updateViaContentProvider().model(task).rx().subscribeOn(Schedulers.io()).subscribe();
            task.saveInFirebase();
        }

        Q.Category.deleteViaContentProvider().model(category).rx().subscribeOn(Schedulers.io()).subscribe();
        category.deleteInFirebase();
    }
}
