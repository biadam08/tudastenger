package com.szte.tudastenger;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

@Module
@InstallIn(SingletonComponent.class)
public class FirebaseModule {
    @Provides
    public FirebaseAuth provideAuth() {
        return FirebaseAuth.getInstance();
    }
    @Provides
    public FirebaseFirestore provideFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    public StorageReference provideStorage() {
        return FirebaseStorage.getInstance().getReference();
    }

    @Provides
    public FirebaseFunctions provideFirebaseFunctions() {
        return FirebaseFunctions.getInstance();
    }
    @Provides
    public FirebaseMessaging provideFirebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
