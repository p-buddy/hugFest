// THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
// KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
// PARTICULAR PURPOSE.
//
// <author>Chris Lee and Ryan Zhou</author>
// <email>wannabedev.ta@gmail.com</email>
// <date>2015-08-14</date>

package com.usc.itp476.contact.contactproject.ingamescreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.usc.itp476.contact.contactproject.ContactApplication;
import com.usc.itp476.contact.contactproject.POJO.GameData;
import com.usc.itp476.contact.contactproject.POJO.GameMarker;
import com.usc.itp476.contact.contactproject.R;
import com.usc.itp476.contact.contactproject.adapters.FriendListGridAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateGameActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();
    private static ArrayList<String> selectedFriendParseIDs;
    private Button buttonCreate = null;
    private TextView textViewwMax = null;
    private SeekBar seekbarMax = null;
    private GridView gridView = null;
    private int maxPoints = 10;
    private GameMarker gameMarkerBeingMade = null;
    private GameData gameBeingMade = null;
    private ParseGeoPoint myLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        selectedFriendParseIDs = new ArrayList<>();
        gridView = (GridView) findViewById(R.id.gridViewInvite);
        buttonCreate = (Button) findViewById(R.id.buttonCreate);
        textViewwMax = (TextView) findViewById(R.id.textViewMaxCreate);
        seekbarMax = (SeekBar) findViewById(R.id.seekbarMax);
        setGridAdapter();
        setListeners();
    }

    private void setGridAdapter(){
        gridView.setAdapter(new FriendListGridAdapter(getApplicationContext(), true, this));
        /*Here we create and set an Adapter for our grid. Note the second parameter is true, which
        stands for yes, display the checkbox because this gridAdapter if for adding friends to a game.
         */
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
    }

    private void setListeners(){
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = ContactApplication.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                /* Below, we have some checks for invalid selections. This way we don't send data to
                the server until we are sure it is appropriate.
                 */
                if (location == null) {
                    Toast.makeText(getApplicationContext(),
                            "Cannot detect location to start game",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else if (selectedFriendParseIDs.size() < 2) {
                    Toast.makeText(getApplicationContext(),
                            "You need to invite at least two friends!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else if (selectedFriendParseIDs.size() > 19) {
                    Toast.makeText(getApplicationContext(),
                            "Maximum number game is 19(+ you)!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    //Here, everything is good and we proceed to create the game
                    myLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                    createGameMarker();
                }
            }
        });

        seekbarMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxPoints = 1 + progress;
                textViewwMax.setText(String.valueOf(maxPoints));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void createGameMarker(){
        /* This method sends data to the server, data needed to display the marker when the user
        looks on the map for created games.
         */
        GameMarker marker = new GameMarker();
        marker.setLocation(myLocation);
        marker.setHostName();
        marker.setGameOver(false);
        marker.setPoints(maxPoints);
        gameMarkerBeingMade = marker;
        marker.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    createGame();
                } else {
                    gameBeingMade = null;
                    Toast.makeText(getApplicationContext(),
                            "Could not make game", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createGame(){

        /* CreateGame is a client heavy code that works extensively with the Parse database to send
        all the information neccessary.
         */
        GameData gameData = new GameData();
        gameData.setLocation(myLocation);
        gameData.setHostName();
        gameData.setPointsToWin(maxPoints);
        gameData.setMarker(gameMarkerBeingMade);
        gameData.setGameOver(false);
        gameBeingMade = gameData;

        gameData.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    gameMarkerBeingMade.setGameID(gameBeingMade);
                    gameMarkerBeingMade.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Toast.makeText(getApplicationContext(),
                                        "Could not make game", Toast.LENGTH_SHORT).show();
                                HashMap<String, String> params = new HashMap<>();
                                params.put("ID", gameMarkerBeingMade.getMarkerID());
                                params.put("type", "Marker");
                                ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                                    @Override
                                    public void done(String s, ParseException e) {
                                        if (e != null) {
                                            Log.wtf(TAG, "could not delete marker when making game");
                                            Log.wtf(TAG, e.getLocalizedMessage());
                                        }
                                    }
                                });
                                selectedFriendParseIDs.clear();
                            } else {
                                ParseRelation<ParseUser> players = gameBeingMade.getRelation("players");
                                players.add(ParseUser.getCurrentUser());
                                gameBeingMade.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            ParseUser me = ParseUser.getCurrentUser();
                                            me.put("inGame", true);
                                            me.put("currentGame", gameBeingMade);
                                            me.put("currentHugs", 0);
                                            me.put("currentTarget", JSONObject.NULL);
                                            me.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    Intent intent = new Intent(
                                                            CreateGameActivity.this.getApplicationContext(),
                                                            TargetActivity.class);
                                                    intent.putExtra(ContactApplication.JOINEDGAME, false);
                                                    intent.putExtra(ContactApplication.MAXPOINTS, maxPoints);
                                                    if (e == null) {
                                                        //Call cloud code to send players to server
                                                        HashMap<String, Object> params = new HashMap<>();
                                                        Log.wtf(TAG, "sending: " + selectedFriendParseIDs.size());
                                                        for (int i = 0; i < selectedFriendParseIDs.size(); ++i) {
                                                            Log.wtf(TAG, "friend: " + selectedFriendParseIDs.get(i));
                                                        }
                                                        params.put("friendIDs", selectedFriendParseIDs);
                                                        params.put("gameID", gameBeingMade.getGameID());
                                                        try {
                                                            ParseCloud.callFunction("addFriendsToGame", params);
                                                            gameBeingMade.fetch();
                                                            intent.putExtra(ContactApplication.GAMEID, gameBeingMade.getGameID());
                                                            startActivityForResult(intent, ContactApplication.REQUEST_CODE_CREATE_GAME);
                                                        } catch (ParseException e2) {
                                                            if (e2.getCode() == -20){
                                                                Toast.makeText(getApplicationContext(),
                                                                        "Could not invite enough players", Toast.LENGTH_SHORT).show();
                                                            }
                                                            Log.wtf(TAG + "addfriendtoGame: ", e2.getLocalizedMessage());
                                                        }finally{
                                                            selectedFriendParseIDs.clear();
                                                        }
                                                    } else {
                                                        Log.wtf(TAG, "trying to put game in player: " + e.getLocalizedMessage());
                                                        HashMap<String, String> params = new HashMap<>();
                                                        params.put("ID", gameMarkerBeingMade.getMarkerID());
                                                        params.put("type", "Marker");
                                                        ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                                                            @Override
                                                            public void done(String s, ParseException e) {
                                                                if (e != null) {
                                                                    Log.wtf(TAG, "could not delete marker when making game");
                                                                    Log.wtf(TAG, e.getLocalizedMessage());
                                                                } else {
                                                                    HashMap<String, String> params = new HashMap<>();
                                                                    params.put("ID", gameBeingMade.getGameID());
                                                                    params.put("type", "Game");
                                                                    ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                                                                        @Override
                                                                        public void done(String s, ParseException e) {
                                                                            if (e != null) {
                                                                                Log.wtf(TAG, "could not delete marker when making game");
                                                                                Log.wtf(TAG, e.getLocalizedMessage());
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                        selectedFriendParseIDs.clear();
                                                    }
                                                }
                                            });
                                        } else {
                                            Log.wtf(TAG, "trying to put player in game: " + e.getLocalizedMessage());
                                            HashMap<String, String> params = new HashMap<>();
                                            params.put("ID", gameMarkerBeingMade.getMarkerID());
                                            params.put("type", "Marker");
                                            ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                                                @Override
                                                public void done(String s, ParseException e) {
                                                    if (e != null) {
                                                        Log.wtf(TAG, "could not delete marker when making game");
                                                        Log.wtf(TAG, e.getLocalizedMessage());
                                                    } else {
                                                        HashMap<String, String> params = new HashMap<>();
                                                        params.put("ID", gameBeingMade.getGameID());
                                                        params.put("type", "Game");
                                                        ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                                                            @Override
                                                            public void done(String s, ParseException e) {
                                                                if (e != null) {
                                                                    Log.wtf(TAG, "could not delete marker when making game");
                                                                    Log.wtf(TAG, e.getLocalizedMessage());
                                                                }
                                                            }
                                                        });
                                                    }
                                                    selectedFriendParseIDs.clear();
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                } else {
                    Log.wtf(TAG, "fail to make game: " + e.getLocalizedMessage());
                    Toast.makeText(getApplicationContext(),
                            "Could not make game", Toast.LENGTH_SHORT).show();
                    HashMap<String, String> params = new HashMap<>();
                    params.put("ID", gameMarkerBeingMade.getMarkerID());
                    params.put("type", "Marker");
                    ParseCloud.callFunctionInBackground("deleteGameData", params, new FunctionCallback<String>() {
                        @Override
                        public void done(String s, ParseException e) {
                            if (e != null) {
                                Log.wtf(TAG, "could not make game and could not delete marker");
                                Log.wtf(TAG, e.getLocalizedMessage());
                            }
                        }
                    });
                    selectedFriendParseIDs.clear();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static ArrayList<String> getSelectedFriendParseIDs() {
        return selectedFriendParseIDs;
    }
}