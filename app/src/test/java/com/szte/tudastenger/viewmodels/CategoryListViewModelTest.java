package com.szte.tudastenger.viewmodels;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.UserRepository;
import com.szte.tudastenger.repositories.QuestionRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CategoryListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Application application;

    @Mock
    private Observer<List<Category>> categoriesDataObserver;

    @Mock
    private Observer<Boolean> isAdminObserver;

    @Mock
    private Observer<String> errorMessageObserver;

    private CategoryListViewModel viewModel;

    @Before
    public void setup() {
        categoryRepository = mock(CategoryRepository.class);
        userRepository = mock(UserRepository.class);
        viewModel = new CategoryListViewModel(application, categoryRepository, userRepository);

        viewModel.getCategoriesData().observeForever(categoriesDataObserver);
        viewModel.getIsAdmin().observeForever(isAdminObserver);
        viewModel.getErrorMessage().observeForever(errorMessageObserver);
    }

    @Test
    public void testCheckAdminIfAdmin() {
        doAnswer(invocation -> {
            UserRepository.AdminStatusCallback callback = invocation.getArgument(0);
            callback.onCheckAdmin(true);
            return null;
        }).when(userRepository).checkAdmin(any(), any());

        viewModel.checkAdmin();

        verify(isAdminObserver).onChanged(true);
    }

    @Test
    public void testCheckAdminIfNotAdmin() {
        doAnswer(invocation -> {
            UserRepository.AdminStatusCallback callback = invocation.getArgument(0);
            callback.onCheckAdmin(false);
            return null;
        }).when(userRepository).checkAdmin(any(), any());

        viewModel.checkAdmin();

        verify(isAdminObserver).onChanged(false);
    }

    @Test
    public void testCheckAdminError() {
        String errorMessage = "Error during check";

        doAnswer(invocation -> {
            UserRepository.ErrorCallback callback = invocation.getArgument(1);
            callback.onError(errorMessage);
            return null;
        }).when(userRepository).checkAdmin(any(), any());

        viewModel.checkAdmin();

        verify(errorMessageObserver).onChanged(errorMessage);
    }

    @Test
    public void testLoadCategoriesSuccess() {
        List<Category> testCategories = Arrays.asList(
                new Category("1", "Test 1", "image1.jpg"),
                new Category("2", "Test 2", "image2.jpg")
        );

        doAnswer(invocation -> {
            CategoryRepository.CategoryLoadedCallback callback = invocation.getArgument(0);
            callback.onCategoriesLoaded(testCategories);
            return null;
        }).when(categoryRepository).loadCategories(any(), any());

        viewModel.loadCategories();

        verify(categoriesDataObserver).onChanged(testCategories);
        assertThat(viewModel.getCategoriesData().getValue()).isEqualTo(testCategories);
    }

    @Test
    public void testLoadCategoriesError() {
        String errorMessage = "Error loading categories";

        doAnswer(invocation -> {
            QuestionRepository.ErrorCallback callback = invocation.getArgument(1);
            callback.onError(errorMessage);
            return null;
        }).when(categoryRepository).loadCategories(any(), any());

        viewModel.loadCategories();

        verify(errorMessageObserver).onChanged(errorMessage);
    }

    @Test
    public void testLoadCategoriesReturnsEmptyList() {
        List<Category> emptyList = Arrays.asList();

        doAnswer(invocation -> {
            CategoryRepository.CategoryLoadedCallback callback = invocation.getArgument(0);
            callback.onCategoriesLoaded(emptyList);
            return null;
        }).when(categoryRepository).loadCategories(any(), any());

        viewModel.loadCategories();

        verify(categoriesDataObserver).onChanged(emptyList);
        assertThat(viewModel.getCategoriesData().getValue()).isEmpty();
    }
}