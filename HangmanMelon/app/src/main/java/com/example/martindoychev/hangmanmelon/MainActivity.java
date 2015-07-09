package com.example.martindoychev.hangmanmelon;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestToken;


public class MainActivity extends AppCompatActivity {

    private RequestToken mSignupOrLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BaasBox.builder(this).setAuthentication(BaasBox.Config.AuthType.SESSION_TOKEN)
                .setApiDomain("192.168.1.105")
                .setPort(9000)
                .setAppCode("1234567890")
                .init();

        String mUsername = "admin", mPassword="admin";
        signupWithBaasBox(false, mUsername, mPassword);

        Intent intent = new Intent(this,GameActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }

    private void signupWithBaasBox(boolean newUser, String mUsername, String mPassword){
        BaasUser user = BaasUser.withUserName(mUsername);
        user.setPassword(mPassword);
        if (newUser) {
            mSignupOrLogin=user.signup(onComplete);
        } else {
            mSignupOrLogin=user.login(onComplete);
        }
    }

    private final BaasHandler<BaasUser> onComplete =
            new BaasHandler<BaasUser>() {
                @Override
                public void handle(BaasResult<BaasUser> result) {

                    mSignupOrLogin = null;
                    if (result.isFailed()){
                        Log.d("ERROR","ERROR",result.error());
                    }
                    completeLogin(result.isSuccess());
                }
            };

    private void completeLogin(boolean success){
        //showProgress(false);
        mSignupOrLogin = null;
        if (success) {
//            Intent intent = new Intent(this,NoteListActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//            finish();
            Log.i("baasboxlogin", "success");
        } else {
            Log.i("baasboxlogin", "failed");
//            mPasswordView.setError(getString(R.string.error_incorrect_password));
//            mPasswordView.requestFocus();
        }
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
