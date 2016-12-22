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

package net.fred.taskgame.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.fred.taskgame.R;
import net.fred.taskgame.fragments.DetailFragment;
import net.fred.taskgame.fragments.ListFragment;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.NavigationUtils;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.RxFirebase;
import net.fred.taskgame.utils.ThrottledContentObserver;
import net.fred.taskgame.utils.UiUtils;
import net.frju.androidquery.gen.Q;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CODE_CATEGORY = 2;
    private static final int REQUEST_CODE_SIGN_IN = 3;

    private FragmentManager mFragmentManager;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.fab)
    FloatingActionButton mFab;
    @BindView(R.id.navigation_view)
    NavigationView mNavigationView;

    private View mDrawerHeader;
    private ImageView mPlayerImageView;
    private TextView mPlayerName;
    private TextView mCurrentPoints;
    private ActionBarDrawerToggle mDrawerToggle;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private final ThrottledContentObserver mContentObserver = new ThrottledContentObserver(new Handler(), 100) {
        @Override
        public void onChangeThrottled() {
            initNavigationMenu();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mCurrentPointsObserver = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (mCurrentPoints != null && PrefUtils.PREF_CURRENT_POINTS.equals(key)) {
                mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));
            }
        }
    };

    private DatabaseReference mFirebaseDatabase;

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

        if (mFragmentManager.findFragmentByTag(ListFragment.class.getName()) == null) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, new ListFragment(), ListFragment.class.getName()).commit();
        }

        // Handling of Intent actions
        handleIntents();

        //Listen for changes in the back stack
        mFragmentManager.addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        displayHomeOrUpIcon();

        // registers for callbacks from the specified tables
        getContentResolver().registerContentObserver(Q.Task.getContentUri(), true, mContentObserver);
        getContentResolver().registerContentObserver(Q.Category.getContentUri(), true, mContentObserver);

        PrefUtils.registerOnPrefChangeListener(mCurrentPointsObserver);

        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.setItemIconTintList(null);
        initNavigationMenu();
    }

    private void firebaseLogin() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            mFirebaseDatabase = FirebaseDatabase.getInstance().getReference();
            mCompositeDisposable.add(RxFirebase.observeChildren(DbUtils.getFirebaseTasksNode())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<RxFirebase.FirebaseChildEvent>() {
                        @Override
                        public void accept(RxFirebase.FirebaseChildEvent ev) throws Exception {
                            switch (ev.eventType) {
                                case CHILD_ADDED:
                                case CHILD_CHANGED: {
                                    Task task = ev.snapshot.getValue(Task.class);
                                    task.id = ev.snapshot.getKey();
                                    Q.Task.save(task).query();
                                    break;
                                }
                                case CHILD_REMOVED: {
                                    Task task = new Task(); // no need to copy everything, only id needed
                                    task.id = ev.snapshot.getKey();
                                    Q.Task.delete().model(task).query();
                                    break;
                                }
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Dog.e("Error", throwable);
                        }
                    }));

            mCompositeDisposable.add(RxFirebase.observeChildren(DbUtils.getFirebaseCategoriesNode())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<RxFirebase.FirebaseChildEvent>() {
                        @Override
                        public void accept(RxFirebase.FirebaseChildEvent ev) throws Exception {
                            switch (ev.eventType) {
                                case CHILD_ADDED:
                                case CHILD_CHANGED: {
                                    Category category = ev.snapshot.getValue(Category.class);
                                    category.id = ev.snapshot.getKey();
                                    Q.Category.save(category).query();
                                    break;
                                }
                                case CHILD_REMOVED: {
                                    Category category = new Category(); // no need to copy everything, only id needed
                                    category.id = ev.snapshot.getKey();
                                    Q.Category.delete().model(category).query();
                                    break;
                                }
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Dog.e("Error", throwable);
                        }
                    }));

            mCompositeDisposable.add(RxFirebase.observeSingle(DbUtils.getFirebaseCurrentUserNode().child(DbUtils.FIREBASE_CURRENT_POINTS_NODE_NAME))
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<DataSnapshot>() {
                        @Override
                        public void accept(DataSnapshot snapshot) throws Exception {
                            if (snapshot.getValue() != null) {
                                PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, snapshot.getValue(Long.class));
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Dog.e("Error", throwable);
                        }
                    }));

            mPlayerName.setText(firebaseUser.getDisplayName());
            Glide.with(MainActivity.this).load(firebaseUser.getPhotoUrl()).asBitmap().fitCenter().fallback(android.R.drawable.sym_def_app_icon).placeholder(android.R.drawable.sym_def_app_icon).into(new BitmapImageViewTarget(mPlayerImageView) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    getView().setImageDrawable(circularBitmapDrawable);
                }
            });
        }
    }

    private void firebaseLogout() {
        mCompositeDisposable.clear();
    }

    @Override
    protected void onDestroy() {
        getContentResolver().unregisterContentObserver(mContentObserver);
        PrefUtils.unregisterOnPrefChangeListener(mCurrentPointsObserver);
        firebaseLogout();

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

            case REQUEST_CODE_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    firebaseLogin();

                    // We successfully logged in, let's add on firebase what we have
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final DatabaseReference categoriesFirebase = DbUtils.getFirebaseCategoriesNode();
                            if (categoriesFirebase != null) {
                                for (final Category category : DbUtils.getCategories()) {
                                    categoriesFirebase.child(String.valueOf(category.id)).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            if (!snapshot.exists()) {
                                                Dog.d("add cat: " + category.id);
                                                categoriesFirebase.child(String.valueOf(category.id)).setValue(category);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                        }
                                    });
                                }
                            }

                            final DatabaseReference tasksFirebase = DbUtils.getFirebaseTasksNode();
                            if (tasksFirebase != null) {
                                for (final Task task : DbUtils.getTasks()) {
                                    tasksFirebase.child(String.valueOf(task.id)).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            if (!snapshot.exists()) {
                                                Dog.d("add task: " + task.id);
                                                tasksFirebase.child(String.valueOf(task.id)).setValue(task);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                        }
                                    });
                                }
                            }
                        }
                    }).run();
                }
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
        String currentNavigation = NavigationUtils.getNavigation();

        MenuItem item = menu.add(1, R.string.drawer_tasks_item, Menu.NONE, R.string.drawer_tasks_item);
        item.setIcon(R.drawable.ic_assignment_grey600_24dp);
        long activeTaskCount = DbUtils.getActiveTaskCount();
        if (activeTaskCount > 0) {
            item.setActionView(R.layout.menu_counter);
            ((TextView) item.getActionView()).setText(String.valueOf(activeTaskCount));
        }
        if (NavigationUtils.TASKS.equals(currentNavigation)) {
            item.setChecked(true);
        }
        nbItems++;

        if (DbUtils.getFinishedTaskCount() > 0) {
            item = menu.add(1, R.string.drawer_finished_tasks_item, Menu.NONE, R.string.drawer_finished_tasks_item);
            item.setIcon(R.drawable.ic_assignment_turned_in_grey600_24dp);
            if (NavigationUtils.FINISHED_TASKS.equals(currentNavigation)) {
                item.setChecked(true);
            }
            nbItems++;
        }

        // Retrieves data to fill tags list
        for (Category category : DbUtils.getCategories()) {
            item = menu.add(1, R.string.category, Menu.NONE, category.name);
            long categoryCount = DbUtils.getActiveTaskCountByCategory(category);
            if (categoryCount > 0) {
                item.setActionView(R.layout.menu_counter);
                ((TextView) item.getActionView()).setText(String.valueOf(categoryCount));
            }
            Intent extraIntent = new Intent();
            extraIntent.putExtra("category", category.id);
            item.setIntent(extraIntent);
            item.setIcon(new ColorDrawable(category.color));
            if (category.id.equals(currentNavigation)) {
                item.setChecked(true);
            }

            nbItems++;
            listCategories.put(nbItems, category);
        }

        menu.setGroupCheckable(1, true, true);

        item = menu.add(Menu.NONE, R.string.find_games, Menu.NONE, R.string.find_games);
        item.setIcon(R.drawable.ic_mood_grey600_24dp);

        item = menu.add(Menu.NONE, R.string.settings, Menu.NONE, R.string.settings);
        item.setIcon(R.drawable.ic_settings_grey600_24dp);

        mNavigationView.post(new Runnable() {
            @Override
            public void run() {
                // Initialized the views which was not inflated before
                if (mDrawerHeader == null && !isFinishing() && !isDestroyed()) {
                    mDrawerHeader = mNavigationView.findViewById(R.id.drawer_header);
                    mDrawerHeader.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            if (auth.getCurrentUser() == null) {
                                startActivityForResult(
                                        AuthUI.getInstance().createSignInIntentBuilder()
                                                .setTheme(R.style.AppTheme)
                                                .setLogo(R.mipmap.ic_launcher)
                                                .setProviders(new String[]{AuthUI.GOOGLE_PROVIDER, AuthUI.EMAIL_PROVIDER}) //, AuthUI.FACEBOOK_PROVIDER})
                                                .build(),
                                        REQUEST_CODE_SIGN_IN);
                            } else {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(R.string.sign_out_confirmation)
                                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                AuthUI.getInstance().signOut(MainActivity.this);

                                                Glide.with(MainActivity.this).load(android.R.drawable.sym_def_app_icon).into(mPlayerImageView);
                                                mPlayerName.setText(R.string.not_logged_in);
                                                firebaseLogout();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }
                                        }).show();
                            }
                        }
                    });
                    mCurrentPoints = (TextView) mDrawerHeader.findViewById(R.id.currentPoints);
                    mPlayerImageView = (ImageView) mDrawerHeader.findViewById(R.id.player);

                    mPlayerName = (TextView) mDrawerHeader.findViewById(R.id.player_name);

                    firebaseLogin();

                    mCurrentPoints.setText(String.valueOf(PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0)));
                }

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
            case R.string.find_games:
                final String appPackageName = "net.fred.taskgame.hero";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                break;
            case R.string.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            default: // tasks, finished tasks, categories
                // Reset intent
                getIntent().setAction(Intent.ACTION_MAIN);

                if (item.getItemId() == R.string.drawer_tasks_item) {
                    updateNavigation(NavigationUtils.TASKS);
                } else if (item.getItemId() == R.string.drawer_finished_tasks_item) {
                    updateNavigation(NavigationUtils.FINISHED_TASKS);
                } else { // This is a category
                    updateNavigation(item.getIntent().getStringExtra("category"));
                }
                break;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void updateNavigation(String navigation) {
        NavigationUtils.setNavigation(navigation);

        if (getIntent() != null && getIntent().hasExtra(Constants.INTENT_WIDGET)) {
            getIntent().removeExtra(Constants.INTENT_WIDGET);
            setIntent(getIntent());
        }
    }

    public String getWidgetCatId() {
        // Check if is launched from a widget with categories to set tag
        if (getIntent() != null && getIntent().hasExtra(Constants.INTENT_WIDGET)) {
            //noinspection ConstantConditions
            String widgetId = getIntent().getExtras().get(Constants.INTENT_WIDGET).toString();
            return PrefUtils.getString(PrefUtils.PREF_WIDGET_PREFIX + widgetId, null);
        }

        return null;
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

    @Override
    public void onBackPressed() {
        // DetailFragment
        Fragment f = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
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
        ListFragment fragment = (ListFragment) mFragmentManager.findFragmentByTag(ListFragment.class.getName());
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
                task = DbUtils.getTask(i.getStringExtra(Constants.INTENT_TASK_ID));
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
        return Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())
                || Constants.ACTION_WIDGET.equals(i.getAction())
                || ((Intent.ACTION_SEND.equals(i.getAction())
                || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
                || Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
                && i.getType() != null)
                || (i.getAction() != null && i.getAction().contains(Constants.ACTION_NOTIFICATION_CLICK));
    }


    private boolean isTaskAlreadyOpened(Task task) {
        DetailFragment detailFragment = (DetailFragment) mFragmentManager.findFragmentByTag(DetailFragment.class.getName());
        return detailFragment != null && detailFragment.getCurrentTask().id.equals(task.id);
    }

    public void switchToList() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, new ListFragment(), ListFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    public void switchToDetail(Task task) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, DetailFragment.newInstance(task), DetailFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }
}
