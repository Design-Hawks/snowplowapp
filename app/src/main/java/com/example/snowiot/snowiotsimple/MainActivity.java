package com.example.snowiot.snowiotsimple;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    //to check if user is already or still logged in
    private FirebaseAuth.AuthStateListener mAuthListener;

    //define firebase auth
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private Button mStartWork;
    private Button mEndWork;
    private TextView mWelcomeMessage;
    private TextView mDrivewaysAvailable;
    private TextView mRatingNumber;
    private RatingBar mUserRatings;

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    int metricMode = 1;
    String userType;
//    float userRating = 0.0f;
//    int numberOfRatings = 0;

    NotificationManager notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartWork = (Button) findViewById(R.id.startWork);
        mEndWork = (Button) findViewById(R.id.endWork);
        mDrivewaysAvailable = (TextView) findViewById(R.id.jobsAvailable);
        mWelcomeMessage = (TextView) findViewById(R.id.welcomeMessage);
        mRatingNumber = (TextView) findViewById(R.id.rating);
        mUserRatings = (RatingBar) findViewById(R.id.snowPlowRating);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {             //If getCurrentUser == null no user is logged in.

                    startActivity(new Intent(MainActivity.this, Login.class)); //if user is not logged in, go back to login activity and end main activity.
                    finish();                                                  //end activity
                    finish();                                                  //end activity
                }

            }
        };

        mAuth = FirebaseAuth.getInstance();         //get current instance of whos authenticated
        mUser = FirebaseAuth.getInstance().getCurrentUser();        //get current user, note FirebaseUser is not the same as FirebaseAuth

        ((GlobalVariables) this.getApplication()).storeUserUID(mUser.getUid());      //store useruid in a global variable so that it can be accessed by all activities

        DatabaseReference mUserHandleRef = FirebaseDatabase.getInstance().getReference("users/" + mUser.getUid() + "/requesthandle"); //dynamic reference to requesthandle

        DatabaseReference mUserTypeRef = mRootRef.child("driveways/" + mUser.getUid() + "/type");
        mUserTypeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userType = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Monitor when "user to have job delivered to" flag changes in the database to trigger action
        DatabaseReference mAcceptDeclineNotification = mUserHandleRef.child("jobDeliveredToUID");
        mAcceptDeclineNotification.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String s = dataSnapshot.getValue(String.class);
                if(!(s.equals("null"))){
                    ((GlobalVariables) getApplication()).setJobDeliveredToUID(s);
                    alertSnowplowNotification("Snow-Sense User Response", "User has accepted your offer. Click to open assignment window.","User has accepted your offer.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mStartWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mRootRef.child("driveways/" + mUser.getUid() + "/status").setValue(1);
//                Toast.makeText(getApplicationContext(), "You are now on duty. Your location is now broadcast.", Toast.LENGTH_SHORT).show();
                startShiftPrompt();
            }
        });

        mEndWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRootRef.child("driveways/" + mUser.getUid() + "/status").setValue(0);
                Toast.makeText(getApplicationContext(), "You are now off Duty. Your location was disabled.", Toast.LENGTH_SHORT).show();
            }
        });


        DatabaseReference mGetUserNameRef = mRootRef.child("driveways/" + mUser.getUid() + "/name");
        mGetUserNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                mWelcomeMessage.setText("Welcome " + name + ".");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference mDrivewaysRef = mRootRef.child("driveways");
        mDrivewaysRef.addChildEventListener(new ChildEventListener() {
            int drivewaysAvailable = 0;
            HashMap<String, Integer> drivewaysAvailableForService = new HashMap<String, Integer>();
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Driveways driveway = dataSnapshot.getValue(Driveways.class);

                drivewaysAvailableForService.put(dataSnapshot.getKey(), driveway.getStatus());

                if ((driveway.getType().equals("sensor"))&&(driveway.getStatus() == 2)){
                drivewaysAvailable = drivewaysAvailable + 1;
                }

                if (drivewaysAvailable == 1) {
                    mDrivewaysAvailable.setText("There is " + String.valueOf(drivewaysAvailable) + " driveway in need of snow removal services.");
                }
                else{
                    mDrivewaysAvailable.setText("There are " + String.valueOf(drivewaysAvailable) + " driveways in need of snow removal services.");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                drivewaysAvailable = 0;

                Driveways driveway = dataSnapshot.getValue(Driveways.class);

                drivewaysAvailableForService.put(dataSnapshot.getKey(), driveway.getStatus());

                for (String key : drivewaysAvailableForService.keySet()){
                    if (drivewaysAvailableForService.get(key) == 2){
                        drivewaysAvailable = drivewaysAvailable + 1;
                    }
                }

                if (drivewaysAvailable == 1) {
                    mDrivewaysAvailable.setText("There is " + String.valueOf(drivewaysAvailable) + " driveway in need of snow removal services.");
                }
                else{
                    mDrivewaysAvailable.setText("There are " + String.valueOf(drivewaysAvailable) + " driveways in need of snow removal services.");
                }

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final DatabaseReference mUserRating = mRootRef.child("users/" + mUser.getUid() + "/ratings");
        mUserRating.addChildEventListener(new ChildEventListener() {
            int numberOfRatings = 0;
            float userRating = 0.0f;
            HashMap<String, Float> storeRatings = new HashMap<String, Float>();

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
             if (!(dataSnapshot.getValue() == null)){
                 userRating = userRating + dataSnapshot.getValue(Float.class);
                 numberOfRatings = numberOfRatings + 1;
                 storeRatings.put(dataSnapshot.getKey(),dataSnapshot.getValue(Float.class));
             }
                mUserRatings.setRating(userRating/numberOfRatings);
                mRatingNumber.setText("Your overall rating: " + Float.toString(userRating/numberOfRatings));


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                numberOfRatings = 0;
                userRating = 0.0f;

                storeRatings.put(dataSnapshot.getKey(), dataSnapshot.getValue(Float.class));

                for (String key : storeRatings.keySet()) {
                    userRating = userRating + storeRatings.get(key);
                    numberOfRatings = numberOfRatings + 1;
                }
                mUserRatings.setRating(userRating/numberOfRatings);
                mRatingNumber.setText("Your overall rating: " + Float.toString(userRating/numberOfRatings));
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    @Override   //Added this too
    protected void onStart() {
        super.onStart();


        mAuth.addAuthStateListener(mAuthListener);  //Add authenticator state listener


    }

    //Code by Andres Menendez (Youtube)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.action_map) {
            Intent drivewaySensorsMap = new Intent(MainActivity.this, maps.class);
            startActivity(drivewaySensorsMap);
            return true;
        }

        if (id == R.id.action_contacterrequest) {
            Intent userContactedWindow = new Intent(MainActivity.this, MapsServiceMode.class);
            startActivity(userContactedWindow);
            return true;
        }


        if (id == R.id.action_signout) {
            FirebaseAuth.getInstance().signOut();
            Intent returnToLoginScreen = new Intent(MainActivity.this, Login.class);
            startActivity(returnToLoginScreen);
            finish();   //end main activity after logging out
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *Copy of "alertSensorUserOfService"
     */
    public void alertSnowplowNotification (String contentTitle, String contentText, String contentTicker){

        //Set notification message contents
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new
                NotificationCompat.Builder(this)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setTicker(contentTicker)
                .setAutoCancel(true)                                    //Automatically clear notification when clicked on the task bar
                .setSmallIcon(R.drawable.alreadyplowing);

        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        notificationBuilder.setOnlyAlertOnce(true);
        Intent mapsWindow = new Intent(this, MapsServiceMode.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MapsServiceMode.class);
        taskStackBuilder.addNextIntent(mapsWindow);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2, notificationBuilder.build());
    }

    public void startShiftPrompt (){

        AlertDialog.Builder mAlert = new AlertDialog.Builder(this);
        mAlert.setMessage("Are you sure that you want to begin your shift?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        mRootRef.child("driveways/" + mUser.getUid() + "/status").setValue(1);
                        Toast.makeText(getApplicationContext(), "You are now on duty. Your location is now broadcast.", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                })
                .create();
        mAlert.show();
    }


}
