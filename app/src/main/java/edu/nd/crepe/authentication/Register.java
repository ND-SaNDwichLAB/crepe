package edu.nd.crepe.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import edu.nd.crepe.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends AppCompatActivity {

    private static final String TAG = "Register";

    EditText accountEditTextRegister, passwordEditTextRegister;
    Button registerButtonRegister, loginButtonRegister;
    FirebaseAuth mAuth;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        accountEditTextRegister = findViewById(R.id.accountIdEditTextRegister);
        passwordEditTextRegister = findViewById(R.id.passwordEditTextRegister);
        registerButtonRegister = findViewById(R.id.RegisterButtonRegister);
        loginButtonRegister = findViewById(R.id.LoginButtonRegisterUI);

        loginButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        registerButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // the email is trimmed because keyboards like to append a trailing space after autocomplete
                String email = accountEditTextRegister.getText().toString().trim();
                String password = passwordEditTextRegister.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Register.this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                registerButtonRegister.setEnabled(false);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                registerButtonRegister.setEnabled(true);
                                if (task.isSuccessful()) {
                                    // the account is created and signed in
                                    Log.d(TAG, "createUserWithEmail:success");
                                    Toast.makeText(Register.this, "Account created.", Toast.LENGTH_SHORT).show();

                                } else {
                                    // If registration fails, display a message to the user.
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(Register.this, "Registration failed.",
                                            Toast.LENGTH_SHORT).show();

                                }
                            }
                        });

            }
        });


    }
}
