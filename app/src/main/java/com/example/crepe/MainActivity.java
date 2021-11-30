package com.example.crepe;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Layout;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.crepe.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private ActivityMainBinding binding;
    private FloatingActionButton fabBtn;
    private FloatingActionButton addUrlBtn;
    private FloatingActionButton createNewBtn;

    private ActionBarDrawerToggle sidemenuToggle;
    private DrawerLayout drawerLayout;
    private NavigationView sidebarNavView;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button popupCancelBtn;
    private Button popupNextBtn;

    private Animation top_appear_anim;
    private Animation top_disappear_anim;
    private Animation left_appear_anim;
    private Animation left_disappear_anim;
    //add config
    private TextView dateText;
    private ImageButton strtImgBtn = findViewById(R.id.startImageButton);
    private ImageButton endImgBtn = findViewById(R.id.endImageButton);
    private Boolean clicked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        sidebarNavView = findViewById(R.id.sidebarNavView);

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

        // get the fab icons
        fabBtn = findViewById(R.id.fab);
        addUrlBtn = findViewById(R.id.fab_url);
        createNewBtn = findViewById(R.id.fab_add_new);


        addUrlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "addUrlBtn icon clicked", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                clicked = !clicked;
//                setVisibility(clicked);
//                setAnimation(clicked);
                createNewPopupBox();
            }
        });

        createNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "createNewBtn icon clicked", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                clicked = !clicked;
//                setVisibility(clicked);
//                setAnimation(clicked);
                createNewAddConfig();
            }
        });

        //date picker
        dateText = findViewById(R.id.startTextView);

        strtImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });
    }

    // a function to switch between fragments using the navDrawer
    private void displaySelectedScreen(int itemId) {

        // initialize a fragment for switching
        Fragment fragment = null;

        switch (itemId) {
            case R.id.nav_menu_home:
                fragment = new HomeFragment();
                break;
            case R.id.nav_menu_data:
                fragment = new DataFragment();
                break;
            default:
                Log.i("Menu Selection", "Menu Item Selection Error: no selection detected");
                break;
        }

        //replacing the fragment
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
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
                setVisibility(clicked);
                setAnimation(clicked);
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        sidemenuToggle.syncState();
        super.onPostCreate(savedInstanceState);

    }

    private void setVisibility(Boolean clicked) {
        // if the fab icon is clicked, show the small buttons
        if(!clicked) {
            addUrlBtn.setVisibility(View.VISIBLE);
            createNewBtn.setVisibility(View.VISIBLE);
        } else {
            // if the fab icon is clicked to be closed, set the visibilities to invisible
            addUrlBtn.setVisibility(View.INVISIBLE);
            createNewBtn.setVisibility(View.INVISIBLE);
        }
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

    public void createNewPopupBox() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View popupView = getLayoutInflater().inflate(R.layout.popup_box, null);

        dialogBuilder.setView(popupView);
        dialog = dialogBuilder.create();
        dialog.show();

        // get the popup elements
        popupCancelBtn = (Button) popupView.findViewById(R.id.cancelButton);
        popupNextBtn = (Button) popupView.findViewById(R.id.nextButton);

        popupCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        popupNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    public void createNewAddConfig() {
        dialogBuilder = new AlertDialog.Builder(this);
        final View addConfigView = getLayoutInflater().inflate(R.layout.add_config, null);

        dialogBuilder.setView(addConfigView);
        dialog = dialogBuilder.create();
        dialog.show();
        strtImgBtn = (ImageButton) addConfigView.findViewById(R.id.startImageButton);
        endImgBtn = (ImageButton) addConfigView.findViewById(R.id.endImageButton);


        // get the popup elements
//        popupCancelBtn = (Button) popupView.findViewById(R.id.cancelButton_add); //duplication
//        popupNextBtn = (Button) popupView.findViewById(R.id.nextButton_add);
//
//        popupCancelBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//
//        popupNextBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
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

    private void showDatePickerDialog(){
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                this,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        String date = month + "/" + dayOfMonth + "/" + year;
        dateText.setText(date);
    }



}