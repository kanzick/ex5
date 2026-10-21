package org.example.ex5

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppScreen {
    CourseList,
    CourseDetail,
    CourseForm,
    CategoryManager
}

data class CourseDraft(
    val id: String = "",
    var title: String = "",
    var description: String = "",
    var categoryId: String = "",
    var instructor: String = "",
    var price: String = "",
    var duration: String = "",
    var thumbnail: String = "",
    var trailerUrl: String = "",
    var createdAt: String = ""
) {
    fun toCourseRequest(): CourseRequest {
        val cleanedPrice = price.toDoubleOrNull() ?: 0.0
        val cleanedDuration = duration.toIntOrNull() ?: 0
        val createdDate = createdAt.ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        return CourseRequest(
            title = title.trim(),
            description = description.trim(),
            categoryId = categoryId,
            instructor = instructor.trim(),
            price = cleanedPrice.coerceAtLeast(0.0),
            duration = cleanedDuration.coerceAtLeast(0),
            thumbnail = thumbnail.trim(),
            trailerUrl = trailerUrl.trim(),
            createdAt = createdDate
        )
    }

    companion object {
        fun from(course: Course): CourseDraft = CourseDraft(
            id = course.id,
            title = course.title,
            description = course.description,
            categoryId = course.categoryId,
            instructor = course.instructor,
            price = course.price.toString(),
            duration = course.duration.toString(),
            thumbnail = course.thumbnail,
            trailerUrl = course.trailerUrl,
            createdAt = course.createdAt
        )
    }
}

