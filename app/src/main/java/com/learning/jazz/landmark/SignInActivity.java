package com.learning.jazz.landmark;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import models.SignInConstants;
import models.SignInType;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initView();
        detectIntent();
    }

    private void detectIntent() {
        Intent intent = getIntent();
        SignInType command = (SignInType) intent.getSerializableExtra(SignInConstants.SIGN_IN_COMMAND);
        if(command == null){
            return;
        }
        switch (command){
            case LOGOUT:
                signOut();
                break;
            case LOGIN:
                signIn();
                break;
        }
    }

    private void initView() {
        findViewById(R.id.button_signIn).setOnClickListener(this);
        //configure signin
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        //initialize client to connect
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_signIn:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }

    private void signOut(){
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }
    @Override
    public void onStart(){
        super.onStart();
        updateUI(GoogleSignIn.getLastSignedInAccount(this));
    }


    private void updateUI(GoogleSignInAccount account) {
        //TODO: in account is not null, will go to map view instead, else enable login screen
        if(account != null){
            findViewById(R.id.button_signIn).setEnabled(false);
            startActivity(new Intent(this,MapsActivity.class));
        }
        findViewById(R.id.button_signIn).setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //handle sign in result
        switch (requestCode){
            case RC_SIGN_IN:
                handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data));
                break;
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            updateUI(completedTask.getResult(ApiException.class));
        } catch (ApiException e) {
            error(this.getClass().getName(), "signInResult:failed code=" + e.getStatusCode());

            updateUI(null);
        }
    }

    private void error(String name, String msg) {
        Log.w(name,msg);
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}
