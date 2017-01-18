/*
 * Copyright (c) 2012-2017 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.fred.taskgame.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import net.fred.taskgame.App
import net.fred.taskgame.models.Category
import net.fred.taskgame.models.Task
import net.frju.androidquery.gen.CATEGORY
import net.frju.androidquery.gen.TASK
import net.frju.androidquery.operation.condition.Where
import net.frju.androidquery.operation.keyword.OrderBy
import java.util.*

object DbUtils {

    var FIREBASE_USERS_NODE_NAME = "users"
    var FIREBASE_TASKS_NODE_NAME = "tasks"
    var FIREBASE_CATEGORIES_NODE_NAME = "categories"
    var FIREBASE_CURRENT_POINTS_NODE_NAME = "currentPoints"

    val firebaseCurrentUserNode: DatabaseReference?
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                return FirebaseDatabase.getInstance().reference.child(FIREBASE_USERS_NODE_NAME).child(user.uid)
            }

            return null
        }

    val firebaseTasksNode: DatabaseReference?
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                return FirebaseDatabase.getInstance().reference.child(FIREBASE_USERS_NODE_NAME).child(user.uid).child(FIREBASE_TASKS_NODE_NAME)
            }

            return null
        }

    val firebaseCategoriesNode: DatabaseReference?
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                return FirebaseDatabase.getInstance().reference.child(FIREBASE_USERS_NODE_NAME).child(user.uid).child(FIREBASE_CATEGORIES_NODE_NAME)
            }

            return null
        }

    val currentPoints: Long
        get() = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)

    fun updateCurrentPoints(newPoints: Long) {
        PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, newPoints)
        val firebase = firebaseCurrentUserNode
        if (firebase != null) {
            val childUpdates = HashMap<String, Any>()
            childUpdates.put(FIREBASE_CURRENT_POINTS_NODE_NAME, newPoints)
            firebase.updateChildren(childUpdates)
        }
    }

    // Inserting or updating single task
    fun updateTask(task: Task, updateLastModification: Boolean) {
        if (task.creationDate != 0L && updateLastModification) { // If there already was a creation date, we put at least modification date
            task.lastModificationDate = Calendar.getInstance().timeInMillis
        }

        TASK.save(task).query()
        task.saveInFirebase()
    }

    /**
     * Getting single task

     * @param id
     * *
     * @return
     */
    fun getTask(id: String): Task? {
        return TASK.select().where(Where.field(TASK.ID).isEqualTo(id)).queryFirst()
    }


    /**
     * Getting All tasks

     * @return Tasks list
     */
    val tasksFromCurrentNavigation: MutableList<Task>
        get() {
            val navigation = NavigationUtils.navigation
            if (NavigationUtils.TASKS == navigation) {
                return activeTasks
            } else if (NavigationUtils.FINISHED_TASKS == navigation) {
                return finishedTasks
            } else {
                return getActiveTasksByCategory(navigation)
            }
        }


    val activeTasks: MutableList<Task>
        get() = getTasks(Where.field(TASK.FINISHED).isEqualTo(false))

    val finishedTasks: MutableList<Task>
        get() = getTasks(Where.field(TASK.FINISHED).isEqualTo(true))


    /**
     * Common method for tasks retrieval. It accepts a query to perform and returns matching records.

     * @return Tasks list
     */
    val tasks: MutableList<Task>
        get() = getTasks(null)

    /**
     * Common method for tasks retrieval. It accepts a query to perform and returns matching records.

     * @return Tasks list
     */
    fun getTasks(vararg conditions: Where?): MutableList<Task> {
        val orderBy = arrayOfNulls<OrderBy>(2)
        orderBy[0] = OrderBy(TASK.DISPLAY_PRIORITY, OrderBy.Order.ASC)
        orderBy[1] = OrderBy(TASK.CREATION_DATE, OrderBy.Order.DESC)

        return TASK.select().where(*conditions).orderBy(*orderBy).query().toList()
    }

    fun finishTaskAsync(task: Task) {
        task.finished = true
        task.cancelReminderAlarm(App.context!!)
        updateCurrentPoints(currentPoints + task.pointReward)
        updateTask(task, false)
    }

    fun restoreTaskAsync(task: Task) {
        task.finished = false
        updateTask(task, false)
    }

    /**
     * Gets tasks matching pattern with title or content text

     * @param pattern String to match with
     * *
     * @return Tasks list
     */
    fun getTasksByPattern(pattern: String): MutableList<Task> {
        val conditions = ArrayList<Where>()

        conditions.add(Where.field(TASK.FINISHED).isEqualTo(NavigationUtils.FINISHED_TASKS == NavigationUtils.navigation))

        if (NavigationUtils.isDisplayingACategory) {
            conditions.add(Where.field(TASK.CATEGORY_ID).isEqualTo(NavigationUtils.navigation))
        }

        conditions.add(Where.field(TASK.TITLE).isLike("%$pattern%")
                .or(Where.field(TASK.CONTENT).isLike("%$pattern%")))

        return getTasks(*conditions.toTypedArray())
    }


    /**
     * Search for tasks with reminder

     * @param filterPastReminders Excludes past reminders
     * *
     * @return Tasks list
     */
    fun getTasksWithReminder(filterPastReminders: Boolean): List<Task> {
        val conditions = ArrayList<Where>()

        if (filterPastReminders) {
            conditions.add(Where.field(TASK.ALARM_DATE).isMoreThanOrEqualTo(Calendar.getInstance().timeInMillis))
        } else {
            conditions.add(Where.field(TASK.ALARM_DATE).isNotEqualTo(null))
        }

        conditions.add(Where.field(TASK.FINISHED).isNotEqualTo(true))

        return getTasks(*conditions.toTypedArray())
    }

    /**
     * Retrieves all tasks related to Category it passed as parameter

     * @param categoryId Category integer identifier
     * *
     * @return List of tasks with requested category
     */
    fun getActiveTasksByCategory(categoryId: String): MutableList<Task> {
        val conditions = ArrayList<Where>()

        conditions.add(Where.field(TASK.CATEGORY_ID).isEqualTo(categoryId))
        conditions.add(Where.field(TASK.FINISHED).isNotEqualTo(true))

        return getTasks(*conditions.toTypedArray())
    }

    /**
     * Retrieves categories list from database

     * @return List of categories
     */
    val categories: MutableList<Category>
        get() = CATEGORY.select().orderBy(CATEGORY.CREATION_DATE, OrderBy.Order.ASC).query().toList()

    fun getCategory(categoryId: String): Category {
        return CATEGORY.select().where(Where.field(CATEGORY.ID).isEqualTo(categoryId)).queryFirst()
    }

    val activeTaskCount: Long
        get() = TASK.count().where(Where.field(TASK.FINISHED).isEqualTo(false)).query()

    val finishedTaskCount: Long
        get() = TASK.count().where(Where.field(TASK.FINISHED).isEqualTo(true)).query()

    fun getActiveTaskCountByCategory(category: Category): Long {
        return TASK.count().where(
                Where.field(TASK.FINISHED).isEqualTo(false),
                Where.field(TASK.CATEGORY_ID).isEqualTo(category.id)
        ).query()
    }

    fun deleteCategory(category: Category) {
        // DO NOT USE the below commented solution: it will break firebase sync
        //new Update(Task.class).set(Task_Table.categoryId.isNull()).where(Task_Table.categoryId.eq(category.id));

        for (task in getTasks(Where.field(TASK.CATEGORY_ID).isEqualTo(category.id))) {
            task.categoryId = null
            TASK.update().model(task).query()
            task.saveInFirebase()
        }

        CATEGORY.delete().model(category).query()
        category.deleteInFirebase()
    }
}
