package com.notes.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.ink.persistence.InkRepository
import com.notes.ink.persistence.NotebookEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotebookListScreen(
    repository: InkRepository,
    onOpenNotebook: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val notebooks by repository.getAllNotebooks().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var notebookToRename by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookToDelete by remember { mutableStateOf<NotebookEntity?>(null) }
    var dialogTitleInput by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Notebook") },
            text = {
                OutlinedTextField(
                    value = dialogTitleInput,
                    onValueChange = { dialogTitleInput = it },
                    label = { Text("Notebook Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = dialogTitleInput.trim().ifEmpty { "Untitled Notebook" }
                        showCreateDialog = false
                        dialogTitleInput = ""
                        coroutineScope.launch {
                            val nb = repository.createNotebook(title = title)
                            onOpenNotebook(nb.id)
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (notebookToRename != null) {
        AlertDialog(
            onDismissRequest = { notebookToRename = null },
            title = { Text("Rename Notebook") },
            text = {
                OutlinedTextField(
                    value = dialogTitleInput,
                    onValueChange = { dialogTitleInput = it },
                    label = { Text("New Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = dialogTitleInput.trim()
                        val target = notebookToRename
                        notebookToRename = null
                        if (target != null && newTitle.isNotEmpty()) {
                            coroutineScope.launch {
                                repository.renameNotebook(target.id, newTitle)
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (notebookToDelete != null) {
        AlertDialog(
            onDismissRequest = { notebookToDelete = null },
            title = { Text("Delete Notebook") },
            text = { Text("Are you sure you want to delete '${notebookToDelete?.title}' and all its pages? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = notebookToDelete
                        notebookToDelete = null
                        if (target != null) {
                            coroutineScope.launch {
                                repository.deleteNotebook(target.id)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    dialogTitleInput = ""
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Notebook")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "My Notebooks",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (notebooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notebooks yet. Tap + to create one!", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notebooks, key = { it.id }) { notebook ->
                        NotebookCard(
                            notebook = notebook,
                            onClick = { onOpenNotebook(notebook.id) },
                            onRename = {
                                dialogTitleInput = notebook.title
                                notebookToRename = notebook
                            },
                            onDelete = {
                                notebookToDelete = notebook
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotebookCard(
    notebook: NotebookEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(notebook.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(notebook.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Placeholder thumbnail cover
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📓", fontSize = 48.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = notebook.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Edited $dateStr",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
                }
            }
        }
    }
}
