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
package net.fred.taskgame.fragment;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.games.Games;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.runtime.transaction.process.ProcessModelInfo;
import com.raizlabs.android.dbflow.runtime.transaction.process.SaveModelTransaction;

import net.fred.taskgame.R;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.IdBasedModel;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.model.adapters.TaskAdapter;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.utils.recycler.ItemActionListener;
import net.fred.taskgame.utils.recycler.SimpleItemTouchHelperCallback;
import net.fred.taskgame.view.EmptyRecyclerView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ListFragment extends Fragment {

    private static final int REQUEST_CODE_CATEGORY_TASKS = 3;

    @Bind(R.id.recycler_view)
    EmptyRecyclerView mRecyclerView;
    @Bind(R.id.empty_view)
    View mEmptyView;

    private FloatingActionButton mFab;

    private TaskAdapter mAdapter;

    private List<Task> mModifiedTasks = new ArrayList<>();
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private ActionMode mActionMode;
    private boolean mKeepActionMode = false;

    private boolean mUndoFinishTask = false;
    private boolean mUndoCategorize = false;
    private Category mUndoCategorizeCategory = null;
    private final SparseArray<Task> mUndoTasksList = new SparseArray<>();
    // Used to remember removed categories from tasks
    private final Map<Task, Category> mUndoCategoryMap = new HashMap<>();

    // Search variables
    private String mSearchQuery;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            if (getActivity() != null) {
                initTasksList(getActivity().getIntent());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSearchQuery = savedInstanceState.getString("mSearchQuery");
            mKeepActionMode = false;
        }
        View layout = inflater.inflate(R.layout.fragment_list, container, false);
        ButterKnife.bind(this, layout);

        // List view initialization
        initRecyclerView();

        // registers for callbacks from the specified tables
        mContentObserver.registerForContentChanges(inflater.getContext(), Task.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Category.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Attachment.class);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editTask(new Task());
            }
        });
        mFab.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Task taskWithChecklist = new Task();
                taskWithChecklist.isChecklist = true;
                editTask(taskWithChecklist);
                return true;
            }
        });

        // Init title
        String[] navigationList = getResources().getStringArray(R.array.navigation_list);
        String[] navigationListCodes = getResources().getStringArray(R.array.navigation_list_codes);
        String navigation = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navigationListCodes[0]);
        int index = Arrays.asList(navigationListCodes).indexOf(navigation);
        CharSequence title = "";
        // If is a traditional navigation item
        if (index >= 0 && index < navigationListCodes.length) {
            title = navigationList[index];
        } else {
            List<Category> categories = DbHelper.getCategories();
            for (Category tag : categories) {
                if (navigation.equals(String.valueOf(tag.id))) title = tag.name;
            }
        }
        title = title == null ? getString(R.string.app_name) : title;
        getMainActivity().getSupportActionBar().setTitle(title);

        // Init tasks list
        initTasksList(getActivity().getIntent());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!mKeepActionMode) {
            commitPending();
            finishActionMode();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mSearchQuery", mSearchQuery);
    }

    @Override
    public void onDestroyView() {
        mContentObserver.unregisterForContentChanges(getView().getContext());

        super.onDestroyView();
    }

    private final class ModeCallback implements android.support.v7.view.ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_list, menu);
            mActionMode = mode;

            mFab.hide();

            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clears data structures
            mAdapter.clearSelections();

            // Defines the conditions to set actionbar items visible or not
            if (!Navigation.checkNavigation(Navigation.FINISHED)) {
                mFab.show();
            }

            mActionMode = null;
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            prepareActionModeMenu();
            return true;
        }


        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            performAction(item, mode);
            return true;
        }
    }

    public void finishActionMode() {
        mAdapter.clearSelections();
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /**
     * Manage check/uncheck of tasks in list during multiple selection phase
     */
    private void updateActionMode() {
        // Close CAB if no items are selected
        if (mAdapter.getSelectedItemCount() == 0) {
            finishActionMode();
        } else {
            prepareActionModeMenu();
        }
    }

    /**
     * Tasks list initialization. Data, actions and callback are defined here.
     */
    private void initRecyclerView() {
        //noinspection ConstantConditions
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        //adapter
        ItemActionListener listener = new ItemActionListener() {
            @Override
            public boolean onItemMove(int fromPosition, int toPosition) {
                if (fromPosition == toPosition) {
                    return true;
                }

                Collections.swap(mAdapter.getTasks(), fromPosition, toPosition);
                mAdapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }

            @Override
            public void onItemMoveFinished() {
                List<Task> tasks = mAdapter.getTasks();
                int count = tasks.size();

                for (int i = 0; i < count; i++) {
                    Task task = tasks.get(i);
                    task.displayPriority = count - 1 - i;
                }

                TransactionManager.getInstance().addTransaction(new SaveModelTransaction<>(ProcessModelInfo.withModels(tasks)));

                finishActionMode();
            }

            @Override
            public void onItemSwiped(int position) {
                // Depending on settings and note status this action will...

                if (Navigation.checkNavigation(Navigation.FINISHED)) { // ...restore
                    restoreTasks(new int[]{position});
                } else { // ...finish
                    finishTasks(new int[]{position});
                }

                mAdapter.notifyItemRemoved(position);
            }

            @Override
            public void onItemClicked(int position) {
                editTask(mAdapter.getTasks().get(position));
            }

            @Override
            public void onItemSelected(int position) {
                if (mActionMode == null) {
                    ((MainActivity) getActivity()).startSupportActionMode(new ModeCallback());
                }

                // Start the CAB using the ActionMode.Callback defined above
                updateActionMode();
                setCabTitle();
            }

            @Override
            public void onItemUnselected(int position) {
                // Start the CAB using the ActionMode.Callback defined above
                updateActionMode();
                setCabTitle();
            }
        };
        mAdapter = new TaskAdapter(getActivity(), listener, mRecyclerView, new ArrayList<Task>());
        mRecyclerView.setEmptyView(mEmptyView);

        mRecyclerView.setLayoutManager(layoutManager);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
        // Initialization of SearchView
        initSearchView(menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setActionItemsVisibility(menu);
    }

    private void prepareActionModeMenu() {
        Menu menu = mActionMode.getMenu();
        int navigation = Navigation.getNavigation();

        if (navigation == Navigation.FINISHED) {
            menu.findItem(R.id.menu_restore_task).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else {
            if (mAdapter.getSelectedItemCount() == 1) {
                menu.findItem(R.id.menu_share).setVisible(true);
            } else {
                menu.findItem(R.id.menu_share).setVisible(false);
            }
            menu.findItem(R.id.menu_category).setVisible(true);
            menu.findItem(R.id.menu_finish_task).setVisible(true);
        }
        menu.findItem(R.id.menu_search).setVisible(false);
        menu.findItem(R.id.menu_select_all).setVisible(true);

        setCabTitle();
    }

    private void setCabTitle() {
        if (mActionMode != null) {
            int title = mAdapter.getSelectedItemCount();
            mActionMode.setTitle(String.valueOf(title));
        }
    }

    /**
     * SearchView initialization. It's a little complex because it's not using SearchManager but is implementing on its
     * own.
     */
    private void initSearchView(final Menu menu) {

        // Save item as class attribute to make it collapse on drawer opening
        mSearchMenuItem = menu.findItem(R.id.menu_search);

        // Associate searchable configuration with the SearchView
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // Expands the widget hiding other actionbar icons
        mSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setActionItemsVisibility(menu);
            }
        });

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Reinitialize tasks list to all tasks when search is collapsed
                mSearchQuery = null;
                mSearchView.setOnQueryTextListener(null); // to avoid a bug
                getActivity().getIntent().setAction(Intent.ACTION_MAIN);
                initTasksList(getActivity().getIntent());
                getActivity().supportInvalidateOptionsMenu();
                return true;
            }


            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String arg0) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String pattern) {
                        mSearchQuery = pattern;
                        onTasksLoaded(DbHelper.getTasksByPattern(mSearchQuery));
                        return true;
                    }
                });
                return true;
            }
        });
    }

    private void setActionItemsVisibility(Menu menu) {
        // Defines the conditions to set actionbar items visible or not
        boolean isInFinishedTasksView = Navigation.checkNavigation(Navigation.FINISHED);

        if (!isInFinishedTasksView) {
            mFab.show();
        } else {
            mFab.hide();
        }
        menu.findItem(R.id.menu_delete_all_finished_tasks).setVisible(isInFinishedTasksView);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        performAction(item, null);
        return super.onOptionsItemSelected(item);
    }

    /**
     * Performs one of the ActionBar button's actions
     */
    private boolean performAction(MenuItem item, ActionMode actionMode) {
        if (actionMode == null) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    if (getMainActivity().getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
                        getMainActivity().getDrawerLayout().closeDrawer(GravityCompat.START);
                    } else {
                        getMainActivity().getDrawerLayout().openDrawer(GravityCompat.START);
                    }
                    break;
                case R.id.menu_delete_all_finished_tasks:
                    deleteAllFinishedTasks();
                    break;
            }
        } else {
            switch (item.getItemId()) {
                case R.id.menu_category:
                    categorizeTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_share:
                    shareTask(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_finish_task:
                    finishTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_restore_task:
                    restoreTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_delete:
                    deleteTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_select_all:
                    selectAllTasks();
                    break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void editTask(final Task task) {
        if (task.id == IdBasedModel.INVALID_ID) {

            // if navigation is a category it will be set into note
            try {
                long categoryId;
                if (getMainActivity().getWidgetCatId() != -1) {
                    categoryId = getMainActivity().getWidgetCatId();
                } else {
                    categoryId = Navigation.getCategory();
                }

                task.setCategory(DbHelper.getCategory(categoryId));
            } catch (NumberFormatException e) {
            }
        }

        // Fragments replacing
        getMainActivity().switchToDetail(task);
    }

    @Override
    public void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQUEST_CODE_CATEGORY_TASKS:
                if (intent != null) {
                    Category tag = Parcels.unwrap(intent.getParcelableExtra(Constants.INTENT_CATEGORY));
                    categorizeTasks(mAdapter.getSelectedItems(), tag);
                }
                break;

            default:
                break;
        }
    }

    private void deleteAllFinishedTasks() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.delete_finished_tasks_confirmation)
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        int[] positions = new int[mAdapter.getTasks().size()];
                        for (int i = 0; i < positions.length; i++) {
                            positions[i] = i;
                        }
                        deleteTasksExecute(positions);
                    }
                }).build();
        dialog.show();
    }

    public void initTasksList(Intent intent) {
        initTasksList(intent, false);
    }

    public void initTasksList(Intent intent, boolean shouldStopSearch) {
        if (shouldStopSearch && mSearchMenuItem != null && MenuItemCompat.isActionViewExpanded(mSearchMenuItem)) {
            mSearchQuery = null;
            MenuItemCompat.collapseActionView(mSearchMenuItem); // collapsing the menu will trigger a new call to initTasksList
        }

        // Searching
        if (mSearchQuery != null || Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // Get the intent, verify the action and get the query
            if (intent.getStringExtra(SearchManager.QUERY) != null) {
                mSearchQuery = intent.getStringExtra(SearchManager.QUERY);
            }
            onTasksLoaded(DbHelper.getTasksByPattern(mSearchQuery));
        } else {
            // Check if is launched from a widget with categories to set tag
            if (getMainActivity().getWidgetCatId() != -1) {
                onTasksLoaded(DbHelper.getTasksByCategory(getMainActivity().getWidgetCatId()));
            } else { // Gets all tasks
                onTasksLoaded(DbHelper.getTasksFromCurrentNavigation());
            }
        }
    }

    private void onTasksLoaded(List<Task> tasks) {
        mAdapter.setTasks(tasks);
    }

    private void finishTasks(int[] positions) {
        for (int position : positions) {
            Task task = mAdapter.getTasks().get(position);
            // Saves tasks to be eventually restored at right position
            mUndoTasksList.put(position + mUndoTasksList.size(), task);
            mModifiedTasks.add(task);
            // Removes note adapter
            mAdapter.getTasks().remove(task);
        }

        finishActionMode();

        // Advice to user
        Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.task_finished, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.info))
                .setAction(R.string.undo, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onUndo();
                    }
                })
                .show();
        mUndoFinishTask = true;
    }

    private void finishTaskExecute(Task task) {
        if (TextUtils.isEmpty(task.questId)) {
            DbHelper.finishTask(task);
        } else {
            // we directly delete quests (to ot be able to restore them), but we still add the reward
            PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0) + task.pointReward);
            Games.Events.increment(getMainActivity().getApiClient(), task.questEventId, 1);
            Games.Quests.claim(getMainActivity().getApiClient(), task.questId, task.questMilestoneId);

            DbHelper.deleteTask(task);
        }

        mAdapter.getTasks().remove(task);
    }

    private void restoreTasks(int[] positions) {
        for (int position : positions) {
            Task task = mAdapter.getTasks().get(position);
            restoreTaskExecute(task);
        }

        finishActionMode();

        // Advice to user
        UiUtils.showMessage(getActivity(), R.string.task_restored);
    }

    private void restoreTaskExecute(Task task) {
        DbHelper.restoreTask(task);
        mAdapter.getTasks().remove(task);
    }


    /**
     * Selects all tasks in list
     */
    private void selectAllTasks() {
        mAdapter.selectAll();
        prepareActionModeMenu();
    }


    /**
     * Batch note permanent deletion
     */
    private void deleteTasks(final int[] positions) {
        // Confirm dialog creation
        new MaterialDialog.Builder(getActivity())
                .content(R.string.delete_task_confirmation)
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deleteTasksExecute(positions);
                    }
                }).build().show();
    }


    /**
     * Performs tasks permanent deletion after confirmation by the user
     */
    private void deleteTasksExecute(int[] positions) {
        for (int position : positions) {
            Task task = mAdapter.getTasks().get(position);
            mAdapter.getTasks().remove(task);
            // Deleting note using DbHelper
            DbHelper.deleteTask(task);
        }

        finishActionMode();

        // Advice to user
        UiUtils.showMessage(getActivity(), R.string.task_deleted);
    }


    /**
     * Associates to or removes categories
     */
    private void categorizeTasks(final int[] positions) {
        // Retrieves all available categories
        final List<Category> categories = DbHelper.getCategories();

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.categorize_as)
                .adapter(new NavDrawerCategoryAdapter(getActivity(), categories), null)
                .positiveText(R.string.add_category)
                .negativeText(R.string.remove_category)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mKeepActionMode = true;
                        Intent intent = new Intent(getActivity(), CategoryActivity.class);
                        intent.putExtra("noHome", true);
                        startActivityForResult(intent, REQUEST_CODE_CATEGORY_TASKS);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        categorizeTasks(positions, null);
                    }
                }).build();

        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                categorizeTasks(positions, categories.get(position));
            }
        });

        dialog.show();
    }

    private void categorizeTasks(int[] positions, Category category) {
        for (int position : positions) {
            Task task = mAdapter.getTasks().get(position);
            // If is restore it will be done immediately, otherwise the undo bar
            // will be shown
            if (category != null) {
                categorizeTaskExecute(task, category);
            } else {
                // Saves categories associated to eventually undo
                mUndoCategoryMap.put(task, task.getCategory());
                // Saves tasks to be eventually restored at right position
                mUndoTasksList.put(mAdapter.getTasks().indexOf(task) + mUndoTasksList.size(), task);
                mModifiedTasks.add(task);
            }
            // Update adapter content if actual navigation is the category
            // associated with actually cycled note
            if (Navigation.checkNavigation(Navigation.CATEGORY) && !Navigation.checkNavigationCategory(category)) {
                mAdapter.notifyItemRemoved(mAdapter.getTasks().indexOf(task));
                mAdapter.getTasks().remove(task);
            } else {
                task.setCategory(category);
                mAdapter.notifyItemChanged(mAdapter.getTasks().indexOf(task));
            }
        }

        finishActionMode();

        // Advice to user
        if (category != null) {
            UiUtils.showMessage(getActivity(), getResources().getText(R.string.tasks_categorized_as) + " '" + category.name + "'");
        } else {
            Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.tasks_category_removed, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.info))
                    .setAction(R.string.undo, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onUndo();
                        }
                    })
                    .show();
            mUndoCategorize = true;
            mUndoCategorizeCategory = null;
        }
    }

    private void categorizeTaskExecute(Task task, Category category) {
        task.setCategory(category);
        DbHelper.updateTask(task, false);
    }

    private void onUndo() {
        // Cycles removed items to re-insert into adapter
        for (Task task : mModifiedTasks) {
            // Manages uncategorize or archive undo
            if ((mUndoCategorize && !Navigation.checkNavigationCategory(mUndoCategoryMap.get(task)))) {
                if (mUndoCategorize) {
                    task.setCategory(mUndoCategoryMap.get(task));
                }

                mAdapter.notifyItemChanged(mAdapter.getTasks().indexOf(task));
            } else { // Manages finish undo
                int position = mUndoTasksList.keyAt(mUndoTasksList.indexOfValue(task));
                mAdapter.getTasks().add(position, task);
                mAdapter.notifyItemInserted(position);
            }
        }

        mUndoTasksList.clear();
        mModifiedTasks.clear();

        mUndoFinishTask = false;
        mUndoCategorize = false;
        mUndoTasksList.clear();
        mUndoCategoryMap.clear();
        mUndoCategorizeCategory = null;

        finishActionMode();
    }

    public void commitPending() {
        if (mUndoFinishTask || mUndoCategorize) {

            for (Task task : mModifiedTasks) {
                if (mUndoFinishTask) {
                    finishTaskExecute(task);
                } else if (mUndoCategorize) {
                    categorizeTaskExecute(task, mUndoCategorizeCategory);
                }
            }

            mUndoFinishTask = false;
            mUndoCategorize = false;
            mUndoCategorizeCategory = null;

            // Clears data structures
            mModifiedTasks.clear();
            mUndoTasksList.clear();
            mUndoCategoryMap.clear();
        }
    }

    /**
     * Shares the selected note from the list
     */
    private void shareTask(int[] positions) {
        // Only one note should be selected to perform sharing but they'll be cycled anyhow
        for (int position : positions) {
            mAdapter.getTasks().get(position).share(getActivity());
        }

        finishActionMode();
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


}
