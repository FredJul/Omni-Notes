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

import android.animation.Animator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.games.Games;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;

import net.fred.taskgame.R;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.IdBasedModel;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.model.adapters.TaskAdapter;
import net.fred.taskgame.model.listeners.OnViewTouchedListener;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ListFragment extends Fragment implements OnViewTouchedListener {

    private static final int REQUEST_CODE_CATEGORY = 2;
    private static final int REQUEST_CODE_CATEGORY_TASKS = 3;

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @Bind(R.id.empty_view)
    View mEmptyView;

    private TaskAdapter mAdapter;

    private List<Task> mModifiedTasks = new ArrayList<>();
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Menu mMenu;
    private android.support.v7.view.ActionMode mActionMode;
    private boolean mKeepActionMode = false;

    // Undo archive/trash
    private boolean mUndoTrash = false;
    private boolean mUndoCategorize = false;
    private Category mUndoCategorizeCategory = null;
    private final SparseArray<Task> mUndoTasksList = new SparseArray<>();
    // Used to remember removed categories from tasks
    private final Map<Task, Category> mUndoCategoryMap = new HashMap<>();

    // Search variables
    private String mSearchQuery;

    //    Fab
    private FloatingActionsMenu mFab;
    private boolean mFabAllowed;
    private boolean mFabExpanded = false;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            initTasksList(getActivity().getIntent());
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

        // Init FAB
        mFab = (FloatingActionsMenu) getActivity().findViewById(R.id.fab);
        AddFloatingActionButton fabAddButton = (AddFloatingActionButton) mFab.findViewById(com.getbase.floatingactionbutton.R.id.fab_expand_menu_button);
        fabAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFabExpanded) {
                    mFab.toggle();
                    mFabExpanded = false;
                } else {
                    editTask(new Task());
                }
            }
        });
        fabAddButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mFabExpanded = !mFabExpanded;
                mFab.toggle();
                return true;
            }
        });
        mFab.findViewById(R.id.fab_checklist).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = new Task();
                task.isChecklist = true;
                editTask(task);
            }
        });
        mFab.findViewById(R.id.fab_camera).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getActivity().getIntent();
                i.setAction(Constants.ACTION_TAKE_PHOTO);
                getActivity().setIntent(i);
                editTask(new Task());
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
    public void onResume() {
        super.onResume();

        // Removes navigation drawer forced closed status
        if (getMainActivity().getDrawerLayout() != null) {
            getMainActivity().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
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

            mFabAllowed = false;
            hideFab();

            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clears data structures
            mAdapter.clearSelections();

            setFabAllowed(true);
            if (mUndoTasksList.size() == 0) {
                showFab();
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

    private void setFabAllowed(boolean allowed) {
        if (allowed) {
            boolean showFab = Navigation.checkNavigation(new Integer[]{Navigation.TASKS, Navigation.CATEGORY});
            if (showFab) {
                mFabAllowed = true;
            }
        } else {
            mFabAllowed = false;
        }
    }

    private void showFab() {
        if (mFab != null && mFabAllowed && isFabHidden()) {
            animateFab(0, View.VISIBLE, View.VISIBLE);
        }
    }

    private void hideFab() {
        if (mFab != null && !isFabHidden()) {
            mFab.collapse();
            animateFab(mFab.getHeight() + getMarginBottom(mFab), View.VISIBLE, View.INVISIBLE);
        }
    }

    private boolean isFabHidden() {
        return mFab.getVisibility() != View.VISIBLE;
    }

    private void animateFab(int translationY, final int visibilityBefore, final int visibilityAfter) {
        mFab.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(Constants.FAB_ANIMATION_TIME)
                .translationY(translationY)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        mFab.setVisibility(visibilityBefore);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mFab.setVisibility(visibilityAfter);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
    }

    private int getMarginBottom(View view) {
        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
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
    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);

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
        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        RecyclerViewTouchActionGuardManager recyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        recyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        recyclerViewTouchActionGuardManager.setEnabled(true);

        // swipe manager
        RecyclerViewSwipeManager recyclerViewSwipeManager = new RecyclerViewSwipeManager();

        //adapter
        mAdapter = new TaskAdapter(getActivity(), new ArrayList<Task>());
        mAdapter.setEventListener(new TaskAdapter.EventListener() {
            @Override
            public void onItemRemoved(int position) {
                // Depending on settings and note status this action will...

                if (Navigation.checkNavigation(Navigation.TRASH)) { // ...restore
                    untrashTasks(new int[]{position});
                } else if (Navigation.checkNavigation(Navigation.CATEGORY)) { // ...removes category
                    categorizeTasks(new int[]{position}, null);
                } else { // ...trash
                    trashTasks(new int[]{position});
                }
            }

            @Override
            public void onItemViewClicked(View v, int position) {
                if (mActionMode == null) {
                    editTask(mAdapter.getTasks().get(position));
                } else {
                    // If in CAB mode
                    toggleSelection(position);
                    setCabTitle();
                }
            }

            @Override
            public void onItemViewLongClicked(View v, int position) {
                if (mActionMode == null) {
                    ((MainActivity) getActivity()).startSupportActionMode(new ModeCallback());
                }
                // Start the CAB using the ActionMode.Callback defined above
                toggleSelection(position);
                setCabTitle();
            }
        });
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkAdapterIsEmpty();
            }
        });

        RecyclerView.Adapter wrappedAdapter = recyclerViewSwipeManager.createWrappedAdapter(mAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(wrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        recyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        recyclerViewSwipeManager.attachRecyclerView(mRecyclerView);
    }

    private void checkAdapterIsEmpty() {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewTouchOccurred(MotionEvent ev) {
        commitPending();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
        this.mMenu = menu;
        // Initialization of SearchView
        initSearchView(menu);
    }

    private void initSortingSubmenu() {
        final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
        final String[] arrayDialog = getResources().getStringArray(R.array.sortable_columns_human_readable);
        int selected = Arrays.asList(arrayDb).indexOf(PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, arrayDb[0]));

        SubMenu sortMenu = this.mMenu.findItem(R.id.menu_sort).getSubMenu();
        for (int i = 0; i < arrayDialog.length; i++) {
            if (sortMenu.findItem(i) == null) {
                sortMenu.add(Constants.MENU_SORT_GROUP_ID, i, i, arrayDialog[i]);
            }
            if (i == selected) sortMenu.getItem(i).setChecked(true);
        }
        sortMenu.setGroupCheckable(Constants.MENU_SORT_GROUP_ID, true, true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setActionItemsVisibility(menu, false);
    }

    private void prepareActionModeMenu() {
        Menu menu = mActionMode.getMenu();
        int navigation = Navigation.getNavigation();

        if (navigation == Navigation.TRASH) {
            menu.findItem(R.id.menu_untrash).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else {
            if (mAdapter.getSelectedItemCount() == 1) {
                menu.findItem(R.id.menu_share).setVisible(true);
            } else {
                menu.findItem(R.id.menu_share).setVisible(false);
            }
            menu.findItem(R.id.menu_category).setVisible(true);
            menu.findItem(R.id.menu_trash).setVisible(true);
        }
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
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // Expands the widget hiding other actionbar icons
        mSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setActionItemsVisibility(menu, hasFocus);
//                if (!hasFocus) {
//                    MenuItemCompat.collapseActionView(mSearchMenuItem);
//                }
            }
        });

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Reinitialize tasks list to all tasks when search is collapsed
                mSearchQuery = null;
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

    private void setActionItemsVisibility(Menu menu, boolean searchViewHasFocus) {
        // Defines the conditions to set actionbar items visible or not
        boolean drawerOpen = (getMainActivity().getDrawerLayout() != null && getMainActivity()
                .getDrawerLayout().isDrawerOpen(GravityCompat.START));
        boolean navigationTrash = Navigation.checkNavigation(Navigation.TRASH);

        if (!navigationTrash) {
            setFabAllowed(true);
            if (!drawerOpen) {
                showFab();
            }
        } else {
            setFabAllowed(false);
            hideFab();
        }
        menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_sort).setVisible(!drawerOpen && !searchViewHasFocus);
        menu.findItem(R.id.menu_empty_trash).setVisible(!drawerOpen && navigationTrash);
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
                case R.id.menu_sort:
                    initSortingSubmenu();
                    break;
                case R.id.menu_empty_trash:
                    emptyTrash();
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
                case R.id.menu_trash:
                    trashTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_untrash:
                    untrashTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_delete:
                    deleteTasks(mAdapter.getSelectedItems());
                    break;
                case R.id.menu_select_all:
                    selectAllTasks();
                    break;
            }
        }

        checkSortActionPerformed(item);

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
            case REQUEST_CODE_CATEGORY:
                // Dialog retarded to give time to activity's views of being
                // completely initialized
                // The dialog style is chosen depending on result code
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        UiUtils.showMessage(getActivity(), R.string.category_saved);
                        break;
                    case Activity.RESULT_FIRST_USER:
                        UiUtils.showMessage(getActivity(), R.string.category_deleted);
                        break;
                    default:
                        break;
                }

                break;

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


    private void checkSortActionPerformed(MenuItem item) {
        if (item.getGroupId() == Constants.MENU_SORT_GROUP_ID) {
            final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
            PrefUtils.putString(PrefUtils.PREF_SORTING_COLUMN, arrayDb[item.getOrder()]);
            initTasksList(getActivity().getIntent());
        }
    }

    /**
     * Empties trash deleting all the tasks
     */
    private void emptyTrash() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.empty_trash_confirmation)
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

    /**
     * Tasks list adapter initialization and association to view
     */
    public void initTasksList(Intent intent) {
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
                onTasksLoaded(DbHelper.getAllTasks());
            }
        }
    }

    private void onTasksLoaded(List<Task> tasks) {
        mAdapter.setTasks(tasks);
    }

    private void trashTasks(int[] positions) {
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
        Snackbar.make(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), R.string.task_trashed, Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.info))
                .setAction(R.string.undo, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onUndo();
                    }
                })
                .show();
        hideFab();
        mUndoTrash = true;
    }

    private void trashTaskExecute(Task task) {
        DbHelper.trashTask(task);
        mAdapter.getTasks().remove(task);

        if (!TextUtils.isEmpty(task.questId)) {
            Games.Events.increment(getMainActivity().getApiClient(), task.questId, 1);
        }
    }

    private void untrashTasks(int[] positions) {
        for (int position : positions) {
            Task task = mAdapter.getTasks().get(position);
            untrashTaskExecute(task);
        }

        finishActionMode();

        // Advice to user
        UiUtils.showMessage(getActivity(), R.string.task_untrashed);
    }

    private void untrashTaskExecute(Task task) {
        DbHelper.untrashTask(task);
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
     * Categories addition and editing
     */
    public void editCategory(Category category) {
        Intent categoryIntent = new Intent(getActivity(), CategoryActivity.class);
        categoryIntent.putExtra(Constants.INTENT_CATEGORY, Parcels.wrap(category));
        startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
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
            Snackbar.make(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), R.string.tasks_category_removed, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.info))
                    .setAction(R.string.undo, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onUndo();
                        }
                    })
                    .show();
            hideFab();
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
            } else { // Manages trash undo
                int position = mUndoTasksList.keyAt(mUndoTasksList.indexOfValue(task));
                mAdapter.getTasks().add(position, task);
                mAdapter.notifyItemInserted(position);
            }
        }

        mUndoTasksList.clear();
        mModifiedTasks.clear();

        mUndoTrash = false;
        mUndoCategorize = false;
        mUndoTasksList.clear();
        mUndoCategoryMap.clear();
        mUndoCategorizeCategory = null;

        finishActionMode();
    }

    public void commitPending() {
        if (mUndoTrash || mUndoCategorize) {

            for (Task task : mModifiedTasks) {
                if (mUndoTrash) {
                    trashTaskExecute(task);
                } else if (mUndoCategorize) {
                    categorizeTaskExecute(task, mUndoCategorizeCategory);
                }
            }

            mUndoTrash = false;
            mUndoCategorize = false;
            mUndoCategorizeCategory = null;

            // Clears data structures
            mModifiedTasks.clear();
            mUndoTasksList.clear();
            mUndoCategoryMap.clear();

            showFab();
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

    public MenuItem getSearchMenuItem() {
        return mSearchMenuItem;
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


}
