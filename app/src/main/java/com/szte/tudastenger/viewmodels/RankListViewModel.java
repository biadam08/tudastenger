package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.Rank;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.RankRepository;
import com.szte.tudastenger.repositories.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RankListViewModel extends AndroidViewModel {
    private final RankRepository rankRepository;
    private final UserRepository userRepository;

    private MutableLiveData<List<Rank>> ranksData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isAdmin = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public RankListViewModel(Application application, RankRepository rankRepository, UserRepository userRepository) {
        super(application);
        this.rankRepository = rankRepository;
        this.userRepository = userRepository;
    }

    public LiveData<List<Rank>> getRanksData() { return ranksData; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void checkAdmin() {
        userRepository.checkAdmin(isAdminStatus -> isAdmin.setValue(isAdminStatus), error -> errorMessage.setValue(error));
    }

    public void loadCategories() {
        rankRepository.loadRanks(ranks -> ranksData.setValue(ranks), error -> errorMessage.setValue(error));
    }
}
