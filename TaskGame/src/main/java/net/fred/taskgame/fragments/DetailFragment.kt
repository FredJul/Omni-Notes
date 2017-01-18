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

package net.fred.taskgame.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.MenuItemCompat
import android.text.Spannable
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.text.style.URLSpan
import android.view.*
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_detail.*
import net.fred.taskgame.App
import net.fred.taskgame.R
import net.fred.taskgame.activities.CategoryActivity
import net.fred.taskgame.activities.MainActivity
import net.fred.taskgame.adapters.CategoryAdapter
import net.fred.taskgame.listeners.OnReminderPickedListener
import net.fred.taskgame.models.Category
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.*
import net.fred.taskgame.utils.date.DateHelper
import net.fred.taskgame.utils.date.ReminderPickers
import net.frju.androidquery.gen.TASK
import org.jetbrains.anko.doAsync
import org.parceler.Parcels
import java.util.*


class DetailFragment : Fragment(), OnReminderPickedListener {

    /**
     * Used to check currently opened note from activity to avoid opening multiple times the same one
     */
    var currentTask: Task? = null
    private var originalTask: Task? = null
    // Values to print result
    private var exitMessage: String? = null
    private var exitMessageStyle: UiUtils.MessageType = UiUtils.MessageType.TYPE_INFO

    private val mainActivity: MainActivity?
        get() = activity as MainActivity?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Restored temp note after orientation change
        if (savedInstanceState != null) {
            currentTask = Parcels.unwrap<Task>(savedInstanceState.getParcelable<Parcelable>("mTask"))
            originalTask = Parcels.unwrap<Task>(savedInstanceState.getParcelable<Parcelable>("originalTask"))
        }

        // Handling of Intent actions
        handleIntents()

        if (originalTask == null) {
            originalTask = Parcels.unwrap<Task>(arguments.getParcelable<Parcelable>(Constants.EXTRA_TASK))
        }

        if (currentTask == null) {
            currentTask = Task(originalTask!!)
        }

        mainActivity?.supportActionBar?.setTitle(R.string.task)

