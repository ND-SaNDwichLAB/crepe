package com.example.crepe.authentication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.crepe.MainActivity;
import com.example.crepe.R;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.User;
import com.example.crepe.network.FirebaseCommunicationManager;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.Calendar;

public class GoogleLoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<Intent> signInLauncher;
    private SignInButton signInButton;
    private static final String TAG = "GoogleLoginActivity";

    private static final int RC_SIGN_IN = 100;

    private DatabaseManager dbManager;
    private FirebaseCommunicationManager fbManager;


    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // initialize database managers
        dbManager = DatabaseManager.getInstance(getApplicationContext());
        fbManager = new FirebaseCommunicationManager(getApplicationContext());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signInButton = findViewById(R.id.googleSignInButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                signInLauncher.launch(intent);

            }
        });

        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Log.d(TAG, "onActivityResult: Google Sign In Activity Result");
                Intent data = result.getData();

                Bundle extras = data.getExtras();
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        Log.d(TAG, key + ": " + extras.get(key));
                    }
                }

                int resultCode = result.getResultCode();
                if (resultCode == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "onActivityResult: Google Sign In Successful");
                        firebaseAuthWithGoogle(account);
                        finish();
                    } catch (ApiException e) {
                        Log.d(TAG, "onActivityResult: Google Sign In Failed");
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "onActivityResult: Google Sign In Failed");
                }
            }
        });
    }



    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                          @Override
                                          public void onSuccess(AuthResult authResult) {
                                              Log.d(TAG, "firebaseAuthWithGoogle: Sign In Successful");
                                              FirebaseUser user = mAuth.getCurrentUser();
                                              createNewUser(user.getUid(), user.getDisplayName());
                                              if (authResult.getAdditionalUserInfo().isNewUser()) {
                                                  Log.d(TAG, "onSuccess: New User");
                                              } else {
                                                  Log.d(TAG, "onSuccess: Existing User");
                                              }
                                              Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                              startActivity(intent);
                                              finish();
                                          }
                                      }
                )
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "firebaseAuthWithGoogle: Sign In Failed");
                        e.printStackTrace();
                    }
                });
    }

    private void createNewUser(String uid, String name) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        User user = new User(uid, name, currentTime, currentTime);

        try {
            fbManager.putUser(user);
            dbManager.addOneUser(user);
        } catch (Exception e) {
            Log.e(TAG, "createNewUser: Error creating new user");
            e.printStackTrace();
        }
    }

}
