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
import android.text.Editable
import android.text.Spannable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.MovementMethod
import android.text.style.URLSpan
import android.view.*
import android.widget.*
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import it.feio.android.checklistview.Settings
import it.feio.android.checklistview.exceptions.ViewNotSupportedException
import it.feio.android.checklistview.interfaces.CheckListChangedListener
import it.feio.android.checklistview.models.ChecklistManager
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
import net.frju.androidquery.gen.Q
import org.jetbrains.anko.onClick
import org.parceler.Parcels
import java.util.*


class DetailFragment : Fragment(), OnReminderPickedListener, TextWatcher, CheckListChangedListener {

    private var contentEditText: EditText? = null

    /**
     * Used to check currently opened note from activity to avoid opening multiple times the same one
     */
    var currentTask: Task? = null
    private var originalTask: Task? = null
    private var checklistManager: ChecklistManager? = null
    // Values to print result
    private var exitMessage: String? = null
    private var exitMessageStyle: UiUtils.MessageType = UiUtils.MessageType.TYPE_INFO
    private var contentLineCounter = 1
    private var toggleChecklistView: View? = null

    private val compositeDisposable = CompositeDisposable()

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
            originalTask = Parcels.unwrap<Task>(arguments.getParcelable<Parcelable>(Constants.INTENT_TASK))
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

        contentEditText = detail_content

        initViews()

