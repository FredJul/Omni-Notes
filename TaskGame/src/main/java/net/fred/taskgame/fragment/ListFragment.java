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
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.Html;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.activity.BaseActivity;
import net.fred.taskgame.activity.CategoryActivity;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.async.DeleteNoteTask;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.model.adapters.TaskAdapter;
import net.fred.taskgame.model.listeners.AbsListViewScrollDetector;
import net.fred.taskgame.model.listeners.OnViewTouchedListener;
import net.fred.taskgame.utils.AnimationsHelper;
import net.fred.taskgame.utils.BitmapHelper;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.CroutonHelper;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.view.InterceptorLinearLayout;
import net.fred.taskgame.view.UndoBarController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;

import static android.support.v4.view.ViewCompat.animate;


public class ListFragment extends Fragment implements OnViewTouchedListener, UndoBarController.UndoListener {

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
    private boolean goBackOnToggleSearchLabel = false;
    private boolean searchLabelActive = false;

    private TaskAdapter taskAdapter;
    private UndoBarController ubc;

    //    Fab
    private FloatingActionsMenu fab;
    private boolean fabAllowed;
    private boolean fabHidden = true;
    private boolean fabExpanded = false;

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

        // Restores savedInstanceState
        if (savedInstanceState != null) {
            getMainActivity().navigationTmp = savedInstanceState.getString("navigationTmp");
        }

        initFab();

        // Activity title initialization
        initTitle();

        ubc = new UndoBarController(getActivity().findViewById(R.id.undobar), this);

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
                    editNote(new Task(), v);
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
        list.setOnScrollListener(
                new AbsListViewScrollDetector() {
                    public void onScrollUp() {
                        if (fab != null) {
                            fab.collapse();
                            hideFab();
                        }
                    }

                    public void onScrollDown() {
                        if (fab != null) {
                            fab.collapse();
                            showFab();
                        }
                    }
                });

        fab.findViewById(R.id.fab_checklist).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = new Task();
                task.isChecklist = true;
                editNote(task, v);
            }
        });
        fab.findViewById(R.id.fab_camera).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getActivity().getIntent();
                i.setAction(Constants.ACTION_TAKE_PHOTO);
                getActivity().setIntent(i);
                editNote(new Task(), v);
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
        Crouton.cancelAllCroutons();

        // Clears data structures
        // getSelectedTasks().clear();
