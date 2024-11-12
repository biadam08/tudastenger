package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Rank;
import com.szte.tudastenger.repositories.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RankEditUploadViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final RankRepository rankRepository;

    private final MutableLiveData<String> rankId = new MutableLiveData<>();
    private final MutableLiveData<String> rankName = new MutableLiveData<>();
    private final MutableLiveData<String> threshold = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private final MutableLiveData<Rank> rankData = new MutableLiveData<>();

    @Inject
    public RankEditUploadViewModel(Application application, UserRepository userRepository, RankRepository rankRepository) {
        super(application);
        this.userRepository = userRepository;
        this.rankRepository = rankRepository;
    }

    public LiveData<String> getRankId() { return rankId; }
    public LiveData<String> getRankName() { return rankName; }
    public LiveData<String> getThreshold() { return threshold; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Rank> getRankData() { return rankData; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }

    public void checkAdmin() {
        userRepository.checkAdmin(isAdminStatus -> isAdmin.setValue(isAdminStatus), error -> errorMessage.setValue(error));
    }

    public void init(String rankId) {
        this.rankId.setValue(rankId);
        if (rankId != null) {
            loadRankData(rankId);
        }
    }

    public void uploadRank(String name, String pointThreshold) {
        if (name.isEmpty()) {
            errorMessage.setValue("A kategória neve nem lehet üres");
            return;
        }
        rankName.setValue(name);
        threshold.setValue(pointThreshold);

        if (rankId.getValue() != null) {
            updateRank();
        } else {
            addNewRank();
        }
    }

    private void updateRank() {
        try {
            Rank rank = new Rank(
                    rankId.getValue(),
                    rankName.getValue().trim(),
                    Long.parseLong(threshold.getValue())
            );


            rankRepository.updateRank(rank, message -> {
                clearData();
                successMessage.setValue(message);
            }, error -> errorMessage.setValue(error));
        } catch (NumberFormatException e) {
            errorMessage.setValue("A ponthatár szám legyen");
        } catch (Exception e){
            errorMessage.setValue("Hiba a feltöltés során");
        }
    }

    private void addNewRank() {
        try {
            Rank rank = new Rank(
                    rankId.getValue().trim(),
                    rankName.getValue(),
                    Long.parseLong(threshold.getValue())
            );

            rankRepository.addNewRank(rank, message -> {
                clearData();
                successMessage.setValue(message);
            }, error -> errorMessage.setValue(error));

        } catch (NumberFormatException e) {
            errorMessage.setValue("A ponthatár szám legyen");
        } catch (Exception e){
            errorMessage.setValue("Hiba a feltöltés során");
        }
    }

    private void loadRankData(String rankId) {
        rankRepository.loadRankData(
                rankId,
                rank -> {
                    rankData.setValue(rank);
                });
    }

   public void deleteRank() {
        if (rankId.getValue() == null) {
            errorMessage.setValue("Nincs törlendő rang");
            return;
        }

        rankRepository.deleteRank(
                rankId.getValue(),
                message -> successMessage.setValue(message),
                error -> errorMessage.setValue(error)
        );
    }


    private void clearData() {
        rankName.setValue("");
        threshold.setValue("");
    }
}
