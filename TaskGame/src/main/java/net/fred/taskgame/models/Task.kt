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

package net.fred.taskgame.models

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import net.fred.taskgame.App
import net.fred.taskgame.R
import net.fred.taskgame.models.providers.LocalDatabaseProvider
import net.fred.taskgame.receivers.AlarmReceiver
import net.fred.taskgame.utils.Constants
import net.fred.taskgame.utils.DbUtils
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.database.ModelListener
import net.frju.androidquery.gen.Q
import net.frju.androidquery.operation.condition.Where
import org.parceler.Parcel
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Task : ModelListener {

    @DbField(primaryKey = true)
    var id: String? = null
    @DbField
    var title = ""
    @DbField
    var content = ""
    @DbField
    var creationDate: Long = 0
    @DbField
    var lastModificationDate: Long = 0
    @DbField
    var displayPriority: Int = 0
    @DbField
    var finished: Boolean = false
    @DbField
    var alarmDate: Long = 0
    @DbField
    var checklist: Boolean = false
    @DbField
    var categoryId: String? = null
    @DbField
    var pointReward = NORMAL_POINT_REWARD

    @Transient
    @org.parceler.Transient
    var category: Category? = null
        @org.parceler.Transient
        get() {
            if (categoryId == null) {
                field = null
                return null
            }

            if (field == null) {
                field = Q.Category.select().where(Where.where(Q.Category.ID, Where.Op.IS, categoryId)).queryFirst()
            }

            return field
        }
        @org.parceler.Transient
        set(category) {
            categoryId = category?.id
            field = category
        }

    constructor()

    constructor(task: Task) {
        id = task.id
        title = task.title
        content = task.content
        creationDate = task.creationDate
        lastModificationDate = task.lastModificationDate
        displayPriority = task.displayPriority
        finished = task.finished
        alarmDate = task.alarmDate
        checklist = task.checklist
        categoryId = task.categoryId
        pointReward = task.pointReward
        category = task.category
    }

    fun hasAlarmInFuture(): Boolean {
        return alarmDate > Calendar.getInstance().timeInMillis
    }

    override fun toString(): String {
        return title
    }

    fun share(context: Context) {
        val titleText = title
        val contentText = titleText + System.getProperty("line.separator") + content

        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText)
        shareIntent.putExtra(Intent.EXTRA_TEXT, contentText)

        context.startActivity(Intent.createChooser(shareIntent, context.resources.getString(R.string.share_message_chooser)))
    }

    fun saveInFirebase() {
        DbUtils.firebaseTasksNode?.child(id)?.setValue(this)
    }

    fun deleteInFirebase() {
        DbUtils.firebaseTasksNode?.child(id)?.removeValue()
    }

    fun computeListItemTitleAndContent(): Array<String> {

        // Defining title and content texts
        var titleText = title.trim { it <= ' ' }
        var contentText = content.trim { it <= ' ' }

        if (titleText.isEmpty() && contentText.length >= 0) {
            val wrapIndex = contentText.indexOf('\n')
            if (wrapIndex != -1) {
                titleText = contentText.substring(0, wrapIndex)
                contentText = contentText.substring(titleText.length).trim { it <= ' ' }
            } else {
                titleText = contentText
                contentText = ""
            }
        }

        // Replacing checkmarks symbols with html entities
        if (checklist) {
            titleText = titleText.replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM, "✓ ")
                    .replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM, "□ ")
            contentText = contentText
                    .replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM, "✓ ")
                    .replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM, "□ ")
        }

        return arrayOf(titleText, contentText)
    }

    fun setupReminderAlarm(context: Context) {
        if (hasAlarmInFuture()) {
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra(Constants.INTENT_TASK_ID, id) // Do not use parcelable with API 24+ for PendingIntentve
            val sender = PendingIntent.getBroadcast(context, id!!.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmDate, sender)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, alarmDate, sender)
            }
        }
    }

    fun cancelReminderAlarm(context: Context) {
        if (!alarmDate.equals(0)) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val p = PendingIntent.getBroadcast(context, creationDate.toInt(), intent, 0)
            am.cancel(p)
            p.cancel()
        }
    }

    override fun onPreInsert() {
        if (id == null) {
            id = UUID.randomUUID().toString()
        }

        if (creationDate.equals(0)) {
            creationDate = System.currentTimeMillis()
        }
    }

    override fun onPreUpdate() {

    }

    override fun onPreDelete() {
        cancelReminderAlarm(App.context!!)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Task

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (creationDate != other.creationDate) return false
        if (lastModificationDate != other.lastModificationDate) return false
        if (displayPriority != other.displayPriority) return false
        if (finished != other.finished) return false
        if (alarmDate != other.alarmDate) return false
        if (checklist != other.checklist) return false
        if (categoryId != other.categoryId) return false
        if (pointReward != other.pointReward) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + creationDate.hashCode()
        result = 31 * result + lastModificationDate.hashCode()
        result = 31 * result + displayPriority
        result = 31 * result + finished.hashCode()
        result = 31 * result + alarmDate.hashCode()
        result = 31 * result + checklist.hashCode()
        result = 31 * result + (categoryId?.hashCode() ?: 0)
        result = 31 * result + pointReward.hashCode()
        return result
    }

    companion object {

        val LOW_POINT_REWARD: Long = 20
        val NORMAL_POINT_REWARD: Long = 50
        val HIGH_POINT_REWARD: Long = 100
        val VERY_HIGH_POINT_REWARD: Long = 200
    }
}
