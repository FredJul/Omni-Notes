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

package net.fred.taskgame.activities

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.fred.taskgame.R
import net.fred.taskgame.fragments.DetailFragment
import net.fred.taskgame.fragments.ListFragment
import net.fred.taskgame.models.Category
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.*
import net.frju.androidquery.gen.Q
import net.frju.androidquery.utils.ThrottledContentObserver
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import org.parceler.Parcels

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener, NavigationView.OnNavigationItemSelectedListener {

    private val drawer_header by lazy { navigation_view.findViewById(R.id.drawer_header) as ViewGroup }
    private val navigation_menu_view by lazy { navigation_view.findViewById(R.id.design_navigation_view) as ViewGroup }
    private val player_image by lazy { drawer_header.findViewById(R.id.player_image) as ImageView }
    private val player_name by lazy { drawer_header.findViewById(R.id.player_name) as TextView }
    private val current_points by lazy { drawer_header.findViewById(R.id.current_points) as TextView }
    private val drawer_toggle by lazy {
        ActionBarDrawerToggle(this,
                drawer_layout,
                R.string.drawer_open,
                R.string.drawer_close)
    }

    private val compositeDisposable = CompositeDisposable()

    val lazyFab: FloatingActionButton by lazy { fab }
    val lazyDrawerLayout: DrawerLayout by lazy { drawer_layout }

    private var drawerInitialized = false

    private val contentObserver = object : ThrottledContentObserver(Handler(), 100) {
        override fun onChangeThrottled() {
            initNavigationMenu()
        }
    }

    private val currentPointsObserver = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (PrefUtils.PREF_CURRENT_POINTS == key) {
            current_points.text = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0).toString()
        }
    }

    private var firebaseDatabase: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // just styling option
        drawer_layout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        drawer_toggle.isDrawerIndicatorEnabled = true
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                // Finishes action mode
                finishActionMode()
            }

            override fun onDrawerClosed(drawerView: View) {
                // Call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
        drawer_toggle.syncState()

        if (supportFragmentManager.findFragmentByTag(ListFragment::class.java.name) == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.fragment_container, ListFragment(), ListFragment::class.java.name).commit()
        }

        // Handling of Intent actions
        handleIntents()

        //Listen for changes in the back stack
        supportFragmentManager.addOnBackStackChangedListener(this)
        //Handle when activity is recreated like on orientation Change
        displayHomeOrUpIcon()

        // registers for callbacks from the specified tables
        contentResolver.registerContentObserver(Q.Task.getContentUri(), true, contentObserver)
        contentResolver.registerContentObserver(Q.Category.getContentUri(), true, contentObserver)

        navigation_view.setNavigationItemSelectedListener(this)
        navigation_view.itemIconTintList = null

        drawerInitialized = false
        initNavigationMenu()
    }

    private fun firebaseLogin() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null) {
            firebaseDatabase = FirebaseDatabase.getInstance().reference
            compositeDisposable.add(RxFirebase.observeChildren(DbUtils.firebaseTasksNode!!)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ ev ->
                        when (ev.eventType) {
                            RxFirebase.EventType.CHILD_ADDED, RxFirebase.EventType.CHILD_CHANGED -> {
                                val task = ev.snapshot.getValue(Task::class.java)
                                task.id = ev.snapshot.key
                                Q.Task.save(task).query()
                            }
                            RxFirebase.EventType.CHILD_REMOVED -> {
                                val task = Task() // no need to copy everything, only id needed
                                task.id = ev.snapshot.key
                                Q.Task.delete().model(task).query()
                            }
                            RxFirebase.EventType.CHILD_MOVED -> {
                            }
                        }
                    }, { throwable -> Dog.e("Error", throwable) }))

            compositeDisposable.add(RxFirebase.observeChildren(DbUtils.firebaseCategoriesNode!!)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ ev ->
                        when (ev.eventType) {
                            RxFirebase.EventType.CHILD_ADDED, RxFirebase.EventType.CHILD_CHANGED -> {
                                val category = ev.snapshot.getValue(Category::class.java)
                                category.id = ev.snapshot.key
                                Q.Category.save(category).query()
                            }
                            RxFirebase.EventType.CHILD_REMOVED -> {
                                val category = Category() // no need to copy everything, only id needed
                                category.id = ev.snapshot.key
                                Q.Category.delete().model(category).query()
                            }
                            RxFirebase.EventType.CHILD_MOVED -> {
                            }
                        }
                    }, { throwable -> Dog.e("Error", throwable) }))

            compositeDisposable.add(RxFirebase.observeSingle(DbUtils.firebaseCurrentUserNode!!.child(DbUtils.FIREBASE_CURRENT_POINTS_NODE_NAME))
                    .subscribeOn(Schedulers.io())
                    .subscribe({ snapshot ->
                        if (snapshot.value != null) {
                            PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, snapshot.getValue(Long::class.java))
                        }
                    }, { throwable -> Dog.e("Error", throwable) }))

            player_name.text = firebaseUser.displayName
            Glide.with(this@MainActivity).load(firebaseUser.photoUrl).asBitmap().fitCenter().fallback(android.R.drawable.sym_def_app_icon).placeholder(android.R.drawable.sym_def_app_icon).into(object : BitmapImageViewTarget(player_image) {
                override fun setResource(resource: Bitmap) {
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, resource)
                    circularBitmapDrawable.isCircular = true
                    getView().setImageDrawable(circularBitmapDrawable)
                }
            })
        }
    }

    private fun firebaseLogout() {
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(contentObserver)
        PrefUtils.unregisterOnPrefChangeListener(currentPointsObserver)
        firebaseLogout()

        super.onDestroy()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            REQUEST_CODE_CATEGORY ->
                // Dialog retarded to give time to activity's views of being
                // completely initialized
                // The dialog style is chosen depending on result code
                when (resultCode) {
                    Activity.RESULT_OK -> UiUtils.showMessage(this, R.string.category_saved)
                    Activity.RESULT_FIRST_USER -> UiUtils.showMessage(this, R.string.category_deleted)
                    else -> {
                    }
                }

            REQUEST_CODE_SIGN_IN -> if (resultCode == Activity.RESULT_OK) {
                firebaseLogin()

                // We successfully logged in, let's add on firebase what we have
                Thread(Runnable {
                    val categoriesFirebase = DbUtils.firebaseCategoriesNode
                    if (categoriesFirebase != null) {
                        for (category in DbUtils.categories) {
                            categoriesFirebase.child(category.id.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        Dog.d("add cat: " + category.id!!)
                                        categoriesFirebase.child(category.id.toString()).setValue(category)
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        }
                    }

                    val tasksFirebase = DbUtils.firebaseTasksNode
                    if (tasksFirebase != null) {
                        for (task in DbUtils.tasks) {
                            tasksFirebase.child(task.id.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        Dog.d("add task: " + task.id!!)
                                        tasksFirebase.child(task.id.toString()).setValue(task)
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        }
                    }
                }).run()
            }
            else -> {
            }
        }
    }

    private fun initNavigationMenu() {
        navigation_view.menu.clear()
        val currentNavigation = NavigationUtils.navigation

        var item = navigation_view.menu.add(1, R.string.all_tasks, Menu.NONE, R.string.all_tasks)
        item.setIcon(R.drawable.ic_assignment_grey600_24dp)
        val activeTaskCount = DbUtils.activeTaskCount
        if (activeTaskCount > 0) {
            item.setActionView(R.layout.menu_counter)
            (item.actionView as TextView).text = activeTaskCount.toString()
        }
        if (NavigationUtils.TASKS == currentNavigation) {
            item.isChecked = true
        }

        if (DbUtils.finishedTaskCount > 0) {
            item = navigation_view.menu.add(1, R.string.finished_tasks, Menu.NONE, R.string.finished_tasks)
            item.setIcon(R.drawable.ic_assignment_turned_in_grey600_24dp)
            if (NavigationUtils.FINISHED_TASKS == currentNavigation) {
                item.isChecked = true
            }
        }

        // Retrieves data to fill tags list
        val mapCategories = mutableMapOf<String, Category>()
        for (category in DbUtils.categories) {
            item = navigation_view.menu.add(1, R.string.category, Menu.NONE, category.name)
            val categoryCount = DbUtils.getActiveTaskCountByCategory(category)
            if (categoryCount > 0) {
                item.setActionView(R.layout.menu_counter)
                (item.actionView as TextView).text = categoryCount.toString()
            }
            val extraIntent = Intent()
            extraIntent.putExtra(Constants.EXTRA_CATEGORY, category.id)
            item.intent = extraIntent
            item.icon = ColorDrawable(category.color)
            if (category.id == currentNavigation) {
                item.isChecked = true
            }

            mapCategories.put(category.id!!, category)
        }

        item = navigation_view.menu.add(1, R.string.add_category, Menu.NONE, "")
        item.setActionView(R.layout.menu_counter)
        val addCatView = item.actionView as TextView
        addCatView.setText(R.string.add_category)
        addCatView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_primarydark_24dp, 0, 0, 0)

        navigation_view.menu.setGroupCheckable(1, true, true)

        item = navigation_view.menu.add(Menu.NONE, R.string.find_games, Menu.NONE, R.string.find_games)
        item.setIcon(R.drawable.ic_mood_grey600_24dp)

        item = navigation_view.menu.add(Menu.NONE, R.string.settings, Menu.NONE, R.string.settings)
        item.setIcon(R.drawable.ic_settings_grey600_24dp)

        navigation_view.post {
            // Initialized the views which was not inflated before
            if (!drawerInitialized && !isFinishing && !isDestroyed) {
                drawerInitialized = true

                // do it here to be sure the view can be find
                PrefUtils.registerOnPrefChangeListener(currentPointsObserver)

                drawer_header.onClick {
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        startActivityForResult(
                                AuthUI.getInstance().createSignInIntentBuilder()
                                        .setTheme(R.style.AppTheme)
                                        .setLogo(R.mipmap.ic_launcher)
                                        .setProviders(listOf(AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(), AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build())) //, AuthUI.FACEBOOK_PROVIDER})
                                        .build(),
                                REQUEST_CODE_SIGN_IN)
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                                .setMessage(R.string.sign_out_confirmation)
                                .setPositiveButton(R.string.confirm) { dialog, id ->
                                    AuthUI.getInstance().signOut(this@MainActivity)

                                    Glide.with(this@MainActivity).load(android.R.drawable.sym_def_app_icon).into(player_image)
                                    player_name.setText(R.string.not_logged_in)
                                    firebaseLogout()
                                }
                                .setNegativeButton(android.R.string.no) { dialog, id -> }.show()
                    }
                }

                firebaseLogin()

                current_points.text = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0).toString()
            }

            // Small hack to handle the long press on menu item
            (0..navigation_menu_view.childCount - 1)
                    .map { navigation_menu_view.getChildAt(it) }
                    .forEach {
                        if (it is MenuView.ItemView) {
                            it.onLongClick {
                                val catId = (it as MenuView.ItemView).itemData.intent?.getStringExtra(Constants.EXTRA_CATEGORY)
                                if (catId != null) {
                                    editCategory(mapCategories[catId]!!)
                                }
                                true
                            }
                        }
                    }
        }
    }

    /**
     * Categories addition and editing
     */
    fun editCategory(category: Category?) {
        val categoryIntent = Intent(this, CategoryActivity::class.java)
        categoryIntent.putExtra(Constants.EXTRA_CATEGORY, Parcels.wrap(category))
        startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY)
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntents()

        super.onNewIntent(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.string.find_games -> {
                val appPackageName = "net.fred.taskgame.hero"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
                }

            }
            R.string.settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.string.add_category -> {
                editCategory(null)
                initNavigationMenu() // To not select it in app drawer
            }
            else -> { // tasks, finished tasks, categories
                // Reset intent
                intent.action = Intent.ACTION_MAIN

                if (item.itemId == R.string.all_tasks) {
                    updateNavigation(NavigationUtils.TASKS)
                } else if (item.itemId == R.string.finished_tasks) {
                    updateNavigation(NavigationUtils.FINISHED_TASKS)
                } else { // This is a category
                    updateNavigation(item.intent.getStringExtra(Constants.EXTRA_CATEGORY))
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigation(navigation: String) {
        NavigationUtils.navigation = navigation

        if (intent != null && intent.hasExtra(Constants.EXTRA_WIDGET_ID)) {
            intent.removeExtra(Constants.EXTRA_WIDGET_ID)
            intent = intent
        }
    }

    // Check if is launched from a widget with categories to set tag
    val widgetCatId: String?
        get() {
            if (intent != null && intent.hasExtra(Constants.EXTRA_WIDGET_ID)) {
                val widgetId = intent.extras.get(Constants.EXTRA_WIDGET_ID)!!.toString()
                val pref = PrefUtils.getString(PrefUtils.PREF_WIDGET_PREFIX + widgetId, "")
                if (pref.isNotEmpty()) {
                    return pref
                }
            }

            return null
        }

    /**
     * Checks if allocated fragment is of the required type and then returns it or returns null
     */
    private fun checkFragmentInstance(id: Int, instanceClass: Any): Fragment? {
        val fragment = supportFragmentManager.findFragmentById(id)
        if (instanceClass == fragment.javaClass) {
            return fragment
        }
        return null
    }

    override fun onBackStackChanged() {
        displayHomeOrUpIcon()
    }

    fun displayHomeOrUpIcon() {
        //Enable Up button only if there are entries in the back stack
        val canUp = supportFragmentManager.backStackEntryCount > 0

        drawer_layout.setDrawerLockMode(if (canUp) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED)
        // Use the drawer_toggle to animate the icon
        val anim = ValueAnimator.ofFloat(if (canUp) 0F else 1F, if (canUp) 1F else 0F)
        anim.addUpdateListener { valueAnimator ->
            val slideOffset = valueAnimator.animatedValue as Float
            drawer_toggle.onDrawerSlide(drawer_layout, slideOffset)
        }
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 300
        anim.start()
    }

    //TODO use that to automatically open drawer or popBackStack
    //    @Override
    //    public boolean onSupportNavigateUp() {
    //        //This method is called when the up button is pressed. Just the pop back stack.
    //        getSupportFragmentManager().popBackStack();
    //        return true;
    //    }

    override fun onBackPressed() {
        // DetailFragment
        val f = checkFragmentInstance(R.id.fragment_container, DetailFragment::class.java)
        if (f != null) {
            (f as DetailFragment).saveAndExit()
            return
        }

        // Before exiting from app the navigation drawer is opened
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Finishes multiselection mode started by ListFragment
     */
    fun finishActionMode() {
        val fragment = supportFragmentManager.findFragmentByTag(ListFragment::class.java.name) as ListFragment
        fragment.finishActionMode()
    }

    private fun handleIntents() {
        if (intent.action == null) return

        if (receivedIntent(intent)) {
            var task: Task? = Parcels.unwrap<Task>(intent.getParcelableExtra<Parcelable>(Constants.EXTRA_TASK))
            if (task == null) {
                task = DbUtils.getTask(intent.getStringExtra(Constants.EXTRA_TASK_ID))
            }
            // Checks if the same note is already opened to avoid to open again
            if (task != null && isTaskAlreadyOpened(task)) {
                return
            }
            // Empty note instantiation
            if (task == null) {
                task = Task()
            }
            switchToDetail(task)
        } else if (Intent.ACTION_VIEW == intent.action) { // Tag search
            switchToList()
        }
    }

    private fun receivedIntent(i: Intent): Boolean {
        return Constants.ACTION_NOTIFICATION_CLICK == i.action
                || Constants.ACTION_WIDGET == i.action
                || (Intent.ACTION_SEND == i.action
                || Intent.ACTION_SEND_MULTIPLE == i.action
                || Constants.ACTION_GOOGLE_NOW == i.action) && i.type != null
                || i.action != null && i.action.contains(Constants.ACTION_NOTIFICATION_CLICK)
    }


    private fun isTaskAlreadyOpened(task: Task): Boolean {
        val detailFragment = supportFragmentManager.findFragmentByTag(DetailFragment::class.java.name) as DetailFragment?
        return detailFragment != null && detailFragment.currentTask?.id == task.id
    }

    fun switchToList() {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
        transaction.replace(R.id.fragment_container, ListFragment(), ListFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
    }

    fun switchToDetail(task: Task) {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
        transaction.replace(R.id.fragment_container, DetailFragment.newInstance(task), DetailFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
    }

    companion object {

        private val REQUEST_CODE_CATEGORY = 2
        private val REQUEST_CODE_SIGN_IN = 3
    }
}
