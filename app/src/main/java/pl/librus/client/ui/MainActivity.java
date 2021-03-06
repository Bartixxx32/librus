package pl.librus.client.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.util.List;
import java.util.Locale;

import pl.librus.client.R;
import pl.librus.client.announcements.AnnouncementsFragment;
import pl.librus.client.api.Event;
import pl.librus.client.api.Lesson;
import pl.librus.client.api.LibrusAccount;
import pl.librus.client.api.LibrusCache;
import pl.librus.client.api.LuckyNumber;
import pl.librus.client.api.Timetable;
import pl.librus.client.timetable.TimetableFragment;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "librus-client-log";
    LuckyNumber luckyNumber;
    ActionMenuView amv;
    AppBarLayout appBarLayout;
    TabLayout tabLayout = null;
    LibrusCache cache;
    private TimetableFragment timetableFragment;
    private AnnouncementsFragment announcementsFragment;
    private Drawer drawer;
    private Toolbar toolbar;
    private Timetable timetable;
    private List<Event> events;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean logged_in = prefs.getBoolean("logged_in", false);
        if (!logged_in) {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        } else {
            LibrusCache.load(getApplicationContext()).done(new DoneCallback<LibrusCache>() {
                @Override
                public void onDone(LibrusCache result) {
                    cache = result;
                    display();
                }
            }).fail(new FailCallback<Object>() {
                @Override
                public void onFail(Object result) {
                    try {
                        cache = new LibrusCache(getApplicationContext());
                        cache.update().waitSafely(5000);
                        display();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void display() {
        LibrusAccount account = cache.getAccount();
        luckyNumber = cache.getLuckyNumber();
        timetable = cache.getTimetable();
        events = cache.getEvents();
        for (Event event : events) {

            Lesson lesson = timetable.getLesson(event.getDate(), event.getLessonNumber());
            if (lesson != null) {
                lesson.setEvent(event);
            }
        }
        ProfileDrawerItem profile = new ProfileDrawerItem().withName(account.getName()).withEmail(account.getEmail()).withIcon(R.mipmap.jeb);

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withSelectionListEnabledForSingleProfile(true)
                .withHeaderBackground(R.drawable.background_nav)
                .withHeaderBackgroundScaleType(ImageView.ScaleType.CENTER_CROP)
                .addProfiles(profile)
                .build();
        PrimaryDrawerItem lucky = new PrimaryDrawerItem().withIconTintingEnabled(true).withSelectable(false)
                .withIdentifier(666)
                .withName("Szczęśliwy numerek: " + luckyNumber.getLuckyNumber())
                .withIcon(R.drawable.ic_sentiment_very_satisfied_black_24dp);
        final DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(0)
                                .withName("Plan lekcji")
                                .withIcon(R.drawable.ic_event_note_black_48dp),
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(1)
                                .withName("Oceny - Nie dziala")
                                .withIcon(R.drawable.ic_event_black_48dp),
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(2)
                                .withName("Terminarz - Nie dziala")
                                .withIcon(R.drawable.ic_date_range_black_48dp),
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(3)
                                .withName("Ogłoszenia")
                                .withIcon(R.drawable.ic_announcement_black_48dp),
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(4)
                                .withName("Wiadomości - Nie dziala")
                                .withIcon(R.drawable.ic_message_black_48dp),
                        new PrimaryDrawerItem().withIconTintingEnabled(true)
                                .withIdentifier(5)
                                .withName("Nieobecności - Nie dziala")
                                .withIcon(R.drawable.ic_person_outline_black_48dp),
                        new DividerDrawerItem(),
                        lucky)
                .addStickyDrawerItems(new PrimaryDrawerItem().withIconTintingEnabled(true).withSelectable(false)
                        .withIdentifier(6)
                        .withName("Ustawienia")
                        .withIcon(R.drawable.ic_settings_black_48dp))
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        return selectItem(drawerItem);
                    }
                })
                .withDelayOnDrawerClose(50)
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View clickedView) {
                        onBackPressed();
                        return true;
                    }
                })
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true);

        setContentView(R.layout.activity_main);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = drawerBuilder.withToolbar(toolbar).build();
        timetableFragment = TimetableFragment.newInstance(cache.getTimetable());
        announcementsFragment = AnnouncementsFragment.newInstance(cache.getAnnouncements());
        drawer.setSelection(0);
        if (appBarLayout.findViewById(tabLayout.getId()) == null) {
            appBarLayout.addView(tabLayout);
        }
    }

    void changeFragment(Fragment fragment, String title) {
        Log.d(TAG, "changeFragment: \n" +
                "fragment " + fragment + "\n" +
                "title: " + title);

        toolbar.setTitle(title);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_main, fragment);
        transaction.commit();
    }

    private boolean selectItem(IDrawerItem item) {

        switch ((int) item.getIdentifier()) {
            case 0:
                changeFragment(timetableFragment, "Plan lekcji");
                break;
            case 1:
                changeFragment(new PlaceholderFragment(), "Oceny - Nie dziala");
                break;
            case 2:
                changeFragment(new PlaceholderFragment(), "Terminarz - Nie dziala");
                break;
            case 3:
                changeFragment(announcementsFragment, "Ogłoszenia");
                break;
            case 4:
                changeFragment(new PlaceholderFragment(), "Wiadomości - Nie dziala");
                break;
            case 5:
                changeFragment(new PlaceholderFragment(), "Nieobecności - Nie dziala");
                break;
            case 6:
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
                break;
            case 666:
                String date = luckyNumber.getLuckyNumberDay().toString("EEEE, d MMMM yyyy", new Locale("pl"));
                date = date.substring(0, 1).toUpperCase() + date.substring(1).toLowerCase();
                Toast.makeText(this, date, Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public AppBarLayout getAppBarLayout() {
        return appBarLayout;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        for (int i = 0; i < toolbar.getChildCount(); ++i) {
            if (toolbar.getChildAt(i).getClass().getSimpleName().equals("ActionMenuView")) {
                amv = (ActionMenuView) toolbar.getChildAt(i);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_sync:
                RotateAnimation r = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                r.setDuration(600);
                RotateAnimation rotateAnimation = new RotateAnimation(30, 90, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(10000);
                amv.getChildAt(amv.getChildCount() - 1).startAnimation(r);
                cache.update().done(new DoneCallback<Object>() {
                    @Override
                    public void onDone(Object result) {
                        display();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addTabs(TabLayout tabLayout) {
        if (this.appBarLayout != null) {
            appBarLayout.addView(tabLayout);
        }
        this.tabLayout = tabLayout;
    }

    public void removeTabs(TabLayout tabLayout) {
        appBarLayout.removeView(tabLayout);
        this.tabLayout = null;
    }

    public void setBackArrow(boolean enable) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ActionBarDrawerToggle toggle = getDrawer().getActionBarDrawerToggle();
            if (enable) {
                toggle.setDrawerIndicatorEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                actionBar.setDisplayHomeAsUpEnabled(false);
                toggle.setDrawerIndicatorEnabled(true);
            }
            toggle.syncState();
        }
    }

}
