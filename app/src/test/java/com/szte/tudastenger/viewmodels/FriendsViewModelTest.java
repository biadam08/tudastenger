package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.FriendRepository;
import com.szte.tudastenger.repositories.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FriendsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Application application;

    @Mock
    private Observer<User> currentUserObserver;

    @Mock
    private Observer<ArrayList<User>> friendsDataObserver;

    @Mock
    private Observer<ArrayList<User>> friendRequestsDataObserver;

    @Mock
    private Observer<ArrayList<User>> usersDataObserver;

    @Mock
    private Observer<Boolean> noFriendsVisibilityObserver;

    @Mock
    private Observer<Boolean> noRequestsVisibilityObserver;

    @Mock
    private Observer<Map<String, Boolean>> friendButtonStatesObserver;

    @Mock
    private Observer<User> friendRequestSentObserver;

    private FriendsViewModel viewModel;
    private User testCurrentUser;

    @Before
    public void setup() {
        friendRepository = mock(FriendRepository.class);
        userRepository = mock(UserRepository.class);

        testCurrentUser = new User("1", "Current User", "test@gmail.com", null);

        doAnswer(invocation -> {
            UserRepository.UserLoadedCallback callback = invocation.getArgument(0);
            callback.onUserLoaded(testCurrentUser);
            return null;
        }).when(userRepository).loadCurrentUser(any());

        viewModel = new FriendsViewModel(application, userRepository, friendRepository);

        viewModel.getCurrentUser().observeForever(currentUserObserver);
        viewModel.getFriendsData().observeForever(friendsDataObserver);
        viewModel.getFriendRequestsData().observeForever(friendRequestsDataObserver);
        viewModel.getUsersData().observeForever(usersDataObserver);
        viewModel.getNoFriendsVisibility().observeForever(noFriendsVisibilityObserver);
        viewModel.getNoRequestsVisibility().observeForever(noRequestsVisibilityObserver);
        viewModel.getFriendButtonStates().observeForever(friendButtonStatesObserver);
        viewModel.getFriendRequestSent().observeForever(friendRequestSentObserver);
    }

    @Test
    public void testLoadUsers() {
        User otherUser = new User("2", "Other User", "test@gmail.com", null);

        List<User> allUsers = new ArrayList<>();
        allUsers.add(testCurrentUser);
        allUsers.add(otherUser);

        doAnswer(invocation -> {
            UserRepository.UsersLoadedCallback callback = invocation.getArgument(0);
            callback.onUsersLoaded(allUsers);
            return null;
        }).when(userRepository).loadUsers(any());

        viewModel.loadUsers();

        assertThat(viewModel.getUsersData().getValue()).doesNotContain(testCurrentUser);
        assertThat(viewModel.getUsersData().getValue()).contains(otherUser);
    }

    @Test
    public void testQueryDataWithFriendsAndRequests() {
        User friend = new User("2", "Friend User", "test@gmail.com", null);
        User requestUser = new User("3", "Request User", "test2@gmail.com", null);

        doAnswer(invocation -> {
            FriendRepository.FriendLoadedCallback callback = invocation.getArgument(1);
            callback.onFriendLoaded(friend);
            return null;
        }).when(friendRepository).queryFriends(eq(testCurrentUser.getId()), any(), any());

        doAnswer(invocation -> {
            FriendRepository.RequestLoadedCallback callback = invocation.getArgument(1);
            callback.onRequestLoaded(requestUser);
            return null;
        }).when(friendRepository).queryFriendRequests(eq(testCurrentUser.getId()), any(), any());

        viewModel.queryData();

        verify(friendRepository, Mockito.atLeastOnce()).queryFriends(eq(testCurrentUser.getId()), any(), any());
        verify(friendRepository, Mockito.atLeastOnce()).queryFriendRequests(eq(testCurrentUser.getId()), any(), any());

        assertThat(viewModel.getFriendsData().getValue()).contains(friend);
        assertThat(viewModel.getFriendRequestsData().getValue()).contains(requestUser);
        verify(noFriendsVisibilityObserver, Mockito.atLeastOnce()).onChanged(false);
        verify(noRequestsVisibilityObserver, Mockito.atLeastOnce()).onChanged(false);
    }

    @Test
    public void testQueryDataWithNoFriendsAndNoRequests() {
        doAnswer(invocation -> {
            FriendRepository.NoFriendsCallback callback = invocation.getArgument(2);
            callback.onNoFriends();
            return null;
        }).when(friendRepository).queryFriends(eq(testCurrentUser.getId()), any(), any());

        doAnswer(invocation -> {
            FriendRepository.NoRequestsCallback callback = invocation.getArgument(2);
            callback.onNoRequests();
            return null;
        }).when(friendRepository).queryFriendRequests(eq(testCurrentUser.getId()), any(), any());

        viewModel.queryData();

        verify(friendRepository, Mockito.atLeastOnce()).queryFriends(eq(testCurrentUser.getId()), any(), any());
        verify(friendRepository, Mockito.atLeastOnce()).queryFriendRequests(eq(testCurrentUser.getId()), any(), any());

        verify(noFriendsVisibilityObserver, Mockito.atLeastOnce()).onChanged(true);
        verify(noRequestsVisibilityObserver, Mockito.atLeastOnce()).onChanged(true);
    }


    @Test
    public void testQueryFriendsEmpty() {
        doAnswer(invocation -> {
            FriendRepository.NoFriendsCallback callback = invocation.getArgument(2);
            callback.onNoFriends();
            return null;
        }).when(friendRepository).queryFriends(any(), any(), any());

        viewModel.queryData();

        verify(noFriendsVisibilityObserver).onChanged(true);
    }

    @Test
    public void testDeleteFriend() {
        User friendToDelete = new User("2", "Friend User", "test@gmail.com", null);

        ArrayList<User> currentFriends = new ArrayList<>();
        currentFriends.add(friendToDelete);
        viewModel.getFriendsData().getValue().add(friendToDelete);

        doAnswer(invocation -> {
            FriendRepository.SuccessCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(friendRepository).deleteFriend(any(), any(), any());

        viewModel.deleteFriend(friendToDelete);

        assertThat(viewModel.getFriendsData().getValue()).doesNotContain(friendToDelete);
        verify(noFriendsVisibilityObserver).onChanged(true);
    }

    @Test
    public void testCheckFriendship() {
        String targetUserId = "2";
        boolean canSendRequest = true;

        doAnswer(invocation -> {
            FriendRepository.FriendshipStatusCallback callback = invocation.getArgument(2);
            callback.onStatusChecked(targetUserId, canSendRequest);
            return null;
        }).when(friendRepository).checkFriendshipAndFriendRequest(any(), any(), any());

        viewModel.checkFriendshipAndFriendRequest(testCurrentUser.getId(), targetUserId);

        Map<String, Boolean> expectedStates = new HashMap<>();
        expectedStates.put(targetUserId, canSendRequest);
        assertThat(viewModel.getFriendButtonStates().getValue()).containsExactlyEntriesIn(expectedStates);
    }

    @Test
    public void testSendFriendRequest() {
        User targetUser = new User("2", "Target User", "test@gmail.com", null);

        doAnswer(invocation -> {
            FriendRepository.RequestSentCallback callback = invocation.getArgument(2);
            callback.onRequestSent(targetUser);
            return null;
        }).when(friendRepository).sendFriendRequest(any(), any(), any());

        viewModel.sendFriendRequest(testCurrentUser.getId(), targetUser);

        assertThat(viewModel.getFriendRequestSent().getValue()).isEqualTo(targetUser);
        assertThat(viewModel.getFriendButtonStates().getValue().get(targetUser.getId())).isFalse();
    }

    @Test
    public void testApproveFriendRequest() {
        User requestUser = new User("2", "Request User", "test@gmail.com", null);

        doAnswer(invocation -> {
            FriendRepository.SuccessCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(friendRepository).approveFriendRequest(any(), any(), any());

        viewModel.approveFriendRequest(requestUser);

        assertThat(viewModel.getFriendsData().getValue()).contains(requestUser);
        assertThat(viewModel.getFriendRequestsData().getValue()).doesNotContain(requestUser);
    }

    @Test
    public void testCheckCanApproveRequest() {
        String userId = "2";
        MutableLiveData<Boolean> canApproveLiveData = new MutableLiveData<>();

        when(friendRepository.canApprove(any(), any())).thenReturn(canApproveLiveData);

        LiveData<Boolean> result = viewModel.checkCanApproveRequest(userId, testCurrentUser.getId());
        canApproveLiveData.setValue(true);

        assertThat(result.getValue()).isTrue();
        verify(friendRepository).canApprove(eq(userId), eq(testCurrentUser.getId()));
    }

    @Test
    public void testDeclineFriendRequest() {
        User requestUser = new User("2", "Request User", "test@gmail.com", null);
        ArrayList<User> currentRequests = new ArrayList<>();
        currentRequests.add(requestUser);
        viewModel.getFriendRequestsData().getValue().add(requestUser);

        doAnswer(invocation -> {
            FriendRepository.SuccessCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(friendRepository).declineFriendRequest(any(), any(), any());

        viewModel.declineFriendRequest(requestUser);

        verify(friendRepository).declineFriendRequest(
                eq(testCurrentUser.getId()),
                eq(requestUser.getId()),
                any()
        );
        assertThat(viewModel.getFriendRequestsData().getValue()).doesNotContain(requestUser);
        verify(noRequestsVisibilityObserver, Mockito.atLeastOnce()).onChanged(true);
    }

    @Test
    public void testOnFriendRequestAdded() {
        User newRequest = new User("2", "New Request", "test@gmail.com", null);

        viewModel.onFriendRequestAdded(newRequest);

        assertThat(viewModel.getFriendRequestsData().getValue()).contains(newRequest);
        verify(noRequestsVisibilityObserver, Mockito.atLeastOnce()).onChanged(false);
    }

    @Test
    public void testOnFriendRemoved() {
        User friendToRemove = new User("2", "Friend", "test@gmail.com", null);
        ArrayList<User> currentFriends = new ArrayList<>();
        currentFriends.add(friendToRemove);
        viewModel.getFriendsData().getValue().add(friendToRemove);

        viewModel.onFriendRemoved(friendToRemove);

        assertThat(viewModel.getFriendsData().getValue()).doesNotContain(friendToRemove);
        verify(noFriendsVisibilityObserver, Mockito.atLeastOnce()).onChanged(true);
    }

    @Test
    public void testOnFriendRemovedWithRemainingFriends() {
        User friendToRemove = new User("2", "Friend", "test@gmail.com", null);
        User remainingFriend = new User("3", "Remaining", "test2@gmail.com", null);

        viewModel.getFriendsData().getValue().add(friendToRemove);
        viewModel.getFriendsData().getValue().add(remainingFriend);

        viewModel.onFriendRemoved(friendToRemove);

        assertThat(viewModel.getFriendsData().getValue())
                .containsExactly(remainingFriend);
        verify(noFriendsVisibilityObserver, Mockito.atLeastOnce()).onChanged(false);
    }
}