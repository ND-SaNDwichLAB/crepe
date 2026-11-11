package edu.nd.crepe;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;
import edu.nd.crepe.authentication.GoogleLoginActivity;
import edu.nd.crepe.network.DataLoadingEvent;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;
import edu.nd.crepe.ui.dialog.AddCollectorFromCollectorIdDialogBuilder;
import edu.nd.crepe.ui.main_activity.FabModalBottomSheet;
import edu.nd.crepe.ui.main_activity.HomeFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import edu.nd.crepe.databinding.ActivityMainBinding;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final Type DATAFIELD_LIST_TYPE = new TypeToken<ArrayList<Datafield>>() {}.getType();

    private ActivityMainBinding binding;
    private FloatingActionButton fabBtn;
    private ActionBarDrawerToggle sidemenuToggle;
    private DrawerLayout drawerLayout;
    private NavigationView sidebarNavView;
    private View navHeader;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager firebaseCommunicationManager;
    private AddCollectorFromCollectorIdDialogBuilder addCollectorFromCollectorIdDialogBuilder;
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;

    private FirebaseAuth mAuth;

    private Fragment currentFragment;

    private TextView userNameTextView;

    // the unique id extracted from the user's device, used as their user id
    public static User currentUser;
    public static Drawable userImage;
    private AlertDialog.Builder dialogBuilder;

    private CollectorConfigurationDialogWrapper wrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity", "onCreate called");
        super.onCreate(savedInstanceState);

        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        firebaseCommunicationManager = new FirebaseCommunicationManager(this);
        mAuth = FirebaseAuth.getInstance();

        // Everything on this screen depends on knowing who the user is. If there is no usable profile
        // (for example the local database was cleared while the Firebase session survived), send the
        // user back through the sign in flow instead of crashing further down.
        boolean isFirstResolve = currentUser == null;
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (currentUser == null || (firebaseUser != null && !firebaseUser.getUid().equals(currentUser.getUserId()))) {
            isFirstResolve = true;
            currentUser = resolveCurrentUser(firebaseUser);
        }
        if (currentUser == null) {
            Log.e(TAG, "No local user profile could be found, returning to sign in");
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            startActivity(new Intent(this, GoogleLoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // sidebar toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        sidemenuToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        sidemenuToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(sidemenuToggle);

        // set sidebar use name based on user profile
        sidebarNavView = findViewById(R.id.sidebarNavView);
        navHeader = sidebarNavView.getHeaderView(0);

        userNameTextView = navHeader.findViewById(R.id.userName);
//        ImageView userImageView = navHeader.findViewById(R.id.userImage);

        // the user was fetched with google authentication (see authentication/GoogleLoginActivity.java)
        String displayName = currentUser.getName() != null ? currentUser.getName().trim() : "";
        userNameTextView.setText(displayName);
        if (isFirstResolve && !displayName.isEmpty()) {
            Toast.makeText(this, "Welcome, " + displayName.split(" ")[0] + "! 🥳🎉🎊", Toast.LENGTH_SHORT).show();   // only take the first name
        }

        // get local collectors
        List<Collector> existingCollectors = dbManager.getAllCollectors();
        // we also get a list of collectorIds, easier for checking collector existence
        ArrayList<String> existingCollectorIds = new ArrayList<>();
        for (Collector collector : existingCollectors) {
            existingCollectorIds.add(collector.getCollectorId());
        }

        // retrieve collectors for this user that are not in local yet
        retrieveCollectorsForUser(currentUser, existingCollectorIds);


        // display the home fragment
        // the id here is the id for the nav menu, due to the design of the function.
        displaySelectedScreen(R.id.nav_menu_home);

        // code block for user image, maybe of use in the future
//        if (userImage == null) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        final Drawable d = loadImageFromUrl(currentUser.getPhotoUrl());
//                        Log.i("Load Image", "Loaded user image successfully");
//
//                        // Update UI in main thread
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                userImageView.setImageDrawable(d);
//                            }
//                        });
//                    } catch (Exception e) {
//                        Log.e("Load Image", "Error loading user image", e);
//                    }
//                }
//            }).start();
//
//        }

        // set up the notification channel
        CharSequence name = "crepeChannel";
        String description = "Crepe app notification channel, delivering information regarding collector status changes.";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("CREPE_NOTIFICATION_CHANNEL", name, importance);
        channel.setDescription(description);
        // Register the channel with the system
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        sidebarNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                displaySelectedScreen(item.getItemId());
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });


        this.addCollectorFromCollectorIdDialogBuilder = new AddCollectorFromCollectorIdDialogBuilder(this, refreshCollectorListRunnable);
        this.createCollectorFromConfigDialogBuilder = new CreateCollectorFromConfigDialogBuilder(this, refreshCollectorListRunnable);

        // get the fab icon
        fabBtn = findViewById(R.id.fab);

        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FabModalBottomSheet modalBottomSheet = new FabModalBottomSheet(addCollectorFromCollectorIdDialogBuilder, createCollectorFromConfigDialogBuilder);
                modalBottomSheet.show(getSupportFragmentManager(), FabModalBottomSheet.TAG);


            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("MainActivity", "onStop called");
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "onDestroy called");
        // unregister the event bus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        // NOTE: the database is deliberately left open. DatabaseManager is shared with the accessibility
        // service, which keeps collecting data after this activity is gone.
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity", "onPause called");

        // we used this to store the state of the activity before moving to demonstrate in another app
        // however, we probably do not need this if do not call finish() inside the activity to kill it
        SharedPreferences sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!CollectorConfigurationDialogWrapper.isNull()) {
            wrapper = CollectorConfigurationDialogWrapper.getInstance();
            editor.putString("screen_state", wrapper.getCurrentScreenState());
            editor.putString("collector", new Gson().toJson(wrapper.getCurrentCollector()));
            editor.putBoolean("isEdit", Boolean.TRUE.equals(wrapper.getIsEdit()));
            editor.putString("datafieldsList", new Gson().toJson(wrapper.getDatafields()));
            editor.apply();

            // hide the dialog so that we don't have duplicates back in the activity
            wrapper.hide();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity", "onResume called");
        SharedPreferences sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE);

        String screenState = sharedPreferences.getString("screen_state", null);
        String collectorJson = sharedPreferences.getString("collector", null);
        Boolean isEdit = sharedPreferences.getBoolean("isEdit", false);
        String datafields = sharedPreferences.getString("datafieldsList", null);

        if (screenState != null && !screenState.equals("dismissed") && collectorJson != null && datafields != null) {
            try {
                Gson gson = new Gson();
                Collector prevCollector = gson.fromJson(collectorJson, Collector.class);
                ArrayList<Datafield> datafieldsList = gson.fromJson(datafields, DATAFIELD_LIST_TYPE);
                if (prevCollector == null) {
                    throw new JsonParseException("The stored collector is empty");
                }

                // Always rebuild the wrapper so that it is bound to this activity instance. A wrapper that
                // survived from before this activity was recreated would still point at the destroyed one,
                // and showing a dialog through it would fail.
                wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithCollector(prevCollector);

                wrapper.setDatafields(datafieldsList != null ? datafieldsList : new ArrayList<Datafield>());
                wrapper.setCurrentCollector(prevCollector);
                wrapper.setCurrentScreenState(screenState);
                wrapper.show(isEdit);
            } catch (RuntimeException e) {
                // a stale or corrupted snapshot must never keep the app from opening
                Log.e(TAG, "Could not restore the collector configuration dialog", e);
            }

            // if notification permission dialog shows up during this process, these sharedPreferences will be deleted, leaving no way to recover the state
            // TODO Yuwen figure out how to handle the notification permission dialog
            // clear the shared preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

        }
    }

    // a function to switch between fragments using the navDrawer
    private void displaySelectedScreen(int itemId) {

        // initialize a fragment for switching

        switch (itemId) {
            case R.id.nav_menu_home:
                // Change this back
                currentFragment = new HomeFragment();
                break;
//            case R.id.nav_menu_data:
//                currentFragment = new DataFragment();
//                break;
            default:
                Log.i("Menu Selection", "Menu Item Selection Error: no selection detected");
                break;
        }

        //replacing the fragment
        if (currentFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, currentFragment);
            ft.commit();
        }


    }



    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        sidemenuToggle.syncState();
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(sidemenuToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static String describe(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "An unknown error occurred.";
    }

    private void addParticipatingCollectors(ArrayList<String> collectorIds, List<String> existingCollectorIds) {
        if (collectorIds == null || collectorIds.isEmpty()) {
            return;
        }
        AtomicInteger collectorCounter = new AtomicInteger(0);
        int totalCollectorCount = collectorIds.size();

        // retrieve all collectors associated with this user from firebase and save to local
        for (String collectorId : collectorIds) {
            if (!existingCollectorIds.contains(collectorId)) {
                firebaseCommunicationManager.retrieveCollector(collectorId, new FirebaseCallback<Collector>() {
                    public void onResponse(Collector collector) {
                        if (collector != null) {
                            dbManager.addOneCollector(collector);
                            addDatafieldForCollector(collector);
                        }
                        notifyWhenAllCollectorsHandled(collectorCounter, totalCollectorCount);
                    }

                    public void onErrorResponse(Exception e) {
                        Log.e("Firebase collector", describe(e));
                        // a failed collector still counts, otherwise the home page would never be refreshed for the others
                        notifyWhenAllCollectorsHandled(collectorCounter, totalCollectorCount);
                    }
                });
            } else {
                Log.i("MainActivity", "Collector already exists in local database. Skipped");
                notifyWhenAllCollectorsHandled(collectorCounter, totalCollectorCount);
            }
        }
    }

    // After handling all collectors, post an event and HomeFragment will update the collector list on home page
    private void notifyWhenAllCollectorsHandled(AtomicInteger collectorCounter, int totalCollectorCount) {
        if (collectorCounter.incrementAndGet() == totalCollectorCount) {
            EventBus.getDefault().post(new DataLoadingEvent(true));
        }
    }

    private void addCreatedCollectors(String userId, List<String> existingCollectorIds) {
        firebaseCommunicationManager.retrieveCollectorWithCreatorUserId(userId, new FirebaseCallback<ArrayList<Collector>>() {
            public void onResponse(ArrayList<Collector> collectors) {

                if (collectors != null) {
                    for (Collector collector : collectors) {
                        if (!existingCollectorIds.contains(collector.getCollectorId())) {
                            dbManager.addOneCollector(collector);
                            addDatafieldForCollector(collector);
                        }
                    }
                }
                // After fetching all data, post this event and HomeFragment will update the collector list on home page
                EventBus.getDefault().post(new DataLoadingEvent(true));

            }

            public void onErrorResponse(Exception e) {
                Log.i("Firebase collector", "No collector is found that is created by current user.\n" + describe(e));
            }
        });
    }


    // from firebase, retrieve all datafields associated with this collector and save to local
    private void addDatafieldForCollector(Collector collector) {
        firebaseCommunicationManager.retrieveDatafieldsWithCollectorId(collector.getCollectorId(), new FirebaseCallback<ArrayList<Datafield>>() {
            public void onResponse(ArrayList<Datafield> datafields) {
                if (datafields == null) {
                    return;
                }
                for (Datafield datafield : datafields) {
                    dbManager.addOneDatafield(datafield);
                }
            }

            public void onErrorResponse(Exception e) {
                Log.e("Firebase datafield", describe(e));
            }
        });
    }

    // Find the local profile that belongs to the signed in account.
    // Returns null if there is none; the caller then sends the user back to sign in.
    @Nullable
    private User resolveCurrentUser(@Nullable FirebaseUser firebaseUser) {
        List<User> users = dbManager.getAllUsers();
        if (firebaseUser != null) {
            // several accounts may have signed in on this device, pick the one that is signed in now
            for (User user : users) {
                if (firebaseUser.getUid().equals(user.getUserId())) {
                    return user;
                }
            }
            Log.e(TAG, "None of the " + users.size() + " local user(s) matches the signed in account.");
            return null;
        }
        if (users.size() == 1) {
            return users.get(0);
        }
        if (users.size() > 1) {
            Log.e(TAG, "Error: more than 1 user found in database and nobody is signed in.");
        } else {
            Log.i(TAG, "no user found in database.");
        }
        return null;
    }


    public Drawable loadImageFromUrl(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable drawable = Drawable.createFromStream(is, "src name");

            // resize to 108dp
            // convert dp to pixels
            float density = getApplicationContext().getResources().getDisplayMetrics().density;
            int sizeInPixels = (int) (54 * density);
            // get bitmap from drawable
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            // resize bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, sizeInPixels, sizeInPixels, true);
            // crop to circle
            Bitmap output = Bitmap.createBitmap(resizedBitmap.getWidth(), resizedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);

            canvas.drawCircle(resizedBitmap.getWidth() / 2, resizedBitmap.getHeight() / 2, resizedBitmap.getWidth() / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(resizedBitmap, rect, rect, paint);

            // convert back to drawable
            Drawable outputDrawable = new BitmapDrawable(getApplicationContext().getResources(), output);


            Log.i("MainActivity", "Loaded user image successfully");
            return outputDrawable;
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading user image", e);
            return null;
        }
    }

    public void retrieveCollectorsForUser(User currentUser, List<String> existingCollectorIds) {
        if (currentUser == null) {
            Log.e(TAG, "Cannot retrieve collectors without a user");
            return;
        }

        // get the collectors associated with this user
        // contains 2 types of associations:
        // 1. collectors that this user is participating in
        // simply by using the field under User "userCollectors" (see /database/User.java)
        ArrayList<String> collectorIds = currentUser.getCollectorsForCurrentUser();
        addParticipatingCollectors(collectorIds, existingCollectorIds);

        // 2. collectors that this user has created
        // we need to index all collectors on the "creatorUserId" field, find the ones that contain current user's userId (see /database/Collector.java)
        addCreatedCollectors(currentUser.getUserId(), existingCollectorIds);

    }

    Runnable refreshCollectorListRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentFragment instanceof HomeFragment) {
                try {
                    ((HomeFragment) currentFragment).initCollectorList();
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Could not refresh the collector list", e);
                }
            }
        }
    };

}