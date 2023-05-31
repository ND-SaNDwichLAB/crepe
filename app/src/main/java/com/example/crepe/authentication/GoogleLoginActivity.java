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
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleLoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<Intent> signInLauncher;
    private SignInButton signInButton;
    private static final String TAG = "GoogleLoginActivity";

    private static final int RC_SIGN_IN = 100;


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
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(String.valueOf(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signInButton = findViewById(R.id.googleSignInButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Google Sign In Button Clicked");
                Intent intent = mGoogleSignInClient.getSignInIntent();
                signInLauncher.launch(intent);

            }
        });

        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Log.d(TAG, "onActivityResult: Google Sign In Activity Result");
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                if (resultCode == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    Log.d(TAG, "onActivityResult: Google Sign In intent result");
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
                                              String uid = user.getUid();
                                              String email = user.getEmail();
                                              Log.d(TAG, "firebaseAuthWithGoogle: UID: " + uid);
                                              Log.d(TAG, "firebaseAuthWithGoogle: Email: " + email);
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

}