        mainActivity?.lazyFab?.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
            override fun onHidden(fab: FloatingActionButton) {
                super.onHidden(fab)

                fab.setImageResource(R.drawable.ic_done_white_24dp)
                fab.onClick { saveAndExit() }
                fab.setOnLongClickListener(null)
                fab.show()
            }
        })

        switch_checkviews.onClick {
            toggleChecklistAndKeepChecked()
        }

        setHasOptionsMenu(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentTask!!.title = detail_title.text.toString()
        currentTask!!.content = taskContent
        outState.putParcelable("mTask", Parcels.wrap<Task>(currentTask))
        outState.putParcelable("originalTask", Parcels.wrap<Task>(originalTask))
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()

        // Closes keyboard on exit
        if (toggleChecklistView != null) {
            KeyboardUtils.hideKeyboard(toggleChecklistView)
            contentEditText?.clearFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun handleIntents() {
        val i = activity.intent

        // Action called from home shortcut
        if (Constants.ACTION_NOTIFICATION_CLICK == i.action) {
            originalTask = DbUtils.getTask(i.getStringExtra(Constants.INTENT_TASK_ID))
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
            if (i.hasExtra(Constants.INTENT_WIDGET)) {
                val widgetId = i.extras.get(Constants.INTENT_WIDGET)?.toString()
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
                || Constants.INTENT_GOOGLE_NOW == i.action) && i.type != null) {

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
            detail_title.setSelection(contentEditText!!.text.length)
            false
        }
        detail_title.movementMethod = LinkHandler()
        if (currentTask == Task()) { // if the current task is totally empty, display the keyboard
            KeyboardUtils.showKeyboard(detail_title)
        }

        // Init content edit text
        contentEditText?.setText(currentTask!!.content)
        // Avoid focused line goes under the keyboard
        contentEditText?.addTextChangedListener(this)
        contentEditText?.movementMethod = LinkHandler()
        // Restore checklist
        toggleChecklistView = contentEditText
        if (currentTask!!.checklist) {
            currentTask!!.checklist = false
            toggleChecklistView!!.alpha = 0f
            toggleChecklist()
        }

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
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position) {
                    0 -> currentTask!!.pointReward = Task.LOW_POINT_REWARD
                    1 -> currentTask!!.pointReward = Task.NORMAL_POINT_REWARD
                    2 -> currentTask!!.pointReward = Task.HIGH_POINT_REWARD
                    3 -> currentTask!!.pointReward = Task.VERY_HIGH_POINT_REWARD
                }
                reward_points.text = currentTask!!.pointReward.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

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

        if (!currentTask!!.alarmDate.equals(0)) {
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

        if (!currentTask!!.alarmDate.equals(0)) {
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

            activity.supportFragmentManager.popBackStack()
        } else if (activity != null) {
            if (!TextUtils.isEmpty(exitMessage)) {
                Toast.makeText(activity, exitMessage, Toast.LENGTH_SHORT).show()
            }
            activity.finish()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
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

    fun toggleChecklistAndKeepChecked() {

        // In case checklist is active a prompt will ask about many options to decide hot to convert back to simple text
        if (!currentTask!!.checklist) {
            toggleChecklist()
            return
        }

        // If checklist is active but no items are checked the conversion is done automatically without prompting user
        if (checklistManager!!.checkedCount == 0) {
            toggleChecklist(true, false)
            return
        }

        // Inflate the popup_layout.xml
        val inflater = activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.dialog_remove_checklist_layout, null)

        // Retrieves options checkboxes and initialize their values
        val keepChecked = layout.findViewById(R.id.checklist_keep_checked) as CheckBox
        val keepCheckmarks = layout.findViewById(R.id.checklist_keep_checkmarks) as CheckBox
        keepChecked.isChecked = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true)
        keepCheckmarks.isChecked = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true)

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(android.R.string.ok) { dialog, id ->
                    PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKED, keepChecked.isChecked)
                    PrefUtils.putBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked)

                    toggleChecklist()
                }.show()
    }


    /**
     * Toggles checklist view
     */
    private fun toggleChecklist() {
        val keepChecked = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKED, true)
        val showChecks = PrefUtils.getBoolean(PrefUtils.PREF_KEEP_CHECKMARKS, true)
        toggleChecklist(keepChecked, showChecks)
    }

    private fun toggleChecklist(keepChecked: Boolean, showChecks: Boolean) {
        var checkBehavior = Settings.CHECKED_ON_TOP_OF_CHECKED
        when (PrefUtils.getString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, "0")) {
            "0" -> checkBehavior = Settings.CHECKED_ON_TOP_OF_CHECKED
            "1" -> checkBehavior = Settings.CHECKED_ON_BOTTOM
            "2" -> checkBehavior = Settings.CHECKED_HOLD
        }

        // Get instance and set options to convert EditText to CheckListView
        checklistManager = ChecklistManager.getInstance(activity)
                .moveCheckedOnBottom(checkBehavior)
                .showCheckMarks(showChecks)
                .keepChecked(keepChecked)
                .newEntryHint(getString(R.string.checklist_item_hint))
                .dragVibrationEnabled(true)

        // Links parsing options
        checklistManager!!.addTextChangedListener(this)
        checklistManager!!.setCheckListChangedListener(this)

        // Switches the views
        var newView: View? = null
        try {
            newView = checklistManager!!.convert(toggleChecklistView!!)
        } catch (e: ViewNotSupportedException) {
            Dog.e("Error switching checklist view", e)
        }

        // Switches the views
        if (newView != null) {
            checklistManager!!.replaceViews(toggleChecklistView, newView)
            toggleChecklistView = newView
            if (newView is EditText) {
                contentEditText = newView // not beautiful, but fix a bug
            }
            //			fade(toggleChecklistView, true);
            toggleChecklistView!!.animate().alpha(1f).scaleXBy(0f).scaleX(1f).scaleYBy(0f).scaleY(1f)
            currentTask!!.checklist = !currentTask!!.checklist
        }

        if (!currentTask!!.checklist) {
            switch_checkviews.setText(R.string.checklist_on)
            switch_checkviews.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_list_grey600_24dp, 0, 0, 0)
        } else {
            switch_checkviews.setText(R.string.checklist_off)
            switch_checkviews.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_subject_grey600_24dp, 0, 0, 0)
        }
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
                    val category = Parcels.unwrap<Category>(intent!!.getParcelableExtra<Parcelable>(Constants.INTENT_CATEGORY))
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
                    Q.Task.delete().model(currentTask).query()
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
        currentTask!!.content = taskContent

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

        compositeDisposable.add(Observable.create(ObservableOnSubscribe<Task> { emitter ->
            // Note updating on database
            DbUtils.updateTask(currentTask!!, lastModificationUpdatedNeeded())

            // Set reminder if is not passed yet
            if (currentTask!!.hasAlarmInFuture()) {
                currentTask!!.setupReminderAlarm(App.context!!)
            }

            emitter.onNext(currentTask)
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { goHome() })
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

    private val taskContent: String
        get() {
            var content = ""
            if (!currentTask!!.checklist) {
                content = contentEditText?.text.toString()
            } else {
                if (checklistManager != null) {
                    checklistManager!!.keepChecked(true)
                            .showCheckMarks(true)
                    content = checklistManager!!.text
                }
            }
            return content
        }

    /**
     * Updates share intent
     */
    private fun shareTask() {
        currentTask!!.title = detail_title.text.toString()
        currentTask!!.content = taskContent
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

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        scrollContent()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun afterTextChanged(s: Editable) {}

    override fun onCheckListChanged() {
        scrollContent()
    }

    private fun scrollContent() {
        if (currentTask!!.checklist) {
            if (checklistManager!!.count > contentLineCounter) {
                content_wrapper.scrollBy(0, 60)
            }
            contentLineCounter = checklistManager!!.count
        } else {
            if (contentEditText!!.lineCount > contentLineCounter) {
                content_wrapper.scrollBy(0, 60)
            }
            contentLineCounter = contentEditText!!.lineCount
        }
    }


    inner class LinkHandler : MovementMethod {

        override fun initialize(widget: TextView, text: Spannable) {}

        override fun onKeyDown(widget: TextView, text: Spannable, keyCode: Int, event: KeyEvent): Boolean {
            return false
        }

        override fun onKeyUp(widget: TextView, text: Spannable, keyCode: Int, event: KeyEvent): Boolean {
            return false
        }

        override fun onKeyOther(view: TextView, text: Spannable, event: KeyEvent): Boolean {
            return false
        }

        override fun onTakeFocus(widget: TextView, text: Spannable, direction: Int) {}

        override fun onTrackballEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean {
            return false
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

            return false
        }

        override fun onGenericMotionEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean {
            return false
        }

        override fun canSelectArbitrarily(): Boolean {
            return true
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
                    }
        }
    }

    companion object {

        private val CATEGORY_CHANGE = 1

        fun newInstance(task: Task): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle()
            args.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task))
            fragment.arguments = args

            return fragment
        }
    }
}



