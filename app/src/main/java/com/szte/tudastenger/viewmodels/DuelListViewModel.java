package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.Question;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.DuelRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DuelListViewModel extends AndroidViewModel {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DuelRepository duelRepository;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Duel>> pendingDuels = new MutableLiveData<>();
    private final MutableLiveData<List<Duel>> finishedDuels = new MutableLiveData<>();
    private final Map<String, MutableLiveData<String>> categoryAndQuestionDataMap = new HashMap<>();

    @Inject
    public DuelListViewModel(Application application, DuelRepository duelRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        super(application);
        this.duelRepository = duelRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        loadUser();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Duel>> getPendingDuels() { return pendingDuels; }
    public LiveData<List<Duel>> getFinishedDuels() { return finishedDuels; }
    public LiveData<String> getCategoryAndQuestionNumber(String duelId, String categoryId, int questionCount) {
        if (!categoryAndQuestionDataMap.containsKey(duelId)) {
            MutableLiveData<String> liveData = new MutableLiveData<>();
            categoryRepository.loadCategoryNameForDuel(
                    categoryId,
                    categoryName -> {
                        if (categoryName != null) {
                            String categoryAndQuestionText = categoryName + " / " + questionCount + " db";
                            liveData.postValue(categoryAndQuestionText);
                        }
                    }
            );
            categoryAndQuestionDataMap.put(duelId, liveData);
        }
        return categoryAndQuestionDataMap.get(duelId);
    }


    public LiveData<String> getUsernames(Duel currentDuel) {
        MutableLiveData<String> usernames = new MutableLiveData<>();

        userRepository.getUsernamesForDuel(
                currentDuel.getChallengerUid(),
                currentDuel.getChallengedUid(),
                playerUsernames -> usernames.setValue(playerUsernames)
        );

        return usernames;
    }

    private void loadUser() {
        userRepository.loadCurrentUser(user -> {
                    currentUser.setValue(user);
                    loadDuels();
                }
        );
    }

    private void loadDuels() {
        loadPendingDuels();
        loadFinishedDuels();
    }

    private void loadPendingDuels() {
        User user = currentUser.getValue();
        if (user != null) {
            duelRepository.loadPendingDuels(
                    user.getId(),
                    pendingDuelsList -> pendingDuels.setValue(pendingDuelsList)
            );
        }
    }

    private void loadFinishedDuels() {
        User user = currentUser.getValue();
        if (user != null) {
            duelRepository.loadFinishedDuels(
                    user.getId(),
                    finishedDuelsList -> finishedDuels.setValue(finishedDuelsList)
            );
        }
    }
}
