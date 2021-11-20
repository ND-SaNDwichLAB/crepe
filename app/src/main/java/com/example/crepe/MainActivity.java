package com.example.crepe;

import android.app.AlertDialog;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Layout;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.crepe.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FloatingActionButton fabBtn;
    private FloatingActionButton addUrlBtn;
    private FloatingActionButton createNewBtn;

    private Toolbar appToolbar;

    private ActionBarDrawerToggle sidemenuToggle;
    private DrawerLayout drawerLayout;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button popupCancelBtn;
    private Button popupNextBtn;

    private Animation top_appear_anim;
    private Animation top_disappear_anim;
    private Animation left_appear_anim;
    private Animation left_disappear_anim;

    private Boolean clicked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set toolbar
        appToolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(appToolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(true);

        // load animations
        top_appear_anim = AnimationUtils.loadAnimation( this, R.anim.top_appear);
        top_disappear_anim = AnimationUtils.loadAnimation( this, R.anim.top_disappear);
        left_appear_anim = AnimationUtils.loadAnimation( this, R.anim.left_appear);
        left_disappear_anim = AnimationUtils.loadAnimation( this, R.anim.left_disappear);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // sidebar toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        sidemenuToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(sidemenuToggle);
        sidemenuToggle.syncState();

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
                createNewPopupBox();
            }
        });



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

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

}