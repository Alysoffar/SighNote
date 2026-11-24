package com.notes.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notes.app.ui.NotebookListScreen
import com.notes.ink.pdf.PdfManager
import com.notes.ink.persistence.InkRepository
import com.notes.ink.persistence.NotesDatabase
import com.notes.ink.ui.InkCanvasComposable
import com.notes.ink.ui.InkCanvasViewModel
import com.notes.ink.ui.InkToolbar
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesAppRoot()
                }
            }
        }
    }
}

@Composable
fun NotesAppRoot() {
    val context = LocalContext.current
    val database = remember { NotesDatabase.getInstance(context) }
    val repository = remember { InkRepository(database) }

    var currentNotebookId by remember { mutableStateOf<String?>(null) }
    var inDataCollectionMode by remember { mutableStateOf(false) }

    if (inDataCollectionMode) {
        com.notes.app.ui.DataCollectionScreen(
            onNavigateBack = { inDataCollectionMode = false }
        )
    } else if (currentNotebookId == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            NotebookListScreen(
                repository = repository,
                onOpenNotebook = { notebookId ->
                    currentNotebookId = notebookId
                }
            )

            // Button to jump into handwriting data collection spike (Debug only)
            Button(
                onClick = { inDataCollectionMode = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
            ) {
                Text("Recognition Spike (Phase 2)")
            }
        }
    } else {
        NotebookCanvasScreen(
            notebookId = currentNotebookId!!,
            repository = repository,
            onBackToNotebooks = {
                currentNotebookId = null
            }
        )
    }
}

@Composable
fun NotebookCanvasScreen(
    notebookId: String,
    repository: InkRepository,
    onBackToNotebooks: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val inkViewModel: InkCanvasViewModel = viewModel {
        InkCanvasViewModel(repository)
    }

    val uiState by inkViewModel.uiState.collectAsState()
    var notebookTitle by remember { mutableStateOf("Notebook") }

    // Lifecycle observer to autosave view state and flush saves on pause/stop
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                inkViewModel.saveCurrentViewport()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(notebookId) {
        val nb = repository.getNotebook(notebookId)
        if (nb != null) {
            notebookTitle = nb.title
        }
        val pages = repository.getPagesListForNotebook(notebookId)
        if (pages.isNotEmpty()) {
            inkViewModel.loadPage(pages.first().id)
        } else {
            val newPage = repository.addPage(notebookId)
            inkViewModel.loadPage(newPage.id)
        }
    }

    // PDF Import Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val importedPages = PdfManager.importPdf(context, uri, notebookId, repository)
                    if (importedPages.isNotEmpty()) {
                        inkViewModel.loadPage(importedPages.first().id)
                        Toast.makeText(context, "Imported ${importedPages.size} PDF page(s)", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to import PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // PDF Export Function
    fun exportAndShare() {
        coroutineScope.launch {
            try {
                Toast.makeText(context, "Exporting PDF...", Toast.LENGTH_SHORT).show()
                val exportedFile = PdfManager.exportNotebookToPdf(context, notebookId, repository)
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportedFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Notebook PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Native Stylus Ink Canvas (Fills Screen)
        InkCanvasComposable(
            viewModel = inkViewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Navigation Header & Toolbar (Top Pinned)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            // Notebook Top Bar: Back, Title, Page Navigation, Import PDF, Export PDF
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            inkViewModel.saveCurrentViewport()
                            onBackToNotebooks()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = notebookTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Page Navigator: < Page X of Y > + Add
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { inkViewModel.prevPage() },
                            enabled = uiState.pageIndex > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Page")
                        }
                        Text(
                            text = "Page ${uiState.pageIndex + 1} / ${uiState.totalPages}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { inkViewModel.nextPage() },
                            enabled = uiState.pageIndex < uiState.totalPages - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Page")
                        }
                        Button(
                            onClick = { inkViewModel.addNewPage() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text("+ Page", fontSize = 12.sp)
                        }
                    }

                    // Import PDF & Export Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Import PDF", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { exportAndShare() }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export PDF", modifier = Modifier.padding(end = 4.dp))
                            Text("Export", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Ink Drawing Toolbar (Pen, Highlighter, Eraser, Colors, Undo/Redo, etc.)
            InkToolbar(
                uiState = uiState,
                onSelectTool = { inkViewModel.selectTool(it) },
                onSelectColor = { inkViewModel.selectColor(it) },
                onSelectThickness = { inkViewModel.selectThickness(it) },
                onUndo = { inkViewModel.undo() },
                onRedo = { inkViewModel.redo() },
                onClearPage = { inkViewModel.clearPage() },
                onToggleDebug = { inkViewModel.toggleDebugOverlay() },
                onTriggerStressTest = { inkViewModel.triggerStressTest(2000) },
                onInsertTestPattern = { inkViewModel.insertTestPatternStroke() }
            )
        }
    }
}
