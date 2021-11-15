package com.example.crepe;

import android.app.AlertDialog;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button popupCancelBtn;
    private Button popupNextBtn;

    private Animation fromBottom;
    private Animation toBottom;

    private Boolean clicked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load animations
        fromBottom = AnimationUtils.loadAnimation( this, R.anim.from_bottom );
        toBottom = AnimationUtils.loadAnimation( this, R.anim.to_bottom );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        // get the fab icons
        fabBtn = findViewById(R.id.fab);
        addUrlBtn = findViewById(R.id.fab_url);
        createNewBtn = findViewById(R.id.fab_add_new);

        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Fab icon clicked", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                clicked = !clicked;
                setVisibility(clicked);
                setAnimation(clicked);
            }
        });

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


    private void setVisibility(Boolean clicked) {
        if(clicked) {
            Log.i("hello", "hello");
            addUrlBtn.setVisibility(View.VISIBLE);
            createNewBtn.setVisibility(View.VISIBLE);
        } else {
            addUrlBtn.setVisibility(View.INVISIBLE);
            createNewBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void setAnimation(Boolean clicked) {
        if(clicked) {
            addUrlBtn.startAnimation(toBottom);
            createNewBtn.startAnimation(toBottom);
        } else {
            addUrlBtn.startAnimation(fromBottom);
            createNewBtn.startAnimation(fromBottom);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}