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

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.fragment_list.*
import net.fred.taskgame.R
import net.fred.taskgame.activities.CategoryActivity
import net.fred.taskgame.activities.MainActivity
import net.fred.taskgame.adapters.CategoryAdapter
import net.fred.taskgame.adapters.TaskAdapter
import net.fred.taskgame.models.Category
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.*
import net.fred.taskgame.utils.recycler.ItemActionListener
import net.fred.taskgame.utils.recycler.SimpleItemTouchHelperCallback
import net.frju.androidquery.gen.Q
import net.frju.androidquery.utils.ThrottledContentObserver
import org.jetbrains.anko.onClick
import org.parceler.Parcels
import java.util.*


class ListFragment : Fragment() {

    private var adapter: TaskAdapter? = null

    private val undoTasks = ArrayList<Task>()
    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null
    private var actionMode: ActionMode? = null

    // Search variables
    private var searchQuery: String? = null

    private val contentObserver = object : ThrottledContentObserver(Handler(), 100) {
        override fun onChangeThrottled() {
            if (activity != null) {
                initTasksList(activity.intent, true)
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (activity != null && PrefUtils.PREF_NAVIGATION == key) {
            initTasksList(activity.intent)
        }
    }

    private val mainActivity: MainActivity?
        get() = activity as MainActivity?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString("searchQuery")
        }

        // registers for callbacks from the specified tables
        context.contentResolver.registerContentObserver(Q.Task.getContentUri(), true, contentObserver)
        context.contentResolver.registerContentObserver(Q.Category.getContentUri(), true, contentObserver)

        PrefUtils.registerOnPrefChangeListener(prefListener)

        // List view initialization
        initRecyclerView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mainActivity?.lazyFab?.setImageResource(R.drawable.ic_add_white_24dp)
        mainActivity?.lazyFab?.onClick { editTask(Task()) }

        // Init tasks list
        initTasksList(activity.intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("searchQuery", searchQuery)
    }

    override fun onDestroyView() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        PrefUtils.unregisterOnPrefChangeListener(prefListener)

        super.onDestroyView()
    }

