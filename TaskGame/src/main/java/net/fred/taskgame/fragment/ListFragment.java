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
import android.widget.LinearLayout;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.games.Games;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;

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
import net.fred.taskgame.service.SyncService;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.view.InterceptorLinearLayout;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.v4.view.ViewCompat.animate;


public class ListFragment extends Fragment implements OnViewTouchedListener {

    private static final int REQUEST_CODE_CATEGORY = 2;
    private static final int REQUEST_CODE_CATEGORY_TASKS = 3;

    private DynamicListView list;
    private final List<Task> selectedTasks = new ArrayList<>();
    private List<Task> modifiedTasks = new ArrayList<>();
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private Menu menu;
    private int listViewPosition;
    private int listViewPositionOffset;
    private android.support.v7.view.ActionMode actionMode;
    private boolean keepActionMode = false;

    // Undo archive/trash
    private boolean undoTrash = false;
    private boolean undoCategorize = false;
    private Category undoCategorizeCategory = null;
    private final SparseArray<Task> undoTasksList = new SparseArray<>();
    // Used to remember removed categories from tasks
    private final Map<Task, Category> undoCategoryMap = new HashMap<>();

    // Search variables
    private String searchQuery;

    private TaskAdapter taskAdapter;

    //    Fab
    private FloatingActionsMenu fab;
    private boolean fabAllowed;
    private boolean fabExpanded = false;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            initTasksList(getActivity().getIntent());

