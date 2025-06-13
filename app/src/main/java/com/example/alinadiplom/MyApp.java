package com.example.alinadiplom;

import android.app.Application;

import com.example.alinadiplom.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            DatabaseReference notifRef = FirebaseDatabase.getInstance()
                    .getReference("UserNotifications")
                    .child(uid);

            notifRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);

                    if (title != null && message != null) {
                        NotificationHelper.showLocalNotification(getApplicationContext(), title, message);
                    }
                }

                @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
                @Override public void onChildRemoved(DataSnapshot snapshot) {}
                @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
                @Override public void onCancelled(DatabaseError error) {}
            });
        }
    }
}