    fun finishActionMode() {
        adapter!!.clearSelections()
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    /**
     * Manage check/uncheck of tasks in list during multiple selection phase
     */
    private fun updateActionMode() {
        // Close CAB if no items are selected
        if (adapter!!.selectedItemCount == 0) {
            finishActionMode()
        } else {
            prepareActionModeMenu()
        }
    }

    /**
     * Tasks list initialization. Data, actions and callback are defined here.
     */
    private fun initRecyclerView() {

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        //adapter
        val listener = object : ItemActionListener {
            override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
                if (fromPosition == toPosition) {
                    return true
                }

                Collections.swap(adapter!!.tasks, fromPosition, toPosition)
                adapter!!.notifyItemMoved(fromPosition, toPosition)

                return true
            }

            override fun onItemMoveFinished() {
                val tasks = adapter!!.tasks
                val count = tasks.size

                for (i in 0..count - 1) {
                    val task = tasks[i]
                    task.displayPriority = i
                    task.saveInFirebase()
                }

                Q.Task.update().model(tasks).query()

                finishActionMode()
            }

            override fun onItemSwiped(position: Int) {
                // Depending on settings and note status this action will...

                if (NavigationUtils.FINISHED_TASKS == NavigationUtils.navigation) { // ...restore
                    restoreTasks(intArrayOf(position))
                } else { // ...finish
                    finishTasks(intArrayOf(position))
                }

                adapter!!.notifyItemRemoved(position)
            }

            override fun onItemClicked(position: Int) {
                editTask(adapter!!.tasks[position])
            }

            override fun onItemSelected(position: Int) {
                if (actionMode == null) {
                    (activity as MainActivity).startSupportActionMode(ModeCallback())
                }

                // Start the CAB using the ActionMode.Callback defined above
                updateActionMode()
                setCabTitle()
            }

            override fun onItemUnselected(position: Int) {
                // Start the CAB using the ActionMode.Callback defined above
                updateActionMode()
                setCabTitle()
            }
        }
        adapter = TaskAdapter(listener, recycler_view, ArrayList<Task>())
        recycler_view.setEmptyView(empty_view)

        recycler_view.layoutManager = layoutManager

        val callback = SimpleItemTouchHelperCallback(adapter!!)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
        // Initialization of SearchView
        initSearchView(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        setActionItemsVisibility(menu)
    }

    private fun prepareActionModeMenu() {
        val menu = actionMode!!.menu

        if (NavigationUtils.FINISHED_TASKS == NavigationUtils.navigation) {
            menu.findItem(R.id.menu_restore_task).isVisible = true
            menu.findItem(R.id.menu_delete).isVisible = true
        } else {
            menu.findItem(R.id.menu_share).isVisible = adapter!!.selectedItemCount == 1
            menu.findItem(R.id.menu_category).isVisible = true
            menu.findItem(R.id.menu_finish_task).isVisible = true
        }
        menu.findItem(R.id.menu_search).isVisible = false
        menu.findItem(R.id.menu_select_all).isVisible = true

        setCabTitle()
    }

    private fun setCabTitle() {
        if (actionMode != null) {
            val title = adapter!!.selectedItemCount
            actionMode!!.title = title.toString()
        }
    }

    /**
     * SearchView initialization. It's a little complex because it's not using SearchManager but is implementing on its
     * own.
     */
    private fun initSearchView(menu: Menu) {

        // Save item as class attribute to make it collapse on drawer opening
        searchMenuItem = menu.findItem(R.id.menu_search)

        // Associate searchable configuration with the SearchView
        searchView = MenuItemCompat.getActionView(searchMenuItem) as SearchView?
        searchView!!.imeOptions = EditorInfo.IME_ACTION_SEARCH

        // Expands the widget hiding other actionbar icons
        searchView!!.setOnQueryTextFocusChangeListener { v, hasFocus -> setActionItemsVisibility(menu) }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Reinitialize tasks list to all tasks when search is collapsed
                searchQuery = null
                searchView!!.setOnQueryTextListener(null) // to avoid a bug
                activity.intent.action = Intent.ACTION_MAIN
                initTasksList(activity.intent)
                activity.supportInvalidateOptionsMenu()
                return true
            }


            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                searchView!!.setOnQueryTextListener(object : OnQueryTextListener {
                    override fun onQueryTextSubmit(arg0: String): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(pattern: String): Boolean {
                        searchQuery = pattern
                        onTasksLoaded(DbUtils.getTasksByPattern(searchQuery!!))
                        return true
                    }
                })
                return true
            }
        })
    }

    private fun setActionItemsVisibility(menu: Menu) {
        // Defines the conditions to set actionbar items visible or not
        val isInFinishedTasksView = NavigationUtils.FINISHED_TASKS == NavigationUtils.navigation

        if (!isInFinishedTasksView) {
            mainActivity?.lazyFab?.show()
        } else {
            mainActivity?.lazyFab?.hide()
        }
        menu.findItem(R.id.menu_delete_all_finished_tasks).isVisible = isInFinishedTasksView
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        performAction(item, null)
        return super.onOptionsItemSelected(item)
    }

    /**
     * Performs one of the ActionBar button's actions
     */
    private fun performAction(item: MenuItem, actionMode: ActionMode?): Boolean {
        if (actionMode == null) {
            when (item.itemId) {
                android.R.id.home -> if (mainActivity!!.lazyDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mainActivity!!.lazyDrawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    mainActivity!!.lazyDrawerLayout.openDrawer(GravityCompat.START)
                }
                R.id.menu_delete_all_finished_tasks -> deleteAllFinishedTasks()
            }
        } else {
            when (item.itemId) {
                R.id.menu_category -> categorizeTasks(adapter!!.selectedItems)
                R.id.menu_share -> shareTask(adapter!!.selectedItems)
                R.id.menu_finish_task -> finishTasks(adapter!!.selectedItems)
                R.id.menu_restore_task -> restoreTasks(adapter!!.selectedItems)
                R.id.menu_delete -> askToDeleteTasks(adapter!!.selectedItems)
                R.id.menu_select_all -> selectAllTasks()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun editTask(task: Task) {
        if (task.id == null) {

            // if navigation is a category it will be set into note
            if (mainActivity?.widgetCatId != null) {
                task.category = DbUtils.getCategory(mainActivity?.widgetCatId!!)
            } else if (NavigationUtils.isDisplayingACategory) {
                task.category = DbUtils.getCategory(NavigationUtils.navigation)
            }
        }

        // Fragments replacing
        mainActivity?.switchToDetail(task)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            REQUEST_CODE_CATEGORY_TASKS -> if (intent != null) {
                val tag = Parcels.unwrap<Category>(intent.getParcelableExtra<Parcelable>(Constants.EXTRA_CATEGORY))
                categorizeTasks(adapter!!.selectedItems, tag)
            }

            else -> {
            }
        }
    }

    private fun deleteAllFinishedTasks() {
        AlertDialog.Builder(activity)
                .setMessage(R.string.delete_finished_tasks_confirmation)
                .setPositiveButton(android.R.string.ok) { dialog, id ->
                    val positions = IntArray(adapter!!.tasks.size)
                    for (i in positions.indices) {
                        positions[i] = i
                    }
                    deleteTasks(positions)
                }.show()
    }

    private fun initTasksList(intent: Intent, shouldStopSearch: Boolean = false) {
        if (shouldStopSearch && searchMenuItem != null && MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
            searchQuery = null
            MenuItemCompat.collapseActionView(searchMenuItem) // collapsing the menu will trigger a new call to initTasksList
        }

        // To put back the default color (not the one of finished items)
        mainActivity?.supportActionBar?.setBackgroundDrawable(null)

        // Searching
        if (searchQuery != null || Intent.ACTION_SEARCH == intent.action) {
            // Get the intent, verify the action and get the query
            if (intent.getStringExtra(SearchManager.QUERY) != null) {
                searchQuery = intent.getStringExtra(SearchManager.QUERY)
            }
            onTasksLoaded(DbUtils.getTasksByPattern(searchQuery!!))
        } else {
            // Check if is launched from a widget with categories to set tag
            if (mainActivity?.widgetCatId != null) {
                onTasksLoaded(DbUtils.getActiveTasksByCategory(mainActivity?.widgetCatId!!))
                mainActivity?.supportActionBar?.setTitle(DbUtils.getCategory(mainActivity?.widgetCatId!!).name)
            } else { // Gets all tasks
                onTasksLoaded(DbUtils.tasksFromCurrentNavigation)
                val currentNavigation = NavigationUtils.navigation
                if (NavigationUtils.TASKS == currentNavigation) {
                    mainActivity?.supportActionBar?.setTitle(R.string.all_tasks)
                } else if (NavigationUtils.FINISHED_TASKS == currentNavigation) {
                    mainActivity?.supportActionBar?.setTitle(R.string.finished_tasks)
                    mainActivity?.supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.finished_tasks_actionbar_color)))
                } else {
                    mainActivity?.supportActionBar?.title = DbUtils.getCategory(currentNavigation).name
                }
            }
        }
    }

    private fun onTasksLoaded(tasks: MutableList<Task>) {
        adapter!!.tasks = tasks
    }

    private fun finishTasks(positions: IntArray) {
        val tasks = adapter!!.tasks
        val toRemoveTasks = ArrayList<Task>()
        for (position in positions) {
            val task = tasks[position]
            toRemoveTasks.add(task)

            DbUtils.finishTask(task)
        }
        tasks.removeAll(toRemoveTasks)
        // Saves tasks to be eventually restored at right position
        undoTasks.addAll(toRemoveTasks)

        finishActionMode()
        displayUndoBar(R.string.task_finished)
    }

    private fun restoreTasks(positions: IntArray) {
        val tasks = adapter!!.tasks
        val toRemoveTasks = ArrayList<Task>()
        for (position in positions) {
            val task = tasks[position]
            toRemoveTasks.add(task)
            DbUtils.restoreTask(task)
        }
        undoTasks.addAll(toRemoveTasks) // Saves tasks to be eventually restored at right position
        tasks.removeAll(toRemoveTasks)

        finishActionMode()
        displayUndoBar(R.string.task_restored)
    }

    private fun displayUndoBar(@StringRes messageId: Int) {
        // Advice to user
        Snackbar.make(activity.findViewById(R.id.coordinator_layout), messageId, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(activity, R.color.info))
                .setAction(R.string.undo) { onUndo() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                            undoTasks.clear()
                        }
                        super.onDismissed(snackbar, event)
                    }
                })
                .show()
    }

    /**
     * Selects all tasks in list
     */
    private fun selectAllTasks() {
        adapter!!.selectAll()
        prepareActionModeMenu()
    }


    /**
     * Batch note permanent deletion
     */
    private fun askToDeleteTasks(positions: IntArray) {
        // Confirm dialog creation
        AlertDialog.Builder(activity)
                .setMessage(R.string.delete_task_confirmation)
                .setPositiveButton(android.R.string.ok) { dialog, id -> deleteTasks(positions) }.show()
    }


    /**
     * Performs tasks permanent deletion after confirmation by the user
     */
    private fun deleteTasks(positions: IntArray) {
        val tasksToDelete = ArrayList<Task>()
        for (position in positions) {
            val task = adapter!!.tasks[position]
            tasksToDelete.add(task)
            task.deleteInFirebase()
        }
        adapter!!.tasks.removeAll(tasksToDelete)
        Q.Task.delete().model(tasksToDelete).query()

        finishActionMode()

        // Advice to user
        UiUtils.showMessage(activity, R.string.task_deleted)
    }


    /**
     * Associates to or removes categories
     */
    private fun categorizeTasks(positions: IntArray) {
        // Retrieves all available categories
        val categories = DbUtils.categories

        AlertDialog.Builder(activity).setTitle(R.string.categorize_as)
                .setAdapter(CategoryAdapter(activity, categories)) { dialog, position ->
                    dialog.dismiss()
                    categorizeTasks(positions, categories[position])
                }
                .setPositiveButton(R.string.add_category) { dialog, id ->
                    val intent = Intent(activity, CategoryActivity::class.java)
                    intent.putExtra("noHome", true)
                    startActivityForResult(intent, REQUEST_CODE_CATEGORY_TASKS)
                }
                .setNegativeButton(R.string.remove_category) { dialog, id -> categorizeTasks(positions, null) }.show()
    }

    private fun categorizeTasks(positions: IntArray, category: Category?) {
        for (position in positions) {
            val task = adapter!!.tasks[position]
            task.category = category
            DbUtils.updateTask(task, false)

            // Update adapter content
            if (NavigationUtils.TASKS != NavigationUtils.navigation && NavigationUtils.FINISHED_TASKS != NavigationUtils.navigation
                    && !NavigationUtils.isDisplayingCategory(category)) {
                adapter!!.notifyItemRemoved(adapter!!.tasks.indexOf(task))
                adapter!!.tasks.remove(task)
            } else {
                task.category = category
                adapter!!.notifyItemChanged(adapter!!.tasks.indexOf(task))
            }
        }

        finishActionMode()

        // Advice to user
        if (category != null) {
            UiUtils.showMessage(activity, resources.getText(R.string.tasks_categorized_as).toString() + " '" + category.name + "'")
        } else {
            UiUtils.showMessage(activity, R.string.tasks_category_removed)
        }
    }

    private fun onUndo() {
        // Cycles removed items to re-insert into adapter
        for (task in undoTasks) {
            if (NavigationUtils.FINISHED_TASKS == NavigationUtils.navigation) {
                DbUtils.finishTask(task) // finish it again
            } else {
                DbUtils.restoreTask(task)
            }
        }

        undoTasks.clear()

        finishActionMode()
    }

    /**
     * Shares the selected note from the list
     */
    private fun shareTask(positions: IntArray) {
        // Only one note should be selected to perform sharing but they'll be cycled anyhow
        for (position in positions) {
            adapter!!.tasks[position].share(activity)
        }

        finishActionMode()
    }

    private inner class ModeCallback : android.support.v7.view.ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate the menu for the CAB
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.menu_list, menu)
            actionMode = mode

            mainActivity?.lazyFab?.hide()

            return true
        }


        override fun onDestroyActionMode(mode: ActionMode) {
            // Clears data structures
            adapter!!.clearSelections()

            // Defines the conditions to set actionbar items visible or not
            if (NavigationUtils.FINISHED_TASKS != NavigationUtils.navigation) {
                mainActivity?.lazyFab?.show()
            }

            actionMode = null
        }


        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            prepareActionModeMenu()
            return true
        }


        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            performAction(item, mode)
            return true
        }
    }

    companion object {

        private val REQUEST_CODE_CATEGORY_TASKS = 3
    }
}
