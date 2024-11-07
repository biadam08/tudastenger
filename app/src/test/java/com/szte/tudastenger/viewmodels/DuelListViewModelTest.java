package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.google.firebase.Timestamp;
import com.szte.tudastenger.models.Duel;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.DuelRepository;
import com.szte.tudastenger.repositories.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DuelListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private DuelRepository duelRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Application application;

    @Mock
    private Observer<User> currentUserObserver;

    @Mock
    private Observer<List<Duel>> pendingDuelsObserver;

    @Mock
    private Observer<List<Duel>> finishedDuelsObserver;

    @Mock
    private Observer<String> categoryAndQuestionNumberObserver;

    private DuelListViewModel viewModel;
    private User testCurrentUser;

    @Before
    public void setup() {
        testCurrentUser = new User("1", "Current User", "test@gmail.com", null);

        List<Duel> testPendingDuels = new ArrayList<>();
        testPendingDuels.add(createTestDuel("duel1", false));
        testPendingDuels.add(createTestDuel("duel2", false));

        List<Duel> testFinishedDuels = new ArrayList<>();
        testFinishedDuels.add(createTestDuel("duel3", true));
        testFinishedDuels.add(createTestDuel("duel4", true));

        doAnswer(invocation -> {
            UserRepository.UserLoadedCallback callback = invocation.getArgument(0);
            callback.onUserLoaded(testCurrentUser);
            return null;
        }).when(userRepository).loadCurrentUser(any());

        doAnswer(invocation -> {
            DuelRepository.DuelsLoadedCallback callback = invocation.getArgument(1);
            callback.onDuelsLoaded(testPendingDuels);
            return null;
        }).when(duelRepository).loadPendingDuels(eq(testCurrentUser.getId()), any());

        doAnswer(invocation -> {
            DuelRepository.DuelsLoadedCallback callback = invocation.getArgument(1);
            callback.onDuelsLoaded(testFinishedDuels);
            return null;
        }).when(duelRepository).loadFinishedDuels(eq(testCurrentUser.getId()), any());

        viewModel = new DuelListViewModel(application, duelRepository, categoryRepository, userRepository);

        viewModel.getCurrentUser().observeForever(currentUserObserver);
        viewModel.getPendingDuels().observeForever(pendingDuelsObserver);
        viewModel.getFinishedDuels().observeForever(finishedDuelsObserver);
        viewModel.getCategoryAndQuestionNumber().observeForever(categoryAndQuestionNumberObserver);
    }
    private Duel createTestDuel(String id, boolean isFinished) {
        ArrayList<String> questionIds = new ArrayList<>();
        questionIds.add("q1");
        questionIds.add("q2");

        ArrayList<Boolean> challengerResults = new ArrayList<>();
        challengerResults.add(true);
        challengerResults.add(false);

        ArrayList<Boolean> challengedResults = new ArrayList<>();
        challengedResults.add(false);
        challengedResults.add(true);

        Duel duel = new Duel(id, "1", "2", "categoryId", questionIds, challengerResults, challengedResults);
        duel.setFinished(isFinished);
        return duel;
    }

    @Test
    public void testLoadUser() {
        verify(currentUserObserver).onChanged(testCurrentUser);
        assertThat(viewModel.getCurrentUser().getValue()).isEqualTo(testCurrentUser);
    }

    @Test
    public void testGetUsernames() {
        Duel testDuel = createTestDuel("duel1", false);
        String expectedUsernames = "User1 - User2";

        doAnswer(invocation -> {
            UserRepository.UsernamesLoadedCallback callback = invocation.getArgument(2);
            callback.onUsernamesLoaded(expectedUsernames);
            return null;
        }).when(userRepository).getUsernamesForDuel(any(), any(), any());

        viewModel.getUsernames(testDuel);

        verify(userRepository).getUsernamesForDuel(
                eq(testDuel.getChallengerUid()),
                eq(testDuel.getChallengedUid()),
                any()
        );
    }

    @Test
    public void testLoadDuelsSuccess() {
        verify(duelRepository, Mockito.atLeastOnce()).loadPendingDuels(eq(testCurrentUser.getId()), any());
        verify(duelRepository, Mockito.atLeastOnce()).loadFinishedDuels(eq(testCurrentUser.getId()), any());

        verify(pendingDuelsObserver).onChanged(argThat(duels ->
                duels.size() == 2 &&
                        duels.get(0).getId().equals("duel1") &&
                        duels.get(1).getId().equals("duel2") &&
                        !duels.get(0).isFinished() &&
                        !duels.get(1).isFinished()
        ));

        verify(finishedDuelsObserver).onChanged(argThat(duels ->
                duels.size() == 2 &&
                        duels.get(0).getId().equals("duel3") &&
                        duels.get(1).getId().equals("duel4") &&
                        duels.get(0).isFinished() &&
                        duels.get(1).isFinished()
        ));

        assertThat(viewModel.getPendingDuels().getValue()).isNotNull();
        List<Duel> pendingDuels = viewModel.getPendingDuels().getValue();
        assertThat(pendingDuels.size()).isEqualTo(2);
        assertThat(pendingDuels.get(0).getId()).isEqualTo("duel1");
        assertThat(pendingDuels.get(1).getId()).isEqualTo("duel2");
        assertThat(pendingDuels.get(0).isFinished()).isFalse();
        assertThat(pendingDuels.get(1).isFinished()).isFalse();

        assertThat(viewModel.getFinishedDuels().getValue()).isNotNull();
        List<Duel> finishedDuels = viewModel.getFinishedDuels().getValue();
        assertThat(finishedDuels.size()).isEqualTo(2);
        assertThat(finishedDuels.get(0).getId()).isEqualTo("duel3");
        assertThat(finishedDuels.get(1).getId()).isEqualTo("duel4");
        assertThat(finishedDuels.get(0).isFinished()).isTrue();
        assertThat(finishedDuels.get(1).isFinished()).isTrue();
    }

    @Test
    public void testLoadCategoryAndQuestionNumber() {
        String categoryId = "category1";
        int questionCount = 5;
        Category testCategory = new Category(categoryId, "Test Category", "image.jpg");
        String expectedText = "Test Category / 5 db";

        doAnswer(invocation -> {
            CategoryRepository.CategoryReceivedCallback callback = invocation.getArgument(1);
            callback.onCategoryReceived(testCategory);
            return null;
        }).when(categoryRepository).loadCategoryData(anyString(), any(), any());

        viewModel.loadCategoryAndQuestionNumber(categoryId, questionCount);

        verify(categoryRepository).loadCategoryData(eq(categoryId), any(), any());

        verify(categoryAndQuestionNumberObserver).onChanged(expectedText);
        assertThat(viewModel.getCategoryAndQuestionNumber().getValue()).isEqualTo(expectedText);
    }
}