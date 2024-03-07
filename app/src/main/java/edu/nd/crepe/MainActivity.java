package edu.nd.crepe;

import android.Manifest;
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
import edu.nd.crepe.network.ApiCallManager;
import edu.nd.crepe.network.DataLoadingEvent;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import edu.nd.crepe.servicemanager.CrepeNotificationManager;
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

import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import edu.nd.crepe.databinding.ActivityMainBinding;

import com.google.android.material.navigation.NavigationView;
import com.google.common.reflect.TypeToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

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

import javax.json.JsonObject;

public class MainActivity extends AppCompatActivity {

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

    public static final String CHANNEL_ID = "CREPE_NOTIFICATION_CHANNEL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity", "onCreate called");
        super.onCreate(savedInstanceState);

        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        firebaseCommunicationManager = new FirebaseCommunicationManager(this);
        mAuth = FirebaseAuth.getInstance();
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

        if (currentUser == null) {
            // get the current stored user from the database, which we fetched with google authentication (see authentication/GoogleSignInActivity.java)
            if (dbManager.getAllUsers().size() == 1) {
                currentUser = dbManager.getAllUsers().get(0);
                userNameTextView.setText(currentUser.getName());
                Toast.makeText(this, "Welcome, " + currentUser.getName().split(" ")[0] + "! 🥳🎉🎊", Toast.LENGTH_SHORT).show();   // only take the first name
            } else if (dbManager.getAllUsers().size() > 1) {
                Log.e("Main Activity", "Error: more than 1 user found in database.");
            } else {
                Log.i("Main Activity", "no user found in database.");
            }
        }

        // get local collectors
        ArrayList<Collector> existingCollectors = (ArrayList<Collector>) dbManager.getAllCollectors();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "crepeChannel";
            String description = "Crepe app notification channel, delivering information regarding collector status changes.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "onDestroy called");
        // unregister the event bus
        EventBus.getDefault().unregister(this);
        // close the database
        DatabaseManager.getInstance(this.getApplicationContext()).closeDatabase();
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
            editor.putBoolean("isEdit", wrapper.getIsEdit());
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
            Collector prevCollector = new Gson().fromJson(collectorJson, Collector.class);
            Type type = new TypeToken<ArrayList<Datafield>>() {}.getType();
            ArrayList<Datafield> datafieldsList = new Gson().fromJson(datafields, type);

            if (!CollectorConfigurationDialogWrapper.isNull()) {
                wrapper = CollectorConfigurationDialogWrapper.getInstance();
            } else {
                wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithCollector(prevCollector);
            }

            wrapper.setDatafields(datafieldsList);
            wrapper.setCurrentCollector(prevCollector);
            wrapper.setCurrentScreenState(screenState);
            wrapper.show(isEdit);

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

    private void addParticipatingCollectors(ArrayList<String> collectorIds, ArrayList<String> existingCollectorIds) {
        AtomicInteger collectorCounter = new AtomicInteger(0);
        int totalCollectorCount = collectorIds.size();

        // retrieve all collectors associated with this user from firebase and save to local
        for (String collectorId : collectorIds) {
            if (!existingCollectorIds.contains(collectorId)) {
                firebaseCommunicationManager.retrieveCollector(collectorId, new FirebaseCallback<Collector>() {
                    public void onResponse(Collector collector) {
                        dbManager.addOneCollector(collector);
                        addDatafieldForCollector(collector);

                        // broadcast an event to HomeFragment to update the collector list
                        if (collectorCounter.incrementAndGet() == totalCollectorCount) {
                            // After fetching all data, post this event and HomeFragment will update the collector list on home page
                            EventBus.getDefault().post(new DataLoadingEvent(true));
                        }
                    }

                    public void onErrorResponse(Exception e) {
                        try {
                            Log.e("Firebase collector", e.getMessage());
                        } catch (NullPointerException ex) {
                            Log.e("Firebase collector", "An unknown error occurred.");
                        }
                    }
                });
            } else {
                Log.i("MainActivity", "Collector already exists in local database. Skipped");
                // broadcast an event to HomeFragment to update the collector list
                if (collectorCounter.incrementAndGet() == totalCollectorCount) {
                    // After fetching all data, post this event and HomeFragment will update the collector list on home page
                    EventBus.getDefault().post(new DataLoadingEvent(true));
                }
            }
        }
    }

    private void addCreatedCollectors(String userId, ArrayList<String> existingCollectorIds) {
        firebaseCommunicationManager.retrieveCollectorWithCreatorUserId(userId, new FirebaseCallback<ArrayList<Collector>>() {
            public void onResponse(ArrayList<Collector> collectors) {

                for (Collector collector : collectors) {
                    if (!existingCollectorIds.contains(collector.getCollectorId())) {
                        dbManager.addOneCollector(collector);
                        addDatafieldForCollector(collector);
                    }
                }
                // After fetching all data, post this event and HomeFragment will update the collector list on home page
                EventBus.getDefault().post(new DataLoadingEvent(true));

            }

            public void onErrorResponse(Exception e) {
                try {
                    Log.i("Firebase collector", "No collector is found that is created by current user.\n" + e.getMessage());
                } catch (NullPointerException ex) {
                    Log.e("Firebase collector", "No collector is found that is created by current user. An unknown error occurred.");
                }
            }
        });
    }


    // from firebase, retrieve all datafields associated with this collector and save to local
    private void addDatafieldForCollector(Collector collector) {
        firebaseCommunicationManager.retrieveDatafieldsWithCollectorId(collector.getCollectorId(), new FirebaseCallback<ArrayList<Datafield>>() {
            public void onResponse(ArrayList<Datafield> datafields) {
                for (Datafield datafield : datafields) {
                    dbManager.addOneDatafield(datafield);
                }
            }

            public void onErrorResponse(Exception e) {
                try {
                    Log.e("Firebase datafield", e.getMessage());
                } catch (NullPointerException ex) {
                    Log.e("Firebase datafield", "An unknown error occurred.");
                }
            }
        });
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

    public void retrieveCollectorsForUser(User currentUser, ArrayList<String> existingCollectorIds) {

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
                    e.printStackTrace();
                }
            }
        }
    };

}