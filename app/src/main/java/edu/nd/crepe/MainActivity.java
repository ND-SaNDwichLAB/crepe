package edu.nd.crepe;

import android.app.Dialog;
import android.content.DialogInterface;
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
import android.net.Uri;
import android.os.Bundle;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.User;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.network.DataLoadingEvent;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;
import edu.nd.crepe.ui.dialog.CreateCollectorFromURLDialogBuilder;
import edu.nd.crepe.ui.main_activity.FabModalBottomSheet;
import edu.nd.crepe.ui.main_activity.HomeFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FloatingActionButton fabBtn;
    private LinearLayout addExistingBtn;
    private LinearLayout createNewBtn;

    private ActionBarDrawerToggle sidemenuToggle;
    private DrawerLayout drawerLayout;
    private NavigationView sidebarNavView;
    private View navHeader;



    private DatabaseManager dbManager;

    private CreateCollectorFromURLDialogBuilder createCollectorFromURLDialogBuilder;
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;

    private FirebaseAuth mAuth;

    private Fragment currentFragment;

    private TextView userNameTextView;

    // the unique id extracted from the user's device, used as their user id
    public static User currentUser = null;
    public static Drawable userImage = null;

    private CollectorConfigurationDialogWrapper wrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);

        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        FirebaseCommunicationManager firebaseCommunicationManager = new FirebaseCommunicationManager(this);

        mAuth = FirebaseAuth.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // display the home fragment
        // the id here is the id for the nav menu, due to the design of the function.
        // see details of the function in later sections of this file
        displaySelectedScreen(R.id.nav_menu_home);

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
            // get the current stored user from the database, saved in the log in process with google authentication
            if (dbManager.getAllUsers().size() == 1) {
                currentUser = dbManager.getAllUsers().get(0);
                userNameTextView.setText(currentUser.getName());
                Toast.makeText(this, "Welcome, " + currentUser.getName() + "! 🥳🎉🎊", Toast.LENGTH_SHORT).show();
            } else if (dbManager.getAllUsers().size() > 1) {
                Log.e("Main Activity", "Error: more than 1 user found in database.");
            } else {
                Log.i("Main Activity", "no user found in database.");
            }
        }

        // if it is existing user and the profile is not pulled from firebase yet, register for the event and update when it is ready
        EventBus.getDefault().register(this);

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




        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        sidebarNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                displaySelectedScreen(item.getItemId());
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });


        this.createCollectorFromURLDialogBuilder = new CreateCollectorFromURLDialogBuilder(this, refreshCollectorListRunnable);
        this.createCollectorFromConfigDialogBuilder = new CreateCollectorFromConfigDialogBuilder(this, refreshCollectorListRunnable);

        // get the fab icon
        fabBtn = findViewById(R.id.fab);

        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FabModalBottomSheet modalBottomSheet = new FabModalBottomSheet(createCollectorFromURLDialogBuilder, createCollectorFromConfigDialogBuilder);
                modalBottomSheet.show(getSupportFragmentManager(), FabModalBottomSheet.TAG);

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!CollectorConfigurationDialogWrapper.isNull()) {
            wrapper = CollectorConfigurationDialogWrapper.getInstance();
            editor.putString("screen_state", wrapper.getCurrentScreenState());
            editor.putString("collector", new Gson().toJson(wrapper.getCurrentCollector()));

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
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE);

        String screenState = sharedPreferences.getString("screen_state", null);
        String collectorJson = sharedPreferences.getString("collector", null);

        if (screenState != null && !screenState.equals("dismissed") && collectorJson != null) {
            Collector prevCollector = new Gson().fromJson(collectorJson, Collector.class);
            wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithExistingCollector(prevCollector);
            wrapper.setCurrentScreenState(screenState);
            wrapper.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseManager.getInstance(this.getApplicationContext()).closeDatabase();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadingEvent(DataLoadingEvent event){
        if(event.isCompleted()){
            if (dbManager.getAllUsers().size() == 1) {
                currentUser = dbManager.getAllUsers().get(0);
                userNameTextView.setText(currentUser.getName());
                Toast.makeText(this, "Welcome, " + currentUser.getName() + "! 🥳🎉🎊", Toast.LENGTH_SHORT).show();
            }
        }
    }
}