@Composable
fun CourseManagementApp() {
    val repository = remember { MockApiService() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var screen by remember { mutableStateOf(AppScreen.CourseList) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var editingCourse by remember { mutableStateOf<CourseDraft?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCourse by remember { mutableStateOf<Course?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<Category?>(null) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                categories = repository.getCategories()
                courses = repository.getCourses()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load data. Check your MockAPI URL."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val categoryById = categories.associateBy { it.id }

    val navigateToList: () -> Unit = {
        selectedCourse = null
        editingCourse = null
        screen = AppScreen.CourseList
    }

    when (screen) {
        AppScreen.CourseList -> {
            CourseListScreen(
                categories = categories,
                courses = courses,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onRefresh = { loadData() },
                onOpenDetail = { course ->
                    selectedCourse = course
                    screen = AppScreen.CourseDetail
                },
                onAddCourse = {
                    editingCourse = CourseDraft(categoryId = categories.firstOrNull()?.id ?: "")
                    screen = AppScreen.CourseForm
                },
                onManageCategories = {
                    screen = AppScreen.CategoryManager
                },
                onDeleteCourse = { course -> pendingDeleteCourse = course }
            )
        }

        AppScreen.CourseDetail -> {
            val course = selectedCourse
            if (course != null) {
                CourseDetailScreen(
                    course = course,
                    categoryName = categoryById[course.categoryId]?.name ?: "Unknown",
                    onBack = { navigateToList() },
                    onEdit = {
                        editingCourse = CourseDraft.from(course)
                        screen = AppScreen.CourseForm
                    },
                    onDelete = { pendingDeleteCourse = course },
                    onOpenTrailer = { trailerUrl ->
                        if (trailerUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "No app available to play the trailer.", Toast.LENGTH_SHORT).show() }
                        } else {
                            Toast.makeText(context, "No trailer URL available.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                navigateToList()
            }
        }

        AppScreen.CourseForm -> {
            CourseFormScreen(
                categories = categories,
                initialDraft = editingCourse ?: CourseDraft(categoryId = categories.firstOrNull()?.id ?: ""),
                onSave = { draft ->
                    coroutineScope.launch {
                        try {
                            if (draft.id.isBlank()) {
                                repository.createCourse(draft.toCourseRequest())
                            } else {
                                repository.updateCourse(draft.id, draft.toCourseRequest())
                            }
                            loadData()
                            screen = AppScreen.CourseList
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Unable to save course.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onCancel = { navigateToList() }
            )
        }

        AppScreen.CategoryManager -> {
            CategoryManagerScreen(
                categories = categories,
                courses = courses,
                onBack = { navigateToList() },
                onAddCategory = {
                    editingCategory = null
                    // no separate screen; use inline form below in the same screen.
                },
                onEditCategory = { category ->
                    editingCategory = category
                },
                onDeleteCategory = { category -> pendingDeleteCategory = category },
                editingCategory = editingCategory,
                onSaveCategory = { categoryId, name, description ->
                    val trimmedName = name.trim()
                    val trimmedDescription = description.trim()
                    if (trimmedName.isBlank()) {
                        Toast.makeText(context, "Category name is required.", Toast.LENGTH_SHORT).show()
                        return@CategoryManagerScreen
                    }
                    coroutineScope.launch {
                        try {
                            if (categoryId.isBlank()) {
                                repository.createCategory(CategoryRequest(trimmedName, trimmedDescription))
                            } else {
                                repository.updateCategory(categoryId, CategoryRequest(trimmedName, trimmedDescription))
                            }
                            editingCategory = null
                            loadData()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Unable to save category.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onCancelCategory = {
                    editingCategory = null
                }
            )
        }
    }

    pendingDeleteCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCourse = null },
            title = { Text("Delete course") },
            text = { Text("Delete '${course.title}' from the backend?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            repository.deleteCourse(course.id)
                            pendingDeleteCourse = null
                            loadData()
                            navigateToList()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Unable to delete course.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCourse = null }) { Text("Cancel") }
            }
        )
    }

    pendingDeleteCategory?.let { category ->
        val courseCount = courses.count { it.categoryId == category.id }
        if (courseCount > 0) {
            AlertDialog(
                onDismissRequest = { pendingDeleteCategory = null },
                title = { Text("Cannot delete category") },
                text = { Text("This category is still used by ${courseCount} course(s).") },
                confirmButton = {
                    TextButton(onClick = { pendingDeleteCategory = null }) { Text("OK") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingDeleteCategory = null },
                title = { Text("Delete category") },
                text = { Text("Delete '${category.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                repository.deleteCategory(category.id)
                                pendingDeleteCategory = null
                                loadData()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Unable to delete category.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteCategory = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseListScreen(
    categories: List<Category>,
    courses: List<Course>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onOpenDetail: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onManageCategories: () -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val visibleCourses = courses.filter { course ->
        val matchesQuery = query.isBlank() || course.title.contains(query, ignoreCase = true)
        val matchesCategory = selectedCategoryId == null || course.categoryId == selectedCategoryId
        matchesQuery && matchesCategory
    }

    val categoryNames = categories.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Manager") },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Manage categories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCourse) {
                Icon(Icons.Default.Add, contentDescription = "Add course")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F7FB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search course") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryId?.let { categoryNames[it]?.name } ?: "All categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All categories") },
                            onClick = {
                                selectedCategoryId = null
                                categoryMenuExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) { Text("Retry") }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (visibleCourses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching courses found.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleCourses) { course ->
                            CourseListItem(
                                course = course,
                                categoryName = categoryNames[course.categoryId]?.name ?: "Unknown",
                                onOpen = { onOpenDetail(course) },
                                onDelete = { onDeleteCourse(course) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseListItem(
    course: Course,
    categoryName: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = course.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(course.title, fontWeight = FontWeight.Bold)
                Text(course.instructor, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(categoryName, color = MaterialTheme.colorScheme.primary)
                Text(formatCurrency(course.price), fontWeight = FontWeight.SemiBold)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete course")
            }
        }
    }
}

@Composable
private fun CourseDetailScreen(
    course: Course,
    categoryName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenTrailer: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(course.title) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F7FB))
        ) {
            item {
                AsyncImage(
                    model = course.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Instructor: ${course.instructor}")
                    Text("Category: $categoryName")
                    Text("Duration: ${course.duration} hours")
                    Text("Price: ${formatCurrency(course.price)}")
                    Text("Created: ${course.createdAt}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(course.description)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onOpenTrailer(course.trailerUrl) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Watch trailer")
                        }
                        OutlinedButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Edit")
                        }
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseFormScreen(
    categories: List<Category>,
    initialDraft: CourseDraft,
    onSave: (CourseDraft) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(initialDraft.id, initialDraft.title) { mutableStateOf(initialDraft) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val validCategory = categories.firstOrNull { it.id == draft.categoryId } ?: categories.firstOrNull()
    if (draft.categoryId.isBlank() && validCategory != null) {
        draft = draft.copy(categoryId = validCategory.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(if (draft.id.isBlank()) "Add course" else "Edit course") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(value = draft.title, onValueChange = { draft = draft.copy(title = it) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = draft.description, onValueChange = { draft = draft.copy(description = it) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = draft.instructor, onValueChange = { draft = draft.copy(instructor = it) }, label = { Text("Instructor") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == draft.categoryId }?.name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    draft = draft.copy(categoryId = category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = draft.price, onValueChange = { draft = draft.copy(price = it) }, label = { Text("Price (VND)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = draft.duration, onValueChange = { draft = draft.copy(duration = it) }, label = { Text("Duration (hours)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = draft.thumbnail, onValueChange = { draft = draft.copy(thumbnail = it) }, label = { Text("Thumbnail URL") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = draft.trailerUrl, onValueChange = { draft = draft.copy(trailerUrl = it) }, label = { Text("Trailer URL") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val validationError = validateCourseDraft(draft)
                            if (validationError != null) {
                                Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            onSave(draft)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (draft.id.isBlank()) "Save course" else "Update course")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryManagerScreen(
    categories: List<Category>,
    courses: List<Course>,
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    editingCategory: Category?,
    onSaveCategory: (String, String, String) -> Unit,
    onCancelCategory: () -> Unit
) {
    val context = LocalContext.current
    var categoryName by remember(editingCategory?.id, editingCategory?.name) { mutableStateOf(editingCategory?.name ?: "") }
    var categoryDescription by remember(editingCategory?.id, editingCategory?.description) { mutableStateOf(editingCategory?.description ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Categories") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (editingCategory != null || categoryName.isNotBlank() || categoryDescription.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Category name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = categoryDescription,
                            onValueChange = { categoryDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                if (categoryName.trim().isBlank()) {
                                    Toast.makeText(context, "Category name is required.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onSaveCategory(editingCategory?.id ?: "", categoryName, categoryDescription)
                                categoryName = ""
                                categoryDescription = ""
                            }, modifier = Modifier.weight(1f)) {
                                Text(if (editingCategory != null) "Update" else "Add")
                            }
                            OutlinedButton(onClick = {
                                categoryName = ""
                                categoryDescription = ""
                                onCancelCategory()
                            }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Button(
                    onClick = { onAddCategory() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add category")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (categories.isEmpty()) {
                Text("No categories available.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories) { category ->
                        val usageCount = courses.count { it.categoryId == category.id }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.name, fontWeight = FontWeight.Bold)
                                    Text(category.description.ifBlank { "No description" })
                                    Text("Used by $usageCount course(s)")
                                }
                                Row {
                                    IconButton(onClick = { onEditCategory(category) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit category")
                                    }
                                    IconButton(onClick = { onDeleteCategory(category) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete category")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(value.toLong())} VNĐ"
}

private fun validateCourseDraft(draft: CourseDraft): String? {
    if (draft.title.isBlank()) return "Course title is required."
    if (draft.description.isBlank()) return "Course description is required."
    if (draft.instructor.isBlank()) return "Instructor name is required."
    if (draft.categoryId.isBlank()) return "Please select a valid category."
    if (draft.price.toDoubleOrNull() == null || draft.price.toDouble() < 0) return "Price must be a non-negative number."
    if (draft.duration.toIntOrNull() == null || draft.duration.toInt() <= 0) return "Duration must be a positive number."
    if (draft.thumbnail.isBlank()) return "Thumbnail URL is required."
    if (draft.trailerUrl.isBlank()) return "Trailer URL is required."
    return null
}
