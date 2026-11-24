package com.notes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notes.ink.persistence.InkRepository
import com.notes.ink.persistence.NotesDatabase
import com.notes.ink.ui.InkCanvasComposable
import com.notes.ink.ui.InkCanvasViewModel
import com.notes.ink.ui.InkToolbar
import com.notes.model.BoundingBox
import com.notes.model.Line
import com.notes.model.RecognitionResult
import com.notes.net.ServerMessage
import com.notes.net.TutorWebSocketClient
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotebookCanvasScreen()
                }
            }
        }
    }
}

@Composable
fun NotebookCanvasScreen() {
    val context = LocalContext.current
    val database = remember { NotesDatabase.getInstance(context) }
    val repository = remember { InkRepository(database) }

    val inkViewModel: InkCanvasViewModel = viewModel {
        InkCanvasViewModel(repository)
    }

    val uiState by inkViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        inkViewModel.loadPage("default-page")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Native Stylus Ink Canvas (Fills Screen)
        InkCanvasComposable(
            viewModel = inkViewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Toolbar (Top Pinned)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            InkToolbar(
                uiState = uiState,
                onSelectTool = { inkViewModel.selectTool(it) },
                onSelectColor = { inkViewModel.selectColor(it) },
                onSelectThickness = { inkViewModel.selectThickness(it) },
                onUndo = { inkViewModel.undo() },
                onRedo = { inkViewModel.redo() },
                onClearPage = { inkViewModel.clearPage() },
                onToggleDebug = { inkViewModel.toggleDebugOverlay() },
                onTriggerStressTest = { inkViewModel.triggerStressTest(2000) }
            )
        }
    }
}
