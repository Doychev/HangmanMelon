package com.example.martindoychev.hangmanmelon;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.List;


/*TODO: check if any progress dialog changes are needed; probably we have to start it
at the beginning of the login process and end it if an error occurs or at the end
 */

public class MainActivity extends AppCompatActivity {

    private CallbackManager facebookCallbackManager;
    private ProgressDialog loadingDialog;

    private BaasDocument fbUser;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //handle the callback when facebook login is completed
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this code is used to generate the keyhash used for facebook integration
//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(
//                    "com.example.martindoychev.hangmanmelon",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }

        loadingDialog = ProgressDialog.show(MainActivity.this, "", "Loading. Please wait...", true);
        FacebookSdk.sdkInitialize(this);
        facebookCallbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);

        //prepare the BaasBox connection
        BaasBox.builder(this).setAuthentication(BaasBox.Config.AuthType.SESSION_TOKEN)
                .setApiDomain("192.168.1.105")
                .setPort(9000)
                .setAppCode("1234567890")
                .init();

        //if logged in - redirect to the game
        if (AccessToken.getCurrentAccessToken()!=null) {
            signInBaasBox();
        } else {
            loadingDialog.dismiss();
        }

        //register the callback on the Facebook loginButton
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loadingDialog = ProgressDialog.show(MainActivity.this, "", "Loading. Please wait...", true);
                Log.i("facebook-login", "success");
                //if Facebook login is successful, proceed with BaasBox login
                signInBaasBox();
            }

            @Override
            public void onCancel() {
                Log.i("facebook-login", "cancel");
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("facebook-login", e.getMessage());
                //TODO: check if proper error handling is required here
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    //login to BaasBox with an administrator user; the reason for this is that
    //the users with role "registered" don't have permission to access any data in the DB
    private void signInBaasBox(){
        BaasUser user = BaasUser.withUserName("admin");
        user.setPassword("admin");
        user.login(new BaasHandler<BaasUser>() {
            @Override
            public void handle(BaasResult<BaasUser> result) {
                if (result.isFailed()){
                    Log.e("ERROR", "ERROR", result.error());
                    //TODO: check if proper error handling is required here
                } else {
                    completeLogin(result.isSuccess());
                }
            }
        });
    }

    private void completeLogin(boolean success) {
        if (success) {
            Log.i("baasboxlogin", "success");
            //fetch all users and check if the user is already registered; if he isn't - register him and continue; otherwise - go straight to the game
            BaasDocument.fetchAll("facebookusers", new BaasHandler<List<BaasDocument>>() {
                @Override
                public void handle(BaasResult<List<BaasDocument>> res) {
                    if (res.isSuccess()) {
                        Log.i("baasboxusers", "success");
                        try {
                            boolean userExists = false;
                            for (BaasDocument user : res.get()) {
                                if (user.getString("uid").equals(Profile.getCurrentProfile().getId())) {
                                    fbUser = user;
                                    userExists = true;
                                    break;
                                }
                            }
                            if (userExists) {
                                openDashboardActivity();
                            } else {
                                saveFbUser();
                            }
                        } catch (BaasException e) {
                            e.printStackTrace();
                            //TODO: check if proper error handling is required here
                        }
                    } else {
                        Log.e("baasboxusers", "Error", res.error());
                        //TODO: check if proper error handling is required here
                    }
                }
            });
        } else {
            Log.e("baasboxlogin", "failed");
            throw new RuntimeException();
            //TODO: check if proper error handling is required here
        }
    }

    //store the user's name and id in the "facebookusers" collection; when completed, continue to the game
    private void saveFbUser() {
        BaasDocument user = new BaasDocument("facebookusers");
        user.put("uid", Profile.getCurrentProfile().getId());
        user.put("username", Profile.getCurrentProfile().getName());
        user.put("totalGames", 0);
        user.put("wonGames", 0);
        user.put("letterGuesses", 0);
        user.put("wordGuesses", 0);
        user.save(new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> doc) {
                if (doc.isSuccess()) {
                    try {
                        Log.i("baasboxsaveuser", "success");
                        fbUser = doc.get();
                        openDashboardActivity();
                    } catch (BaasException e) {
                        e.printStackTrace();
                        //TODO: check if proper error handling is required here
                    }
                } else {
                    Log.e("baasboxsaveuser", "fail");
                    //TODO: check if proper error handling is required here
                }
            }
        });
    }

    //start the DashboardActivity, passing the user details in the intent
    private void openDashboardActivity() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("fbUser", fbUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
