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

package net.fred.taskgame.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.quest.Quests;

import net.fred.taskgame.R;
import net.fred.taskgame.fragment.DetailFragment;
import net.fred.taskgame.fragment.ListFragment;
import net.fred.taskgame.fragment.SketchFragment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.listeners.OnPermissionRequestedListener;
import net.fred.taskgame.service.SyncService;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.NavigationUtils;
import net.fred.taskgame.utils.PermissionsHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.utils.UiUtils;

import org.parceler.Parcels;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseGameActivity implements FragmentManager.OnBackStackChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CODE_CATEGORY = 2;
    private static final int REQUEST_CODE_QUESTS = 3;

    public final String FRAGMENT_LIST_TAG = "fragment_list";
    public final String FRAGMENT_DETAIL_TAG = "fragment_detail";
    public final String FRAGMENT_SKETCH_TAG = "fragment_sketch";

    private FragmentManager mFragmentManager;

    public Uri sketchUri;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.fab)
    FloatingActionButton mFab;
    @Bind(R.id.navigation_view)
    NavigationView mNavigationView;

    private ImageView mPlayerImageView;
    private TextView mPlayerName;
    private Button mLeaderboardBtn;
    private Button mQuestsBtn;
    private TextView mCurrentPoints;
    private ActionBarDrawerToggle mDrawerToggle;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            initNavigationMenu();
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener mCurrentPointsObserver = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (mCurrentPoints != null && PrefUtils.PREF_CURRENT_POINTS.equals(key)) {
                mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // ActionBarDrawerToggle± ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close);

        // just styling option
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Commits all pending actions
                commitPending();
                // Finishes action mode
                finishActionMode();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        mDrawerToggle.syncState();

        mFragmentManager = getSupportFragmentManager();

        if (mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG) == null) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, new ListFragment(), FRAGMENT_LIST_TAG).commit();
        }

        // Handling of Intent actions
        handleIntents();

        //Listen for changes in the back stack
        mFragmentManager.addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        displayHomeOrUpIcon();

        // registers for callbacks from the specified tables
        mContentObserver.registerForContentChanges(this, Task.class);
        mContentObserver.registerForContentChanges(this, Category.class);

        PrefUtils.registerOnPrefChangeListener(mCurrentPointsObserver);

        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.setItemIconTintList(null);
        initNavigationMenu();
    }

    @Override
    protected void onDestroy() {
        mContentObserver.unregisterForContentChanges(this);
        PrefUtils.unregisterOnPrefChangeListener(mCurrentPointsObserver);

        super.onDestroy();
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
                        UiUtils.showMessage(this, R.string.category_saved);
                        break;
                    case Activity.RESULT_FIRST_USER:
                        UiUtils.showMessage(this, R.string.category_deleted);
                        break;
                    default:
                        break;
                }

                break;
            case REQUEST_CODE_QUESTS:
                // Just to refresh the list of quests if one has been accepted
                SyncService.triggerSync(this);
                break;
            default:
                break;
        }
    }

    private void initNavigationMenu() {
        Menu menu = mNavigationView.getMenu();
        menu.clear();
        int nbItems = 0;
        final SparseArray<Category> listCategories = new SparseArray<>();
        long currentNavigation = NavigationUtils.getNavigation();

        MenuItem item = menu.add(1, R.string.drawer_tasks_item, Menu.NONE, R.string.drawer_tasks_item);
        item.setIcon(R.drawable.ic_assignment_grey600_24dp);
        if (currentNavigation == NavigationUtils.TASKS) {
            item.setChecked(true);
        }
        nbItems++;

        if (DbHelper.getFinishedTaskCount() != 0) {
            item = menu.add(1, R.string.drawer_finished_tasks_item, Menu.NONE, R.string.drawer_finished_tasks_item);
            item.setIcon(R.drawable.ic_assignment_turned_in_grey600_24dp);
            if (currentNavigation == NavigationUtils.FINISHED_TASKS) {
                item.setChecked(true);
            }
            nbItems++;
        }

        // Retrieves data to fill tags list
        for (Category category : DbHelper.getCategories()) {
            item = menu.add(1, R.string.category, Menu.NONE, category.name);
            Intent extraIntent = new Intent();
            extraIntent.putExtra("category", category.id);
            item.setIntent(extraIntent);
            item.setIcon(new ColorDrawable(category.color));
            if (currentNavigation == category.id) {
                item.setChecked(true);
            }

            nbItems++;
            listCategories.put(nbItems, category);
        }

        menu.setGroupCheckable(1, true, true);

        item = menu.add(Menu.NONE, R.string.settings, Menu.NONE, R.string.settings);
        item.setIcon(R.drawable.ic_settings_grey600_24dp);

        mNavigationView.post(new Runnable() {
            @Override
            public void run() {
                // Initialized the views which was not inflated before
                if (mCurrentPoints == null) {
                    mCurrentPoints = (TextView) mNavigationView.findViewById(R.id.currentPoints);

                    mPlayerImageView = (ImageView) mNavigationView.findViewById(R.id.player);
                    mPlayerImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PermissionsHelper.requestPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS,
                                    R.string.permission_get_account, new OnPermissionRequestedListener() {
                                        @Override
                                        public void onPermissionGranted() {
                                            beginUserInitiatedSignIn();
                                        }
                                    });
                        }
                    });

                    mPlayerName = (TextView) mNavigationView.findViewById(R.id.player_name);

                    mLeaderboardBtn = (Button) mNavigationView.findViewById(R.id.leaderboard_btn);
                    mLeaderboardBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(), Constants.LEADERBOARD_ID), 0);
                            } catch (Exception ignored) {
                            }
                        }
                    });

                    mQuestsBtn = (Button) mNavigationView.findViewById(R.id.quests_btn);
                    mQuestsBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                startActivityForResult(Games.Quests.getQuestsIntent(getApiClient(), Quests.SELECT_ALL_QUESTS), REQUEST_CODE_QUESTS);
                            } catch (Exception ignored) {
                            }
                        }
                    });
                }
                mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));


                // Small hack to handle the long press on menu item
                ViewGroup navigationList = (ViewGroup) mNavigationView.findViewById(android.support.design.R.id.design_navigation_view);
                for (int i = 0; i < listCategories.size(); i++) {
                    final int catPos = listCategories.keyAt(i);
                    View categoryView = navigationList.getChildAt(catPos);
                    if (categoryView != null) {
                        categoryView.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                editCategory(listCategories.get(catPos));
                                return true;
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Categories addition and editing
     */
    public void editCategory(Category category) {
        Intent categoryIntent = new Intent(this, CategoryActivity.class);
        categoryIntent.putExtra(Constants.INTENT_CATEGORY, Parcels.wrap(category));
        startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntents();

        super.onNewIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.string.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            default: // tasks, finished tasks, categories
                commitPending();
                // Reset intent
                getIntent().setAction(Intent.ACTION_MAIN);

                if (item.getItemId() == R.string.drawer_tasks_item) {
                    updateNavigation(NavigationUtils.TASKS);
                } else if (item.getItemId() == R.string.drawer_finished_tasks_item) {
                    updateNavigation(NavigationUtils.FINISHED_TASKS);
                } else { // This is a category
                    updateNavigation(item.getIntent().getLongExtra("category", 0));
                }
                break;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void updateNavigation(long navigation) {
        NavigationUtils.setNavigation(navigation);

        if (getIntent() != null && getIntent().hasExtra(Constants.INTENT_WIDGET)) {
            getIntent().removeExtra(Constants.INTENT_WIDGET);
            setIntent(getIntent());
        }
    }

    public long getWidgetCatId() {
        // Check if is launched from a widget with categories to set tag
        if (getIntent() != null && getIntent().hasExtra(Constants.INTENT_WIDGET)) {
            //noinspection ConstantConditions
            String widgetId = getIntent().getExtras().get(Constants.INTENT_WIDGET).toString();
            return PrefUtils.getLong(PrefUtils.PREF_WIDGET_PREFIX + widgetId, -1);
        }

        return -1;
    }

    @Override
    public void onSignInSucceeded() {
        PrefUtils.putBoolean(PrefUtils.PREF_ALREADY_LOGGED_TO_GAMES, true);
        SyncService.triggerSync(this);

        Player player = Games.Players.getCurrentPlayer(getApiClient());
        Glide.with(this).load(player.getIconImageUrl()).asBitmap().centerCrop().into(new BitmapImageViewTarget(mPlayerImageView) {
            @Override
            protected void setResource(Bitmap resource) {
                RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                getView().setImageDrawable(circularBitmapDrawable);
            }
        });
        mPlayerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.sign_out_confirmation)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                signOut();
                                Glide.with(MainActivity.this).load(android.R.drawable.sym_def_app_icon).into(mPlayerImageView);
                                mPlayerName.setText(R.string.not_logged_in);
                                mLeaderboardBtn.setVisibility(View.GONE);
                                mQuestsBtn.setVisibility(View.GONE);

                                mPlayerImageView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        PermissionsHelper.requestPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS,
                                                R.string.permission_get_account, new OnPermissionRequestedListener() {
                                                    @Override
                                                    public void onPermissionGranted() {
                                                        beginUserInitiatedSignIn();
                                                    }
                                                });
                                    }
                                });
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        }).show();
            }
        });

        mPlayerName.setText(player.getDisplayName());

        mLeaderboardBtn.setVisibility(View.VISIBLE);
        mQuestsBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSignInFailed() {
    }

    public void commitPending() {
        Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            ((ListFragment) f).commitPending();
        }
    }

    /**
     * Checks if allocated fragment is of the required type and then returns it or returns null
     */
    private Fragment checkFragmentInstance(int id, Object instanceClass) {
        Fragment result = null;
        if (mFragmentManager != null) {
            Fragment fragment = mFragmentManager.findFragmentById(id);
            if (instanceClass.equals(fragment.getClass())) {
                result = fragment;
            }
        }
        return result;
    }

    @Override
    public void onBackStackChanged() {
        displayHomeOrUpIcon();
    }

    public void displayHomeOrUpIcon() {
        //Enable Up button only if there are entries in the back stack
        boolean canUp = getSupportFragmentManager().getBackStackEntryCount() > 0;

        if (canUp) {
            mFab.hide();
        }

        mDrawerLayout.setDrawerLockMode(canUp ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
        // Use the drawerToggle to animate the icon
        ValueAnimator anim = ValueAnimator.ofFloat(canUp ? 0 : 1, canUp ? 1 : 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float slideOffset = (Float) valueAnimator.getAnimatedValue();
                mDrawerToggle.onDrawerSlide(mDrawerLayout, slideOffset);
            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(300);
        anim.start();
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

//TODO use that to automatically open drawer or popBackStack
//    @Override
//    public boolean onSupportNavigateUp() {
//        //This method is called when the up button is pressed. Just the pop back stack.
//        getSupportFragmentManager().popBackStack();
//        return true;
//    }

    /*
     * (non-Javadoc)
     * @see android.support.v7.app.ActionBarActivity#onBackPressed()
     *
     * Overrides the onBackPressed behavior for the attached fragments
     */
    @Override
    public void onBackPressed() {
        // SketchFragment
        Fragment f = checkFragmentInstance(R.id.fragment_container, SketchFragment.class);
        if (f != null) {
            ((SketchFragment) f).save();

            // Removes forced portrait orientation for this fragment
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            mFragmentManager.popBackStack();
            return;
        }

        // DetailFragment
        f = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
        if (f != null) {
            ((DetailFragment) f).saveAndExit();
            return;
        }

        // Before exiting from app the navigation drawer is opened
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Finishes multiselection mode started by ListFragment
     */
    public void finishActionMode() {
        ListFragment fragment = (ListFragment) mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG);
        if (fragment != null) {
            fragment.finishActionMode();
        }
    }

    private void handleIntents() {
        Intent i = getIntent();

        if (i.getAction() == null) return;

        if (receivedIntent(i)) {
            Task task = Parcels.unwrap(i.getParcelableExtra(Constants.INTENT_TASK));
            if (task == null) {
                task = DbHelper.getTask(i.getLongExtra(Constants.INTENT_KEY, 0));
            }
            // Checks if the same note is already opened to avoid to open again
            if (task != null && isTaskAlreadyOpened(task)) {
                return;
            }
            // Empty note instantiation
            if (task == null) {
                task = new Task();
            }
            switchToDetail(task);
        }

        // Tag search
        if (Intent.ACTION_VIEW.equals(i.getAction())) {
            switchToList();
        }
    }

    private boolean receivedIntent(Intent i) {
        return Constants.ACTION_SHORTCUT.equals(i.getAction())
                || Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())
                || Constants.ACTION_WIDGET.equals(i.getAction())
                || Constants.ACTION_TAKE_PHOTO.equals(i.getAction())
                || ((Intent.ACTION_SEND.equals(i.getAction())
                || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
                || Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
                && i.getType() != null)
                || (i.getAction() != null && i.getAction().contains(Constants.ACTION_NOTIFICATION_CLICK));
    }


    private boolean isTaskAlreadyOpened(Task task) {
        DetailFragment detailFragment = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
        return detailFragment != null && detailFragment.getCurrentTask().id == task.id;
    }

    public void switchToList() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TRANSITION_HORIZONTAL);
        ListFragment mListFragment = new ListFragment();
        transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack(FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
    }

    public void switchToDetail(Task task) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TRANSITION_HORIZONTAL);
        DetailFragment mDetailFragment = new DetailFragment();
        Bundle b = new Bundle();
        b.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task));
        mDetailFragment.setArguments(b);
        if (mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG) == null) {
            transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG).addToBackStack(FRAGMENT_LIST_TAG).commitAllowingStateLoss();
        } else {
            transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG).addToBackStack(FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
        }
    }
}
