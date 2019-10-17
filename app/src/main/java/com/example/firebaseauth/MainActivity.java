package com.example.firebaseauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    FirebaseAuth mAuth = null;
    TextView textSwitch, switchBtn, textError;
    EditText nameEdit, emailEdit, passwordEdit, emailLogin, passwordLogin;
    ViewSwitcher viewSwitcher;
    LinearLayout signup,login;
    Button continueBtn;
    RelativeLayout phoneContinue;

    //a constant for detecting the login intent result
    private static final int RC_SIGN_IN = 234;

    //a constant for detecting the login intent result
    private static final int FACEBOOK_SIGN_IN = 233;

    //Tag for the logs optional
    private static final String TAG = "simplifiedcoding";

    //creating a GoogleSignInClient object
    GoogleSignInClient mGoogleSignInClient;

    GoogleApiClient googleApiClient;

    CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        //________________ Email & password Authentication _________________________________________

        textSwitch = findViewById(R.id.textSwitch);
        switchBtn = findViewById(R.id.switchBtn);

        viewSwitcher = findViewById(R.id.viewSwitcher);

        signup = findViewById(R.id.signup);
        login = findViewById(R.id.login);

        emailLogin = findViewById(R.id.emailLoginEdt);
        passwordLogin = findViewById(R.id.passwordLoginEdt);

        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewSwitcher.getCurrentView()==signup){
                    viewSwitcher.showNext();
                    textSwitch.setText("You don't have an account ?");
                    switchBtn.setText("Sign up");
                }
                else {
                    viewSwitcher.showPrevious();
                    textSwitch.setText("Already have have an account ?");
                    switchBtn.setText("Login");
                }
            }
        });
        
        continueBtn = findViewById(R.id.continueBtn);
        
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewSwitcher.getCurrentView() == signup){
                    register();
                }
                else {
                    login();
                }
            }
        });




        //____________________________ Phone Authentication ___________________________________

        phoneContinue = findViewById(R.id.phoneContinue);
        phoneContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PhoneActivity.class);
                startActivity(intent);
            }
        });



        //________________ Google Authentication _________________________________________

        //we need a GoogleSignInOptions object
        //And we need to build it as below
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        googleApiClient =new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();

        //Then we will get the GoogleSignInClient object from GoogleSignIn class
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.googleContinue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });



        //____________________ Facebook LogIn __________________________________________

        // Initialize Facebook Login button

        RelativeLayout loginButton = findViewById(R.id.facebookContinue);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCallbackManager = CallbackManager.Factory.create();
                LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("email","public_profile"));
                LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "facebook:onCancel");
                        // ...
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "facebook:onError", error);
                        // ...
                    }
                });

            }
        });


    }


    //__________________ Methods of Email and Password Auth ________________________________

    private void register() {

        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Sending Verification Email");

        nameEdit = findViewById(R.id.nameEdit);
        String name = nameEdit.getText().toString();

        emailEdit = findViewById(R.id.emailEdit);
        String email = emailEdit.getText().toString();

        passwordEdit = findViewById(R.id.passwordEdit);
        String password = passwordEdit.getText().toString();

        textError = findViewById(R.id.errortext);

        if (!(password.isEmpty()) && !(email.isEmpty()) && !(name.isEmpty()) && password.length() > 5) {
            textError.setVisibility(View.INVISIBLE);
            progressDialog.show();
            mAuth = FirebaseAuth.getInstance();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {

                        sendVerificationEmail();

                    } else {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                progressDialog.dismiss();
                            }
                        }, 1000);
                        // If sign in fails, display a message to the user.
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (password.length() < 6) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        progressDialog.dismiss();
                    }
                }, 1000);
                // If sign in fails, display a message to the user.
                Toast.makeText(MainActivity.this, "Password should be more than 6 caracters", Toast.LENGTH_SHORT).show();
            } else {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        progressDialog.dismiss();
                    }
                }, 1000);
                // If sign in fails, display a message to the user.
                Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                textError.setVisibility(View.VISIBLE);
            }
        }



    }

    private void sendVerificationEmail() {
        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Sending Verification Email");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        currentUser.sendEmailVerification().addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                        }
                    }, 1000);
                    mAuth.signOut();
                    progressDialog.dismiss();
                    viewSwitcher.showNext();
                    progressDialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });

    }

    private void login(){
        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Logged In");

        String Email = emailLogin.getText().toString();

        String Password = passwordLogin.getText().toString();

        TextView ErrorText = findViewById(R.id.errortext2);

        if (!(Email.isEmpty()) && !(Password.isEmpty())){
            ErrorText.setVisibility(View.INVISIBLE);
            progressDialog.show();
            mAuth.signInWithEmailAndPassword(Email,Password)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){

                                verifyEmailAddress();
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"Wrong Credentials",Toast.LENGTH_LONG).show();
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        progressDialog.dismiss();
                                    }
                                }, 1000);

                            }
                        }
                    });
        }else {
            ErrorText.setVisibility(View.VISIBLE);
        }
    }

    private void verifyEmailAddress(){
        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Logged In");

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser.isEmailVerified()){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    progressDialog.dismiss();
                }
            }, 1000);

            // You can change the intent as you like ..

            Intent goToHome = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(goToHome);

        }else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    progressDialog.dismiss();
                }
            }, 1000);
            Toast.makeText(getApplicationContext(),"Please verify your account",Toast.LENGTH_LONG).show();
        }

    }


    //________________________ Google Sign in _____________________________________________


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        //getting the auth credential
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        //Now using firebase we are signing in the user here
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            Toast.makeText(MainActivity.this, "User Signed In", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }


    //this method is called on click
    private void signIn() {
        //getting the google signin intent
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleApiClient.clearDefaultAccountAndReconnect();

        //starting the activity for result
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    //_________________________ Facebook log in ___________________________________________

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }


    // _______________________ Other methodes ____________________________________________

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent goToHome = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(goToHome);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if the requestCode is the Google Sign In code that we defined at starting
        if (requestCode == RC_SIGN_IN) {

            //Getting the GoogleSignIn Task
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                //Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                //authenticating with firebase
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(MainActivity.this,"I'm here"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        else{

            // Pass the activity result back to the Facebook SDK
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }


}
