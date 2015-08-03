package com.usc.itp476.contact.contactproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.usc.itp476.contact.contactproject.slidetab.AllTabActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class StartActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();
    public static ParseUser user = null;
    public static final int REQUEST_START_GAME = 104;
    public static final int RESULT_ALLTABS_QUIT_STAY_LOGIN = 105;
    public static final int RESULT_LOGOUT = 200;
    public static final List<String> permissions =
            Arrays.asList("public_profile", "user_friends", "email");

    private String name = null;
    private String pictureURL = null;
    private String userEmail = null;
    private String facebookID = null;
    private JSONArray facebookIDs = null;
    private LoginResult loginResult = null;
    private ProfileTracker profileTracker = null;
    private LoginButton buttonLoginFacebook = null;
    private CallbackManager callbackManager = null;
    private HashMap< String, ArrayList<ParseUser> > parseFriendIDs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        setContentView(R.layout.activity_start);
        buttonLoginFacebook = (LoginButton) findViewById(R.id.login_button);
        createFacebookCallback();
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile newProfile) {
                if( newProfile != null){
                    Profile.setCurrentProfile(newProfile);
                }
                profileTracker.stopTracking();
            }
        };
        profileTracker.startTracking();
        checkSharedPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Logs 'app deactivate' App Event
        AppEventsLogger.deactivateApp(StartActivity.this);
    }

    //Causes app to crash
    @Override
    protected void onResume() {
        super.onResume();
        // Logs 'install' and 'app activate' App Events
        AppEventsLogger.activateApp(StartActivity.this);
        checkSharedPreferences();
    }

    //Called when facebook is done
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_ALLTABS_QUIT_STAY_LOGIN){
            finish();
        }else{
            callbackManager.onActivityResult( requestCode, resultCode, data);
            ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createFacebookCallback(){
        FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                StartActivity.this.loginResult = loginResult;
                //We have the accessToken, now we want to do a graphRequest to get the user data
                findFacebookUserData();
            }
            @Override
            public void onCancel() {
                Log.wtf(TAG, "cancel");
            }
            @Override
            public void onError(FacebookException e) {
                Log.wtf(TAG, "error");
            }
        };
        callbackManager = CallbackManager.Factory.create();
        buttonLoginFacebook.setReadPermissions(permissions);
        buttonLoginFacebook.registerCallback(callbackManager, callback);
    }

    private void checkSharedPreferences(){

        SharedPreferences sharedPreferences = getSharedPreferences(
                ContactApplication.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(ContactApplication.IS_LOGGED_IN, false);
        Log.wtf(TAG, "Check shared pref, isLoggedIn = "+ isLoggedIn );
        if (isLoggedIn){
            goToHome();
        }
    }

    private void addFaceBookFriends(){
        new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/friends",
            null,
            HttpMethod.GET,
            new GraphRequest.Callback() {
                public void onCompleted(GraphResponse response) {
                    JSONObject tempArray = response.getJSONObject();
                    try {
                        if( tempArray!= null ){
                            facebookIDs = tempArray.getJSONArray("data");
                            if( facebookIDs.length() > 0 ) {
                                //actually have friends then proceed
                                HashMap<String, Object> parms = new HashMap<>();
                                parms.put("ids", facebookIDs);
                                try {
                                    //pass in a HashMap, get back a HashMap
                                    parseFriendIDs = ParseCloud.callFunction("getParseFriendsFromFBID", parms);
                                } catch (Exception e) {
                                    Log.wtf(TAG, e.getMessage());
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.wtf(TAG, e.getLocalizedMessage());
                    }
                  createFaceBookParseUser();
                }
            }
        ).executeAsync();
    }

    private void findFacebookUserData(){

        //This method calls a graphrequest to get user id
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        try {
                            userEmail = object.get("email").toString();
                            facebookID = object.get("id").toString();
                            name = object.get("name").toString();
                            pictureURL = object.getJSONObject("picture").getJSONObject("data").getString("url");

                        } catch (JSONException e) {
                            Log.wtf(TAG + "Graph request catch: ", e.getLocalizedMessage());
                        }
                        HashMap<String, Object> parms = new HashMap<>();
                        parms.put("facebookID", facebookID);
                        String tokenID;
                        try {
                            tokenID = ParseCloud.callFunction("getUserSessionToken", parms);
                        } catch (ParseException e) {
                            //New user, so we need to do another request to get friends
                            addFaceBookFriends();
                            return;
                        }
                        if (tokenID != null) {
                            try {
                                //OLD USER
                                user = ParseUser.become(tokenID);
                                Log.wtf(TAG, "setLoggedIn for old user");
                                ContactApplication.setLoggedIn(true);
                                goToHome();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,id,email,picture.width(300).height(300)");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void createFaceBookParseUser() {
        user = new ParseUser();
        user.setUsername(name);
        user.put("name", name);
        user.setPassword(name + ((int)(Math.random() * 9999)));
        user.setEmail(userEmail);
        user.put("totalGames", 0);
        user.put("totalHugs", 0);
        user.put("facebookID", facebookID);
        user.put("pictureLink", pictureURL);
        user.put("inGame", false);

        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    // Hooray! Let them use the app now.
                    ParseFacebookUtils.linkInBackground(user, loginResult.getAccessToken(), new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                ParseRelation<ParseUser> userFriends = user.getRelation("friends");
                                ArrayList<ParseUser> friends;
                                //Arrays in cloudcode are ArrayLists in Android
                                friends = parseFriendIDs.get("friends");
                                //add each friend individually and then save alltogether
                                for (int i = 0; i < friends.size(); ++i) {
                                    userFriends.add(friends.get(i));
                                }
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            Log.wtf(TAG, "setLoggedIn for new user");
                                            ContactApplication.setLoggedIn(true);
                                            goToHome();
                                        } else {
                                            Log.wtf(TAG, e.getLocalizedMessage());
                                        }
                                    }
                                });
                            } else {
                                Log.wtf(TAG, e.getLocalizedMessage());
                            }
                        }
                    });

                } else {
                    // Sign up didn't succeed. Look at the ParseException
                    // to figure out what went wrong
                    Log.wtf(TAG, e.getLocalizedMessage());
                    Toast.makeText(getApplicationContext(),
                            "Could not sign you up", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void goToHome(){
        ParseInstallation pi = ParseInstallation.getCurrentInstallation();
        pi.put("currentUser", ParseUser.getCurrentUser());
        pi.put("currentUserID", ParseUser.getCurrentUser().getObjectId());
        pi.saveInBackground();
        Intent i = new Intent(getApplicationContext(), AllTabActivity.class);
        startActivityForResult(i, REQUEST_START_GAME);
    }
}