        if (currentTask!!.finished) {
            mainActivity?.supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.finished_tasks_actionbar_color)))
        } else {
            mainActivity?.supportActionBar?.setBackgroundDrawable(null)
        }

        initViews()

        // Update FAB. Can sometimes be already hidden
        if (mainActivity?.lazyFab?.isShown ?: false) {
            mainActivity?.lazyFab?.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton) {
                    super.onHidden(fab)

                    fab.setImageResource(R.drawable.ic_done_white_24dp)
                    fab.onClick { saveAndExit() }
                    fab.setOnLongClickListener(null)
                    fab.show()
                }
            })
        } else {
            mainActivity?.lazyFab?.setImageResource(R.drawable.ic_done_white_24dp)
            mainActivity?.lazyFab?.onClick { saveAndExit() }
            mainActivity?.lazyFab?.setOnLongClickListener(null)
            mainActivity?.lazyFab?.show()
        }

        setHasOptionsMenu(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentTask!!.title = detail_title.text.toString()
        currentTask!!.content = detail_content.text.toString()
        outState.putParcelable("mTask", Parcels.wrap<Task>(currentTask))
        outState.putParcelable("originalTask", Parcels.wrap<Task>(originalTask))
        super.onSaveInstanceState(outState)
    }

    private fun handleIntents() {
        val i = activity.intent

        // Action called from home shortcut
        if (Constants.ACTION_NOTIFICATION_CLICK == i.action) {
            originalTask = DbUtils.getTask(i.getStringExtra(Constants.EXTRA_TASK_ID))
            // Checks if the note pointed from the shortcut has been deleted
            if (originalTask == null) {
                UiUtils.showMessage(activity, R.string.shortcut_task_deleted)
                activity.finish()
            } else {
                currentTask = Task(originalTask!!)
            }
            i.action = null
        }

        // Check if is launched from a widget
        if (Constants.ACTION_WIDGET == i.action) {

            //  with tags to set tag
            if (i.hasExtra(Constants.EXTRA_WIDGET_ID)) {
                val widgetId = i.extras.get(Constants.EXTRA_WIDGET_ID)?.toString()
                if (widgetId != null) {
                    val categoryId = PrefUtils.getString(PrefUtils.PREF_WIDGET_PREFIX + widgetId, "")
                    if (!categoryId.isEmpty()) {
                        try {
                            val cat = DbUtils.getCategory(categoryId)
                            currentTask = Task()
                            currentTask!!.category = cat
                        } catch (e: NumberFormatException) {
                        }

                    }
                }
            }

            i.action = null
        }


        /**
         * Handles third party apps requests of sharing
         */
        if ((Intent.ACTION_SEND == i.action
                || Intent.ACTION_SEND_MULTIPLE == i.action
                || Constants.ACTION_GOOGLE_NOW == i.action) && i.type != null) {

            if (currentTask == null) {
                currentTask = Task()
            }

            // Text title
            val title = i.getStringExtra(Intent.EXTRA_SUBJECT)
            if (title != null) {
                currentTask!!.title = title
            }

            // Text content
            val content = i.getStringExtra(Intent.EXTRA_TEXT)
            if (content != null) {
                currentTask!!.content = content
            }

            i.action = null
        }

    }

    private fun initViews() {
        // Color of tag marker if note is tagged a function is active in preferences
        setCategoryMarkerColor(currentTask!!.category)

        // Init title edit text
        detail_title.setText(currentTask!!.title)
        // To avoid dropping here the dragged checklist items
        detail_title.setOnDragListener { v, event -> true }
        //When editor action is pressed focus is moved to last character in content field
        detail_title.setOnEditorActionListener { v, actionId, event ->
            detail_title.requestFocus()
            detail_title.setSelection(detail_content.text.length)
            false
        }
        // detail_title.movementMethod = LinkHandler()
        if (currentTask == Task()) { // if the current task is totally empty, display the keyboard
            KeyboardUtils.showKeyboard(detail_title)
        }

        // Init content edit text
        detail_content.setText(currentTask!!.content)
        detail_content.movementMethod = LinkHandler()

        updateTaskInfoString()

        if (currentTask!!.pointReward == Task.LOW_POINT_REWARD) {
            reward_spinner.setSelection(0)
        } else if (currentTask!!.pointReward == Task.NORMAL_POINT_REWARD) {
            reward_spinner.setSelection(1)
        } else if (currentTask!!.pointReward == Task.HIGH_POINT_REWARD) {
            reward_spinner.setSelection(2)
        } else if (currentTask!!.pointReward == Task.VERY_HIGH_POINT_REWARD) {
            reward_spinner.setSelection(3)
        }
        reward_points.text = currentTask!!.pointReward.toString()

        reward_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> currentTask!!.pointReward = Task.LOW_POINT_REWARD
                    1 -> currentTask!!.pointReward = Task.NORMAL_POINT_REWARD
                    2 -> currentTask!!.pointReward = Task.HIGH_POINT_REWARD
                    3 -> currentTask!!.pointReward = Task.VERY_HIGH_POINT_REWARD
                }
                reward_points.text = currentTask!!.pointReward.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun updateTaskInfoString() {
        var info = ""

        // Footer dates of creation
        val lastModification = DateHelper.getDateTimeShort(activity, currentTask!!.lastModificationDate)
        if (!TextUtils.isEmpty(lastModification)) {
            info = getString(R.string.last_update, lastModification)
        } else {
            val creation = DateHelper.getDateTimeShort(activity, currentTask!!.creationDate)
            if (!TextUtils.isEmpty(creation)) {
                info = getString(R.string.creation, creation)
            }
        }

        if (currentTask!!.alarmDate != 0L) {
            info = getString(R.string.alarm_set_on, DateHelper.getDateTimeShort(context, currentTask!!.alarmDate)) + "  —  " + info
        }

        if (!info.isEmpty()) {
            task_info.text = info
        } else {
            task_info.visibility = View.GONE
        }
    }

    /**
     * Colors category marker in note's title and content elements
     */
    private fun setCategoryMarkerColor(category: Category?) {
        // Coloring the target
        if (category != null) {
            category_marker.setBackgroundColor(category.color)
        } else {
            category_marker.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_detail, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onPrepareOptionsMenu(menu: Menu?) {
        // Closes search view if left open in List fragment
        val searchMenuItem = menu!!.findItem(R.id.menu_search)
        if (searchMenuItem != null) {
            MenuItemCompat.collapseActionView(searchMenuItem)
        }

        val newNote = currentTask!!.id == null

        // If note is finished only this options will be available from menu
        if (currentTask!!.finished) {
            menu.findItem(R.id.menu_restore_task).isVisible = true
            menu.findItem(R.id.menu_delete).isVisible = true
            // Otherwise all other actions will be available
        } else {
            menu.findItem(R.id.menu_finish_task).isVisible = !newNote
        }

        if (currentTask!!.alarmDate != 0L) {
            menu.findItem(R.id.menu_set_reminder).isVisible = false
            menu.findItem(R.id.menu_remove_reminder).isVisible = true
        } else {
            menu.findItem(R.id.menu_set_reminder).isVisible = true
            menu.findItem(R.id.menu_remove_reminder).isVisible = false
        }
    }

    fun goHome() {
        val activity = activity
        // The activity has managed a shared intent from third party app and
        // performs a normal onBackPressed instead of returning back to ListActivity
        if (activity != null && activity.supportFragmentManager != null && activity.supportFragmentManager.backStackEntryCount > 0) {
            if (!TextUtils.isEmpty(exitMessage)) {
                UiUtils.showMessage(getActivity(), exitMessage!!, exitMessageStyle)
            }

            // hide the keyboard
            KeyboardUtils.hideKeyboard(detail_title)

            activity.supportFragmentManager.popBackStack()
        } else if (activity != null) {
            if (!TextUtils.isEmpty(exitMessage)) {
                Toast.makeText(activity, exitMessage, Toast.LENGTH_SHORT).show()
            }
            activity.finish()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> saveAndExit()
            R.id.menu_category -> categorizeNote()
            R.id.menu_share -> shareTask()
            R.id.menu_set_reminder -> {
                val reminderPicker = ReminderPickers(activity, this@DetailFragment)
                reminderPicker.pick(if (currentTask!!.hasAlarmInFuture()) currentTask!!.alarmDate else Calendar.getInstance().timeInMillis)
            }
            R.id.menu_remove_reminder -> {
                currentTask!!.alarmDate = 0
                activity.invalidateOptionsMenu()
                updateTaskInfoString()
            }
            R.id.menu_finish_task -> finishTask()
            R.id.menu_restore_task -> restoreTask()
            R.id.menu_discard_changes -> goHome()
            R.id.menu_delete -> deleteTask()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Categorize note choosing from a list of previously created categories
     */
    private fun categorizeNote() {
        // Retrieves all available categories
        val categories = DbUtils.categories

        AlertDialog.Builder(activity).setTitle(R.string.categorize_as)
                .setAdapter(CategoryAdapter(activity, categories)) { dialog, position ->
                    currentTask!!.category = categories[position]
                    setCategoryMarkerColor(categories[position])
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.add_category) { dialog, id ->
                    val intent = Intent(activity, CategoryActivity::class.java)
                    intent.putExtra("noHome", true)
                    startActivityForResult(intent, CATEGORY_CHANGE)
                }
                .setNegativeButton(R.string.remove_category) { dialog, id ->
                    currentTask!!.category = null
                    setCategoryMarkerColor(null)
                }.show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CATEGORY_CHANGE -> {
                    UiUtils.showMessage(activity, R.string.category_saved)
                    val category = Parcels.unwrap<Category>(intent!!.getParcelableExtra<Parcelable>(Constants.EXTRA_CATEGORY))
                    currentTask!!.category = category
                    setCategoryMarkerColor(category)
                }
            }
        }
    }

    private fun finishTask() {
        // Simply go back if is a new note
        if (currentTask!!.id == null) {
            goHome()
            return
        }

        currentTask!!.finished = true
        exitMessage = getString(R.string.task_finished)
        exitMessageStyle = UiUtils.MessageType.TYPE_WARN
        currentTask!!.cancelReminderAlarm(App.context!!)
        saveTask()
    }

    private fun restoreTask() {
        // Simply go back if is a new note
        if (currentTask!!.id == null) {
            goHome()
            return
        }

        currentTask!!.finished = false
        exitMessage = getString(R.string.task_restored)
        exitMessageStyle = UiUtils.MessageType.TYPE_INFO
        currentTask!!.setupReminderAlarm(App.context!!)
        saveTask()
    }

    private fun deleteTask() {
        // Confirm dialog creation
        AlertDialog.Builder(activity)
                .setMessage(R.string.delete_task_confirmation)
                .setPositiveButton(android.R.string.ok) { dialog, id ->
                    currentTask!!.deleteInFirebase()
                    TASK.delete().model(currentTask).query()
                    UiUtils.showMessage(activity, R.string.task_deleted)
                    goHome()
                }.show()
    }

    fun saveAndExit() {
        exitMessage = getString(R.string.task_updated)
        exitMessageStyle = UiUtils.MessageType.TYPE_INFO
        saveTask()
    }


    /**
     * Save new tasks, modify them or archive
     */
    internal fun saveTask() {
        // Changed fields
        currentTask!!.title = detail_title.text.toString()
        currentTask!!.content = detail_content.text.toString()

        // Check if some text or attachments of any type have been inserted or
        // is an empty note
        if (TextUtils.isEmpty(currentTask!!.title) && TextUtils.isEmpty(currentTask!!.content)) {

            exitMessage = getString(R.string.empty_task_not_saved)
            exitMessageStyle = UiUtils.MessageType.TYPE_INFO
            goHome()
            return
        }

        if (saveNotNeeded()) {
            return
        }

        // Note updating on database
        doAsync {
            DbUtils.updateTask(currentTask!!, lastModificationUpdatedNeeded())

            // Set reminder if is not passed yet
            if (currentTask!!.hasAlarmInFuture()) {
                currentTask!!.setupReminderAlarm(App.context!!)
            }
        }

        goHome()
    }


    /**
     * Checks if nothing is changed to avoid committing if possible (check)
     */
    private fun saveNotNeeded(): Boolean {
        if (currentTask == originalTask) {
            exitMessage = ""
            goHome()
            return true
        }
        return false
    }


    /**
     * Checks if only category or finish status have been changed
     * and then force to not update last modification date*
     */
    private fun lastModificationUpdatedNeeded(): Boolean {
        val tmpTask = Task(currentTask!!)
        tmpTask.category = currentTask!!.category
        tmpTask.finished = currentTask!!.finished
        return tmpTask != originalTask
    }

    /**
     * Updates share intent
     */
    private fun shareTask() {
        currentTask!!.title = detail_title.text.toString()
        currentTask!!.content = detail_content.text.toString()
        currentTask!!.share(activity)
    }

    override fun onReminderPicked(reminder: Long) {
        currentTask!!.alarmDate = reminder
        activity.invalidateOptionsMenu()
        updateTaskInfoString()
    }

    override fun onReminderDismissed() {
        // Nothing to do
    }

    inner class LinkHandler : ScrollingMovementMethod() {

        override fun canSelectArbitrarily(): Boolean {
            return true
        }

        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            val action = event.action

            if (action == MotionEvent.ACTION_UP) {
                var x = event.x.toInt()
                var y = event.y.toInt()

                x -= widget.totalPaddingLeft
                y -= widget.totalPaddingTop

                x += widget.scrollX
                y += widget.scrollY

                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                val link = buffer.getSpans(off, off, URLSpan::class.java)

                if (link.isNotEmpty()) {
                    onLinkClick(link[0].url)
                    return true
                }
            }

            return super.onTouchEvent(widget, buffer, event)
        }

        private fun onLinkClick(url: String) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.open_link_dialog_title)
                    .setPositiveButton(R.string.open_link) { dialog, id ->
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            UiUtils.showErrorMessage(activity, R.string.app_not_found)
                        }
                    }
                    .setNegativeButton(R.string.modify_link) { dialog, id ->
                        // Nothing to do }.show()
                    }.show()
        }
    }

    companion object {

        private val CATEGORY_CHANGE = 1

        fun newInstance(task: Task): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle()
            args.putParcelable(Constants.EXTRA_TASK, Parcels.wrap(task))
            fragment.arguments = args

            return fragment
        }
    }
}



