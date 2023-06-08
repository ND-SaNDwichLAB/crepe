package edu.nd.crepe;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import edu.nd.crepe.R;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.User;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;
import edu.nd.crepe.ui.dialog.CreateCollectorFromURLDialogBuilder;
import edu.nd.crepe.ui.main_activity.HomeFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

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

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FloatingActionButton fabBtn;
    private FloatingActionButton addUrlBtn;
    private FloatingActionButton createNewBtn;

    private ActionBarDrawerToggle sidemenuToggle;
    private DrawerLayout drawerLayout;
    private NavigationView sidebarNavView;
    private View navHeader;


    private Animation top_appear_anim;
    private Animation top_disappear_anim;
    private Animation left_appear_anim;
    private Animation left_disappear_anim;

    // clicked toggle variable for fab icons
    private Boolean clicked = false;

    private Collector testCollector;
    private Collector testCollector2;
    private DatabaseManager dbManager;

    private CreateCollectorFromURLDialogBuilder createCollectorFromURLDialogBuilder;
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;

    private FirebaseAuth mAuth;

    private Fragment currentFragment;

    // the unique id extracted from the user's device, used as their user id
    public static User currentUser = null;
    public static Drawable userImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);

        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        FirebaseCommunicationManager firebaseCommunicationManager = new FirebaseCommunicationManager(this);

        mAuth = FirebaseAuth.getInstance();


        // load animations
        top_appear_anim = AnimationUtils.loadAnimation( this, R.anim.top_appear);
        top_disappear_anim = AnimationUtils.loadAnimation( this, R.anim.top_disappear);
        left_appear_anim = AnimationUtils.loadAnimation( this, R.anim.left_appear);
        left_disappear_anim = AnimationUtils.loadAnimation( this, R.anim.left_disappear);

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

        TextView userNameTextView = navHeader.findViewById(R.id.userName);
//        ImageView userImageView = navHeader.findViewById(R.id.userImage);

        if (currentUser == null) {
            // get the current stored user from the database, saved in the log in process with google authentication
            if (dbManager.getAllUsers().size() == 1) {
                currentUser = dbManager.getAllUsers().get(0);

                userNameTextView.setText(currentUser.getName());
                Toast.makeText(this, "Welcome, " + currentUser.getName() + "! 🥳🎉🎊", Toast.LENGTH_LONG).show();
            }
        }

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



        // refresh name
        Runnable mainActivityRefreshUsernameRunnable = new Runnable() {
            @Override
            public void run() {
                userNameTextView.setText(currentUser.getName());
//                if (userImage != null) {
//                    userImageView.setImageDrawable(userImage);
//                }
            }
        };


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
        this.createCollectorFromURLDialogBuilder = new CreateCollectorFromURLDialogBuilder(this, refreshCollectorListRunnable);
        this.createCollectorFromConfigDialogBuilder = new CreateCollectorFromConfigDialogBuilder(this, refreshCollectorListRunnable);

        // get the fab icons
        fabBtn = findViewById(R.id.fab);
        addUrlBtn = findViewById(R.id.fab_add_from_url);
        createNewBtn = findViewById(R.id.fab_create_new);


        addUrlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // collapse the fab icon
                clicked = !clicked;
                setAnimation(clicked);

                Dialog dialog = createCollectorFromURLDialogBuilder.build();
                dialog.show();
                displaySelectedScreen(R.id.nav_menu_home);
            }
        });

        createNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // collapse the fab icon
                clicked = !clicked;
                setAnimation(clicked);

                CollectorConfigurationDialogWrapper wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithNewCollector();
                wrapper.show();
            }
        });
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

        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clicked = !clicked;
                Log.i(null, "clicked value: " + clicked);
                setAnimation(clicked);
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        sidemenuToggle.syncState();
        super.onPostCreate(savedInstanceState);

    }

    private void setAnimation(Boolean clicked) {
        if(clicked) {
            addUrlBtn.startAnimation(left_appear_anim);
            createNewBtn.startAnimation(top_appear_anim);
        } else {
            addUrlBtn.startAnimation(left_disappear_anim);
            createNewBtn.startAnimation(top_disappear_anim);
        }

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
        DatabaseManager.getInstance(this.getApplicationContext()).close();
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
}