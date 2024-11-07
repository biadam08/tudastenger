package com.szte.tudastenger.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.szte.tudastenger.models.Category;
import com.szte.tudastenger.models.User;
import com.szte.tudastenger.repositories.CategoryRepository;
import com.szte.tudastenger.repositories.UserRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CategoryEditUploadViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Application application;

    @Mock
    private Observer<String> errorMessageObserver;

    @Mock
    private Observer<String> successMessageObserver;

    @Mock
    private Observer<Boolean> isAdminObserver;

    @Mock
    private Observer<Category> categoryDataObserver;

    @Mock
    private Observer<Integer> uploadProgressObserver;

    @Mock
    private Observer<Boolean> isImageUploadingObserver;

    private CategoryEditUploadViewModel viewModel;

    @Before
    public void setup() {
        categoryRepository = mock(CategoryRepository.class);
        userRepository = mock(UserRepository.class);
        viewModel = new CategoryEditUploadViewModel(application, categoryRepository, userRepository);

        viewModel.getErrorMessage().observeForever(errorMessageObserver);
        viewModel.getSuccessMessage().observeForever(successMessageObserver);
        viewModel.getIsAdmin().observeForever(isAdminObserver);
        viewModel.getCategoryData().observeForever(categoryDataObserver);
        viewModel.getUploadProgress().observeForever(uploadProgressObserver);
        viewModel.getIsImageUploading().observeForever(isImageUploadingObserver);
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
    public void testCheckAdminSetsIsAdminTrue() {
        doAnswer(invocation -> {
            UserRepository.AdminStatusCallback callback = invocation.getArgument(0);
            callback.onCheckAdmin(true);
            return null;
        }).when(userRepository).checkAdmin(any(), any());

        viewModel.checkAdmin();

        Boolean isAdminStatus = viewModel.getIsAdmin().getValue();
        assertThat(isAdminStatus).isTrue();
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
    public void testUploadCategoryWithEmptyName() {
        viewModel.uploadCategory("");

        verify(errorMessageObserver).onChanged("A kategória neve nem lehet üres");
        verify(categoryRepository, never()).uploadImage(any(), any(), any(), any());
        verify(categoryRepository, never()).addNewCategory(any(), any(), any());
    }

    @Test
    public void testUploadCategoryWithNameAndImage() {
        String categoryName = "Test category";
        Uri mockUri = mock(Uri.class);
        viewModel.setImageUri(mockUri);

        doAnswer(invocation -> {
            CategoryRepository.ProgressCallback progressCallback = invocation.getArgument(3);
            progressCallback.onProgress(50);
            CategoryRepository.SuccessCallback callback = invocation.getArgument(1);
            callback.onSuccess("test-image.jpg");
            return null;
        }).when(categoryRepository).uploadImage(any(), any(), any(), any());

        doAnswer(invocation -> {
            Category uploadedCategory = invocation.getArgument(0);
            assertThat(uploadedCategory.getName()).isEqualTo(categoryName);
            assertThat(uploadedCategory.getImage()).isEqualTo("test-image.jpg");

            CategoryRepository.SuccessCallback callback = invocation.getArgument(1);
            callback.onSuccess("Successfully added");
            return null;
        }).when(categoryRepository).addNewCategory(any(), any(), any());

        viewModel.uploadCategory(categoryName);

        verify(uploadProgressObserver).onChanged(50);
        verify(isImageUploadingObserver).onChanged(true);
        verify(successMessageObserver).onChanged("Successfully added");
    }

    @Test
    public void testInitLoadsCategory() {
        String categoryId = "001";
        Category testCategory = new Category(categoryId, "Test category", "test-image.jpg");
        String imageUrl = "https://example.com/test-image.jpg";

        doAnswer(invocation -> {
            CategoryRepository.CategoryReceivedCallback categoryCallback = invocation.getArgument(1);
            categoryCallback.onCategoryReceived(testCategory);

            CategoryRepository.ImageUrlCallback imageCallback = invocation.getArgument(2);
            imageCallback.onImageUrlReceived(imageUrl);
            return null;
        }).when(categoryRepository).loadCategoryData(eq(categoryId), any(), any());

        viewModel.init(categoryId);

        verify(categoryDataObserver).onChanged(testCategory);
        assertThat(viewModel.getCategoryName().getValue()).isEqualTo(testCategory.getName());
        assertThat(viewModel.getImageUrl().getValue()).isEqualTo(imageUrl);
    }

    @Test
    public void testDeleteCategory() {
        String categoryId = "001";

        doAnswer(invocation -> {
            CategoryRepository.SuccessCallback callback = invocation.getArgument(1);
            callback.onSuccess("Successfully deleted");
            return null;
        }).when(categoryRepository).deleteCategory(eq(categoryId), any(), any());

        viewModel.init(categoryId);
        viewModel.deleteCategory();

        verify(successMessageObserver).onChanged("Successfully deleted");
    }

    @Test
    public void testUpdateCategory() {
        String categoryId = "001";
        String categoryName = "Modified category";

        doAnswer(invocation -> {
            Category updatedCategory = invocation.getArgument(0);
            assertThat(updatedCategory.getId()).isEqualTo(categoryId);
            assertThat(updatedCategory.getName()).isEqualTo(categoryName);

            CategoryRepository.SuccessCallback callback = invocation.getArgument(1);
            callback.onSuccess("Successfully modified");
            return null;
        }).when(categoryRepository).updateCategory(any(), any(), any());

        viewModel.init(categoryId);
        viewModel.uploadCategory(categoryName);

        verify(successMessageObserver).onChanged("Successfully modified");
    }

    @Test
    public void testUploadImageError() {
        Uri mockUri = mock(Uri.class);
        String errorMessage = "Upload failed";
        viewModel.setImageUri(mockUri);

        doAnswer(invocation -> {
            CategoryRepository.ErrorCallback callback = invocation.getArgument(2);
            callback.onError(errorMessage);
            return null;
        }).when(categoryRepository).uploadImage(any(), any(), any(), any());

        viewModel.uploadCategory("Test");

        verify(errorMessageObserver).onChanged(errorMessage);
        verify(isImageUploadingObserver).onChanged(false);
    }

    @Test
    public void testClearImage() {
        Uri testUri = mock(Uri.class);
        viewModel.setImageUri(testUri);

        viewModel.clearImage();

        assertThat(viewModel.getImageUri().getValue()).isNull();
        assertThat(viewModel.getImageUrl().getValue()).isNull();
    }
}