//		if (taskAdapter != null) {
//			taskAdapter.clearSelectedItems();
//		}
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
                    taskAdapter.restoreDrawable(taskAdapter.getItem(key), v.findViewById(R.id.card_layout));
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


            // Updates app widgets
            BaseActivity.notifyAppWidgets(getActivity());
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
            fabHidden = false;
        }
    }


    private void hideFab() {
        if (fab != null && !isFabHidden()) {
            fab.collapse();
            animateFab(fab.getHeight() + getMarginBottom(fab), View.VISIBLE, View.INVISIBLE);
            fabHidden = true;
        }
    }


    private boolean isFabHidden() {
        return fabHidden;
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
        LinearLayout v = (LinearLayout) view.findViewById(R.id.card_layout);
        if (!getSelectedTasks().contains(task)) {
            getSelectedTasks().add(task);
            taskAdapter.addSelectedItem(position);
            v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
        } else {
            getSelectedTasks().remove(task);
            taskAdapter.removeSelectedItem(position);
            taskAdapter.restoreDrawable(task, v);
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
        Context c = layout.getContext();
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
                    editNote(taskAdapter.getItem(position), view);
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

    private ImageView getZoomListItemView(View view, Task task) {
        final ImageView expandedImageView = (ImageView) getActivity().findViewById(R.id.expanded_image);
        View targetView = null;
        if (task.getAttachmentsList().size() > 0) {
            targetView = view.findViewById(R.id.attachmentThumbnail);
        }
        if (targetView == null && task.getCategory() != null) {
            targetView = view.findViewById(R.id.category_marker);
        }
        if (targetView == null) {
            targetView = new ImageView(getActivity());
            targetView.setBackgroundColor(Color.WHITE);
        }
        targetView.setDrawingCacheEnabled(true);
        targetView.buildDrawingCache();
        Bitmap bmp = targetView.getDrawingCache();
        expandedImageView.setBackgroundColor(BitmapHelper.getDominantColor(bmp));
        return expandedImageView;
    }

    /**
     * Listener that fires note opening once the zooming animation is finished
     */
    private AnimatorListenerAdapter buildAnimatorListenerAdapter(final Task task) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                editNote2(task);
            }
        };
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

            private boolean mSearchPerformed = false;

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Reinitialize tasks list to all tasks when search is collapsed
                searchQuery = null;
                if (getActivity().findViewById(R.id.search_layout).getVisibility() == View.VISIBLE) {
                    toggleSearchLabel(false);
                }
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
                        View searchLayout = getActivity().findViewById(R.id.search_layout);
                        if (searchLayout != null && mSearchPerformed) {
                            searchQuery = pattern;
                            onTasksLoaded(DbHelper.getTasksByPattern(searchQuery));
                            return true;
                        } else {
                            mSearchPerformed = true;
                            return false;
                        }
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
        boolean filterPastReminders = PrefUtils.getBoolean(PrefUtils.PREF_FILTER_PAST_REMINDERS, true);
        boolean navigationReminders = Navigation.checkNavigation(Navigation.REMINDERS);
        boolean navigationTrash = Navigation.checkNavigation(Navigation.TRASH);

        if (!navigationReminders && !navigationTrash) {
            setFabAllowed(true);
            if (!drawerOpen) {
                showFab();
            }
        } else {
            setFabAllowed(false);
            hideFab();
        }
        menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_filter).setVisible(!drawerOpen && !filterPastReminders && navigationReminders && !searchViewHasFocus);
        menu.findItem(R.id.menu_filter_remove).setVisible(!drawerOpen && filterPastReminders && navigationReminders && !searchViewHasFocus);
        menu.findItem(R.id.menu_sort).setVisible(!drawerOpen && !navigationReminders && !searchViewHasFocus);
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
                case R.id.menu_filter:
                    filterReminders(true);
                    break;
                case R.id.menu_filter_remove:
                    filterReminders(false);
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

    void editNote(final Task task, final View view) {
        hideFab();
        AnimationsHelper.zoomListItem(getActivity(), view, getZoomListItemView(view, task),
                getActivity().findViewById(R.id.list_root), buildAnimatorListenerAdapter(task));
    }


    void editNote2(Task task) {
        if (task.id == 0) {

            // if navigation is a tag it will be set into note
            try {
                int tagId;
                if (!TextUtils.isEmpty(getMainActivity().navigationTmp)) {
                    tagId = Integer.parseInt(getMainActivity().navigationTmp);
                } else {
                    tagId = Integer.parseInt(getMainActivity().navigation);
                }
                task.setCategory(DbHelper.getCategory(tagId));
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
                // The dialog style is choosen depending on result code
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getMainActivity().showMessage(R.string.category_saved, CroutonHelper.CONFIRM);
                        break;
                    case Activity.RESULT_FIRST_USER:
                        getMainActivity().showMessage(R.string.category_deleted, CroutonHelper.ALERT);
                        break;
                    default:
                        break;
                }

                break;

            case REQUEST_CODE_CATEGORY_TASKS:
                if (intent != null) {
                    Category tag = intent.getParcelableExtra(Constants.INTENT_CATEGORY);
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
            // Updates app widgets
            BaseActivity.notifyAppWidgets(getActivity());
        }
    }


    /**
     * Empties trash deleting all the tasks
     */
    private void emptyTrash() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.empty_trash_confirmation)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.SimpleCallback() {
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
        // Search for a tag
        // A workaround to simplify it's to simulate normal search
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getCategories() != null
                && intent.getCategories().contains(Intent.CATEGORY_BROWSABLE)) {
            goBackOnToggleSearchLabel = true;
        }

        // Searching
        if (searchQuery != null || Intent.ACTION_SEARCH.equals(intent.getAction())) {

            // Get the intent, verify the action and get the query
            if (intent.getStringExtra(SearchManager.QUERY) != null) {
                searchQuery = intent.getStringExtra(SearchManager.QUERY);
            }
            onTasksLoaded(DbHelper.getTasksByPattern(searchQuery));

            toggleSearchLabel(true);

        } else {
            // Check if is launched from a widget with categories to set tag
            if ((Constants.ACTION_WIDGET_SHOW_LIST.equals(intent.getAction()) && intent
                    .hasExtra(Constants.INTENT_WIDGET))
                    || !TextUtils.isEmpty(getMainActivity().navigationTmp)) {
                String widgetId = intent.hasExtra(Constants.INTENT_WIDGET) ? intent.getExtras()
                        .get(Constants.INTENT_WIDGET).toString() : null;
                if (widgetId != null) {
                    int categoryId = PrefUtils.getInt(PrefUtils.PREF_WIDGET_PREFIX + widgetId, -1);
                    getMainActivity().navigationTmp = (categoryId != -1 ? String.valueOf(categoryId) : null);
                }
                intent.removeExtra(Constants.INTENT_WIDGET);
                onTasksLoaded(DbHelper.getTasksByCategory(
                        getMainActivity().navigationTmp));

                // Gets all tasks
            } else {
                onTasksLoaded(DbHelper.getAllTasks());
            }
        }
    }


    public void toggleSearchLabel(boolean activate) {
        View searchLabel = getActivity().findViewById(R.id.search_layout);
        if (activate) {
            ((android.widget.TextView) getActivity().findViewById(R.id.search_query)).setText(Html.fromHtml("<i>"
                    + getString(R.string.search) + ":</i> " + searchQuery));
            searchLabel.setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.search_cancel).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSearchLabel(false);
                }
            });
            searchLabelActive = true;
        } else {
            if (searchLabelActive) {
                searchLabelActive = false;
                AnimationsHelper.expandOrCollapse(searchLabel, false);
                searchQuery = null;
                if (!goBackOnToggleSearchLabel) {
                    getActivity().getIntent().setAction(Intent.ACTION_MAIN);
                    if (searchView != null) {
                        MenuItemCompat.collapseActionView(searchMenuItem);
                    }
                    initTasksList(getActivity().getIntent());
                } else {
                    getActivity().onBackPressed();
                }
                goBackOnToggleSearchLabel = false;
                if (Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction())) {
                    getActivity().getIntent().setAction(null);
                }
            }
        }
    }

    private void onTasksLoaded(List<Task> tasks) {
        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(getActivity(), tasks);
            list.setAdapter(taskAdapter);
        } else {
            taskAdapter.setTasks(tasks);
        }

        // Restores list view position when turning back to list
        if (list != null && tasks.size() > 0) {
            if (list.getCount() > listViewPosition) {
                list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
            } else {
                list.setSelectionFromTop(0, 0);
            }
        }

        // Fade in the list view
        animate(list).setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(1);
    }


    /**
     * Batch note trashing
     */
    public void trashTasks(boolean trash) {
        int selectedTasksSize = getSelectedTasks().size();
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
            getMainActivity().showMessage(R.string.task_trashed, CroutonHelper.WARN);
        } else {
            getMainActivity().showMessage(R.string.task_untrashed, CroutonHelper.INFO);
        }

        // Creation of undo bar
        if (trash) {
            ubc.showUndoBar(false, selectedTasksSize + " " + getString(R.string.trashed), null);
            hideFab();
            undoTrash = true;
        } else {
            getSelectedTasks().clear();
        }
    }

    private android.support.v7.view.ActionMode getActionMode() {
        return actionMode;
    }


    private List<Task> getSelectedTasks() {
//        return taskAdapter.getSelectedTasks();
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
                v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
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
                .callback(new MaterialDialog.SimpleCallback() {
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
            DeleteNoteTask deleteNoteTask = new DeleteNoteTask(MainApplication.getContext());
            deleteNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, task);
        }

        // Clears data structures
//		taskAdapter.clearSelectedItems();
        list.clearChoices();

        finishActionMode();

        // Advice to user
        getMainActivity().showMessage(R.string.task_deleted, CroutonHelper.ALERT);
    }

    /**
     * Categories addition and editing
     */
    public void editCategory(Category category) {
        Intent categoryIntent = new Intent(getActivity(), CategoryActivity.class);
        categoryIntent.putExtra(Constants.INTENT_CATEGORY, category);
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
                .adapter(new NavDrawerCategoryAdapter(getActivity(), categories))
                .positiveText(R.string.add_category)
//                .neutralText(R.string.cancel)
                .negativeText(R.string.remove_category)
                .callback(new MaterialDialog.Callback() {
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
        String msg;
        if (category != null) {
            msg = getResources().getText(R.string.tasks_categorized_as) + " '" + category.name + "'";
        } else {
            msg = getResources().getText(R.string.tasks_category_removed).toString();
        }
        getMainActivity().showMessage(msg, CroutonHelper.INFO);

        // Creation of undo bar
        if (category == null) {
            ubc.showUndoBar(false, getString(R.string.tasks_category_removed), null);
            hideFab();
            undoCategorize = true;
            undoCategorizeCategory = null;
        } else {
            getSelectedTasks().clear();
        }
    }


    private void categorizeNote(Task task, Category category) {
        task.setCategory(category);
        DbHelper.updateTaskAsync(task, false);
    }

    @Override
    public void onUndo(Parcelable undoToken) {
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
        Crouton.cancelAllCroutons();

        if (getActionMode() != null) {
            getActionMode().finish();
        }
        ubc.hideUndoBar(false);
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

            ubc.hideUndoBar(false);
            showFab();

            BaseActivity.notifyAppWidgets(getActivity());
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

    /**
     * Excludes past reminders
     */
    private void filterReminders(boolean filter) {
        PrefUtils.putBoolean(PrefUtils.PREF_FILTER_PAST_REMINDERS, filter);
        // Change list view
        initTasksList(getActivity().getIntent());
        // Called to switch menu voices
        getActivity().supportInvalidateOptionsMenu();
    }


    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


}