            // Just notify the SyncAdapter that there was a change
            SyncService.triggerSync(getContext(), false);
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
            if (savedInstanceState.containsKey("listViewPosition")) {
                listViewPosition = savedInstanceState.getInt("listViewPosition");
                listViewPositionOffset = savedInstanceState.getInt("listViewPositionOffset");
                searchQuery = savedInstanceState.getString("searchQuery");
            }
            keepActionMode = false;
        }
        View layout = inflater.inflate(R.layout.fragment_list, container, false);

        // List view initialization
        initListView(layout);

        // registers for callbacks from the specified tables
        mContentObserver.registerForContentChanges(inflater.getContext(), Task.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Category.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Attachment.class);

        return layout;
    }

    @Override
    public void onDestroyView() {
        mContentObserver.unregisterForContentChanges(getView().getContext());
        list = null;
        taskAdapter = null;

        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initFab();

        // Activity title initialization
        initTitle();

        initTasksList(getActivity().getIntent());
    }

    private void initFab() {
        fab = (FloatingActionsMenu) getActivity().findViewById(R.id.fab);
        AddFloatingActionButton fabAddButton = (AddFloatingActionButton) fab.findViewById(com.getbase
                .floatingactionbutton.R.id.fab_expand_menu_button);
        fabAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fabExpanded) {
                    fab.toggle();
                    fabExpanded = false;
                } else {
                    editTask(new Task());
                }
            }
        });
        fabAddButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                fabExpanded = !fabExpanded;
                fab.toggle();
                return true;
            }
        });

        fab.findViewById(R.id.fab_checklist).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = new Task();
                task.isChecklist = true;
                editTask(task);
            }
        });
        fab.findViewById(R.id.fab_camera).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getActivity().getIntent();
                i.setAction(Constants.ACTION_TAKE_PHOTO);
                getActivity().setIntent(i);
                editTask(new Task());
            }
        });
    }


    /**
     * Activity title initialization based on navigation
     */
    private void initTitle() {
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
        getMainActivity().setActionBarTitle(title.toString());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!keepActionMode) {
            commitPending();
            list.clearChoices();
            if (getActionMode() != null) {
                getActionMode().finish();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        refreshListScrollPosition();
        outState.putInt("listViewPosition", listViewPosition);
        outState.putInt("listViewPositionOffset", listViewPositionOffset);
        outState.putString("searchQuery", searchQuery);
    }


    private void refreshListScrollPosition() {
        if (list != null) {
            listViewPosition = list.getFirstVisiblePosition();
            View v = list.getChildAt(0);
            listViewPositionOffset = (v == null) ? 0 : v.getTop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Removes navigation drawer forced closed status
        if (getMainActivity().getDrawerLayout() != null) {
            getMainActivity().getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private final class ModeCallback implements android.support.v7.view.ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_list, menu);
            actionMode = mode;

            fabAllowed = false;
            hideFab();

            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are
            // deselected/unchecked.
            for (int i = 0; i < taskAdapter.getSelectedItems().size(); i++) {
                int key = taskAdapter.getSelectedItems().keyAt(i);
                View v = list.getChildAt(key - list.getFirstVisiblePosition());
                if (taskAdapter.getCount() > key && taskAdapter.getItem(key) != null && v != null) {
                    taskAdapter.restoreDrawable(taskAdapter.getItem(key), v);
                }
            }

            // Backups modified tasks in another structure to perform post-elaborations
            modifiedTasks = new ArrayList<>(getSelectedTasks());

            // Clears data structures
            selectedTasks.clear();
            taskAdapter.clearSelectedItems();
            list.clearChoices();

            setFabAllowed(true);
            if (undoTasksList.size() == 0) {
                showFab();
            }

            actionMode = null;
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
                fabAllowed = true;
            }
        } else {
            fabAllowed = false;
        }
    }


    private void showFab() {
        if (fab != null && fabAllowed && isFabHidden()) {
            animateFab(0, View.VISIBLE, View.VISIBLE);
        }
    }


    private void hideFab() {
        if (fab != null && !isFabHidden()) {
            fab.collapse();
            animateFab(fab.getHeight() + getMarginBottom(fab), View.VISIBLE, View.INVISIBLE);
        }
    }


    private boolean isFabHidden() {
        return fab.getVisibility() != View.VISIBLE;
    }


    private void animateFab(int translationY, final int visibilityBefore, final int visibilityAfter) {
        fab.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(Constants.FAB_ANIMATION_TIME)
                .translationY(translationY)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        fab.setVisibility(visibilityBefore);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        fab.setVisibility(visibilityAfter);
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
        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }


    /**
     * Manage check/uncheck of tasks in list during multiple selection phase
     */
    private void toggleListViewItem(View view, int position) {
        Task task = taskAdapter.getItem(position);
        View cardLayout = view.findViewById(R.id.card_layout);
        if (!getSelectedTasks().contains(task)) {
            getSelectedTasks().add(task);
            taskAdapter.addSelectedItem(position);
            cardLayout.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
        } else {
            getSelectedTasks().remove(task);
            taskAdapter.removeSelectedItem(position);
            taskAdapter.restoreDrawable(task, view);
        }
        prepareActionModeMenu();

        // Close CAB if no items are selected
        if (getSelectedTasks().size() == 0) {
            finishActionMode();
        }
    }


    /**
     * Tasks list initialization. Data, actions and callback are defined here.
     */
    private void initListView(View layout) {
        list = (DynamicListView) layout.findViewById(R.id.list);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        list.setItemsCanFocus(false);

        UiUtils.addEmptyFooterView(list, 50);

        // Note long click to start CAB mode
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
                if (getActionMode() != null) {
                    return false;
                }
                // Start the CAB using the ActionMode.Callback defined above
                ((MainActivity) getActivity()).startSupportActionMode(new ModeCallback());
                toggleListViewItem(view, position);
                setCabTitle();
                return true;
            }
        });

        // Note single click listener managed by the activity itself
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                if (getActionMode() == null) {
                    editTask(taskAdapter.getItem(position));
                    return;
                }
                // If in CAB mode
                toggleListViewItem(view, position);
                setCabTitle();
            }
        });

        ((InterceptorLinearLayout) layout.findViewById(R.id.list_root))
                .setOnViewTouchedListener(this);

        list.enableSwipeToDismiss(new OnDismissCallback() {
            @Override
            public void onDismiss(@NonNull ViewGroup viewGroup, @NonNull int[] reverseSortedPositions) {

                // Avoids conflicts with action mode
                finishActionMode();

                for (int position : reverseSortedPositions) {
                    Task task;
                    try {
                        task = taskAdapter.getItem(position);
                    } catch (IndexOutOfBoundsException e) {
                        continue;
                    }
                    getSelectedTasks().add(task);

                    // Depending on settings and note status this action will...
                    // ...restore
                    if (Navigation.checkNavigation(Navigation.TRASH)) {
                        trashTasks(false);
                    }
                    // ...removes category
                    else if (Navigation.checkNavigation(Navigation.CATEGORY)) {
                        categorizeTasksExecute(null);
                    } else {
                        trashTasks(true);
                    }
                }
            }
        });
        list.setEmptyView(layout.findViewById(R.id.empty_list));
    }

    @Override
    public void onViewTouchOccurred(MotionEvent ev) {
        commitPending();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
        this.menu = menu;
        // Initialization of SearchView
        initSearchView(menu);
    }


    private void initSortingSubmenu() {
        final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
        final String[] arrayDialog = getResources().getStringArray(R.array.sortable_columns_human_readable);
        int selected = Arrays.asList(arrayDb).indexOf(PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, arrayDb[0]));

        SubMenu sortMenu = this.menu.findItem(R.id.menu_sort).getSubMenu();
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
        Menu menu = getActionMode().getMenu();
        int navigation = Navigation.getNavigation();

        if (navigation == Navigation.TRASH) {
            menu.findItem(R.id.menu_untrash).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else {
            if (getSelectedCount() == 1) {
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


    private int getSelectedCount() {
        return getSelectedTasks().size();
    }


    private void setCabTitle() {
        if (getActionMode() != null) {
            int title = getSelectedCount();
            getActionMode().setTitle(String.valueOf(title));
        }
    }


    /**
     * SearchView initialization. It's a little complex because it's not using SearchManager but is implementing on its
     * own.
     */
    private void initSearchView(final Menu menu) {

        // Save item as class attribute to make it collapse on drawer opening
        searchMenuItem = menu.findItem(R.id.menu_search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // Expands the widget hiding other actionbar icons
        searchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setActionItemsVisibility(menu, hasFocus);
//                if (!hasFocus) {
//                    MenuItemCompat.collapseActionView(searchMenuItem);
//                }
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Reinitialize tasks list to all tasks when search is collapsed
                searchQuery = null;
                getActivity().getIntent().setAction(Intent.ACTION_MAIN);
                initTasksList(getActivity().getIntent());
                getActivity().supportInvalidateOptionsMenu();
                return true;
            }


            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setOnQueryTextListener(new OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String arg0) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String pattern) {
                        searchQuery = pattern;
                        onTasksLoaded(DbHelper.getTasksByPattern(searchQuery));
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
    public boolean performAction(MenuItem item, ActionMode actionMode) {
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
                    categorizeTasks();
                    break;
                case R.id.menu_share:
                    share();
                    break;
                case R.id.menu_trash:
                    trashTasks(true);
                    break;
                case R.id.menu_untrash:
                    trashTasks(false);
                    break;
                case R.id.menu_delete:
                    deleteTasks();
                    break;
                case R.id.menu_select_all:
                    selectAllTasks();
                    break;
            }
        }

        checkSortActionPerformed(item);

        return super.onOptionsItemSelected(item);
    }

    void editTask(final Task task) {
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

        // Current list scrolling position is saved to be restored later
        refreshListScrollPosition();

        // Fragments replacing
        getMainActivity().switchToDetail(task);
    }

    @Override
    public// Used to show a Crouton dialog after saved (or tried to) a note
    void onActivityResult(int requestCode, final int resultCode, Intent intent) {
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
                    categorizeTasksExecute(tag);
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
            // Resets list scrolling position
            listViewPositionOffset = 0;
            listViewPosition = 0;
            list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
        }
    }


    /**
     * Empties trash deleting all the tasks
     */
    private void emptyTrash() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.empty_trash_confirmation)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        for (int i = 0; i < taskAdapter.getCount(); i++) {
                            getSelectedTasks().add(taskAdapter.getItem(i));
                        }
                        deleteTasksExecute();
                    }
                }).build();
        dialog.show();
    }


    /**
     * Tasks list adapter initialization and association to view
     */
    public void initTasksList(Intent intent) {
        // Searching
        if (searchQuery != null || Intent.ACTION_SEARCH.equals(intent.getAction())) {

            // Get the intent, verify the action and get the query
            if (intent.getStringExtra(SearchManager.QUERY) != null) {
                searchQuery = intent.getStringExtra(SearchManager.QUERY);
            }
            onTasksLoaded(DbHelper.getTasksByPattern(searchQuery));
        } else {
            // Check if is launched from a widget with categories to set tag
            if ((Constants.ACTION_WIDGET_SHOW_LIST.equals(intent.getAction()) && intent
                    .hasExtra(Constants.INTENT_WIDGET))
                    || getMainActivity().getWidgetCatId() != -1) {
                String widgetId = intent.hasExtra(Constants.INTENT_WIDGET) ? intent.getExtras()
                        .get(Constants.INTENT_WIDGET).toString() : null;
                if (widgetId != null) {
                    long categoryId = PrefUtils.getLong(PrefUtils.PREF_WIDGET_PREFIX + widgetId, -1);
                    getMainActivity().mWidgetCatId = (categoryId != -1 ? categoryId : -1);
                }
                intent.removeExtra(Constants.INTENT_WIDGET);
                onTasksLoaded(DbHelper.getTasksByCategory(getMainActivity().getWidgetCatId()));

                // Gets all tasks
            } else {
                onTasksLoaded(DbHelper.getAllTasks());
            }
        }
    }

    private void onTasksLoaded(List<Task> tasks) {
        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(getActivity(), tasks);
            if (list != null) {
                list.setAdapter(taskAdapter);
            }
        } else {
            taskAdapter.setTasks(tasks);
        }

        // Restores list view position when turning back to list
        if (list != null) {
            if (tasks.size() > 0) {
                if (list.getCount() > listViewPosition) {
                    list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
                } else {
                    list.setSelectionFromTop(0, 0);
                }
            }

            // Fade in the list view
            animate(list).setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(1);
        }
    }


    /**
     * Batch note trashing
     */
    public void trashTasks(boolean trash) {
        for (Task task : getSelectedTasks()) {
            // Restore it performed immediately, otherwise undo bar
            if (trash) {
                // Saves tasks to be eventually restored at right position
                undoTasksList.put(taskAdapter.getPosition(task) + undoTasksList.size(), task);
                modifiedTasks.add(task);
            } else {
                trashTask(task, false);
            }
            // Removes note adapter
            taskAdapter.remove(task);
        }

        // If list is empty again Mr Jingles will appear again
        if (taskAdapter.getCount() == 0)
            list.setEmptyView(getActivity().findViewById(R.id.empty_list));

        finishActionMode();

        // Advice to user
        if (trash) {
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
            undoTrash = true;

        } else {
            UiUtils.showMessage(getActivity(), R.string.task_untrashed);
            getSelectedTasks().clear();
        }
    }

    private android.support.v7.view.ActionMode getActionMode() {
        return actionMode;
    }


    private List<Task> getSelectedTasks() {
        return selectedTasks;
    }


    /**
     * Single note logical deletion
     *
     * @param task Note to be deleted
     */
    protected void trashTask(Task task, boolean trash) {
        DbHelper.trashTask(task, trash);
        // Update adapter content
        taskAdapter.remove(task);
        // Informs about update

    }


    /**
     * Selects all tasks in list
     */
    private void selectAllTasks() {
        for (int i = 0; i < list.getChildCount(); i++) {
            LinearLayout v = (LinearLayout) list.getChildAt(i).findViewById(R.id.card_layout);
            // Checks null to avoid the footer
            if (v != null) {
                v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.list_bg_selected));
            }
        }
        selectedTasks.clear();
        for (int i = 0; i < taskAdapter.getCount(); i++) {
            selectedTasks.add(taskAdapter.getItem(i));
            taskAdapter.addSelectedItem(i);
        }
        prepareActionModeMenu();
        setCabTitle();
    }


    /**
     * Batch note permanent deletion
     */
    private void deleteTasks() {
        // Confirm dialog creation
        new MaterialDialog.Builder(getActivity())
                .content(R.string.delete_task_confirmation)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        deleteTasksExecute();
                    }
                }).build().show();
    }


    /**
     * Performs tasks permanent deletion after confirmation by the user
     */
    private void deleteTasksExecute() {
        for (Task task : getSelectedTasks()) {
            taskAdapter.remove(task);
            // Deleting note using DbHelper
            DbHelper.deleteTask(task);
            if (!TextUtils.isEmpty(task.questId)) {
                Games.Events.increment(getMainActivity().getApiClient(), task.questId, 1);
            }
        }

        // Clears data structures
//		taskAdapter.clearSelectedItems();
        list.clearChoices();

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
    private void categorizeTasks() {
        // Retrieves all available categories
        final List<Category> categories = DbHelper.getCategories();

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.categorize_as)
                .adapter(new NavDrawerCategoryAdapter(getActivity(), categories), null)
                .positiveText(R.string.add_category)
//                .neutralText(R.string.cancel)
                .negativeText(R.string.remove_category)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        keepActionMode = true;
                        Intent intent = new Intent(getActivity(), CategoryActivity.class);
                        intent.putExtra("noHome", true);
                        startActivityForResult(intent, REQUEST_CODE_CATEGORY_TASKS);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        categorizeTasksExecute(null);
                    }
                }).build();

        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                categorizeTasksExecute(categories.get(position));
            }
        });

        dialog.show();
    }

    private void categorizeTasksExecute(Category category) {
        for (Task task : getSelectedTasks()) {
            // If is restore it will be done immediately, otherwise the undo bar
            // will be shown
            if (category != null) {
                categorizeNote(task, category);
            } else {
                // Saves categories associated to eventually undo
                undoCategoryMap.put(task, task.getCategory());
                // Saves tasks to be eventually restored at right position
                undoTasksList.put(taskAdapter.getPosition(task) + undoTasksList.size(), task);
                modifiedTasks.add(task);
            }
            // Update adapter content if actual navigation is the category
            // associated with actually cycled note
            if (Navigation.checkNavigation(Navigation.CATEGORY) && !Navigation.checkNavigationCategory(category)) {
                taskAdapter.remove(task);
            } else {
                task.setCategory(category);
                taskAdapter.replace(task, taskAdapter.getPosition(task));
            }
        }

        // Clears data structures
//		taskAdapter.clearSelectedItems();
//		list.clearChoices();
        finishActionMode();

        if (getActionMode() != null) {
            getActionMode().finish();
        }

        // Advice to user
        if (category != null) {
            UiUtils.showMessage(getActivity(), getResources().getText(R.string.tasks_categorized_as) + " '" + category.name + "'");
            getSelectedTasks().clear();
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
            undoCategorize = true;
            undoCategorizeCategory = null;
        }
    }


    private void categorizeNote(Task task, Category category) {
        task.setCategory(category);
        DbHelper.updateTask(task, false);
    }

    public void onUndo() {
        // Cycles removed items to re-insert into adapter
        for (Task task : modifiedTasks) {
            // Manages uncategorize or archive undo
            if ((undoCategorize && !Navigation.checkNavigationCategory(undoCategoryMap.get(task)))) {
                if (undoCategorize) {
                    task.setCategory(undoCategoryMap.get(task));
                }

                taskAdapter.replace(task, taskAdapter.getPosition(task));
                taskAdapter.notifyDataSetChanged();
                // Manages trash undo
            } else {
                list.insert(undoTasksList.keyAt(undoTasksList.indexOfValue(task)), task);
            }
        }

        selectedTasks.clear();
        undoTasksList.clear();
        modifiedTasks.clear();

        undoTrash = false;
        undoCategorize = false;
        undoTasksList.clear();
        undoCategoryMap.clear();
        undoCategorizeCategory = null;

        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }


    public void commitPending() {
        if (undoTrash || undoCategorize) {

            for (Task task : modifiedTasks) {
                if (undoTrash)
                    trashTask(task, true);
                else if (undoCategorize) categorizeNote(task, undoCategorizeCategory);
            }

            undoTrash = false;
            undoCategorize = false;
            undoCategorizeCategory = null;

            // Clears data structures
            selectedTasks.clear();
            modifiedTasks.clear();
            undoTasksList.clear();
            undoCategoryMap.clear();
            list.clearChoices();

            showFab();
        }
    }

    /**
     * Shares the selected note from the list
     */
    private void share() {
        // Only one note should be selected to perform sharing but they'll be cycled anyhow
        for (final Task task : getSelectedTasks()) {
            task.share(getActivity());
        }

        getSelectedTasks().clear();
        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }

    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


}
