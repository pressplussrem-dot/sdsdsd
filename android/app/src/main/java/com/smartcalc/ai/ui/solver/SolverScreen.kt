package com.smartcalc.ai.ui.solver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartcalc.ai.R
import com.smartcalc.ai.SmartCalcApplication
import com.smartcalc.ai.navigation.SolverSource
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    source: SolverSource,
    onClose: () -> Unit,
    viewModel: SolverViewModel = viewModel(factory = SolverViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedEvent by viewModel.savedEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.solver_saved)

    val imageProcessor = remember {
        (context.applicationContext as SmartCalcApplication).container.imageProcessor
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var galleryAttempt by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
        if (!granted) {
            viewModel.onImageError(R.string.error_camera_permission_denied)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.onImageError(R.string.error_image_selection_cancelled)
        } else {
            val file = imageProcessor.copyUriToCache(uri)
            if (file == null) {
                viewModel.onImageError(R.string.error_image_invalid)
            } else {
                viewModel.onImageReady(file)
            }
        }
    }

    // Ask for the camera permission exactly once when the screen opens.
    LaunchedEffect(source) {
        if (source == SolverSource.CAMERA && !hasCameraPermission && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Open the system photo picker when we need a new gallery image.
    LaunchedEffect(source, state, galleryAttempt) {
        if (source == SolverSource.GALLERY && state is SolverUiState.AwaitingImage) {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    LaunchedEffect(savedEvent) {
        if (savedEvent) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.consumeSavedEvent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.solver_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = state) {
                SolverUiState.AwaitingImage -> {
                    if (source == SolverSource.CAMERA) {
                        if (hasCameraPermission) {
                            CameraStep(
                                onCaptured = viewModel::onImageReady,
                                onError = viewModel::onImageError
                            )
                        } else {
                            PermissionRequiredContent(
                                messageRes = R.string.error_camera_permission_denied,
                                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                onOpenSettings = { context.openAppSettings() },
                                onBack = onClose
                            )
                        }
                    } else {
                        LoadingContent(textRes = null)
                    }
                }

                is SolverUiState.ImageReady -> ImageReadyContent(
                    file = current.file,
                    warningRes = current.warningRes,
                    source = source,
                    onSolve = viewModel::solve,
                    onRetake = {
                        galleryAttempt++
                        viewModel.retakeImage()
                    },
                    onBack = onClose
                )

                is SolverUiState.Solving -> SolvingContent(current.file)

                is SolverUiState.Solved -> SolutionContent(
                    state = current,
                    onNewTask = {
                        galleryAttempt++
                        viewModel.startNewTask()
                    },
                    onSave = viewModel::saveToHistory,
                    onBack = onClose
                )

                is SolverUiState.Unclear -> MessageContent(
                    messageRes = R.string.error_unclear_photo,
                    file = current.file,
                    primaryLabelRes = if (source == SolverSource.CAMERA) {
                        R.string.solver_retake
                    } else {
                        R.string.solver_pick_again
                    },
                    onPrimary = {
                        galleryAttempt++
                        viewModel.retakeImage()
                    },
                    secondaryLabelRes = R.string.action_back,
                    onSecondary = onClose
                )

                is SolverUiState.Error -> MessageContent(
                    messageRes = current.messageRes,
                    file = current.file,
                    primaryLabelRes = if (current.canRetrySolve) {
                        R.string.action_retry
                    } else if (source == SolverSource.CAMERA) {
                        R.string.solver_retake
                    } else {
                        R.string.solver_pick_again
                    },
                    onPrimary = {
                        if (current.canRetrySolve) {
                            viewModel.solve()
                        } else {
                            galleryAttempt++
                            viewModel.retakeImage()
                        }
                    },
                    secondaryLabelRes = R.string.action_back,
                    onSecondary = onClose
                )
            }
        }
    }
}

@Composable
private fun CameraStep(
    onCaptured: (File) -> Unit,
    onError: (Int) -> Unit
) {
    var capture by remember {
        mutableStateOf<((onSaved: (File) -> Unit, onError: (Int) -> Unit) -> Unit)?>(null)
    }
    var capturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraCapture(
            modifier = Modifier.fillMaxSize(),
            onCaptureReady = { capture = it },
            onCameraError = onError
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.solver_capture_hint),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val action = capture
                    if (action == null) {
                        onError(R.string.error_camera_unavailable)
                    } else if (!capturing) {
                        capturing = true
                        action(
                            { file ->
                                capturing = false
                                onCaptured(file)
                            },
                            { messageRes ->
                                capturing = false
                                onError(messageRes)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = !capturing
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.solver_shutter))
            }
        }
    }
}

@Composable
private fun ImageReadyContent(
    file: File,
    warningRes: Int?,
    source: SolverSource,
    onSolve: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CapturedImage(file)

        if (warningRes != null) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(warningRes),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSolve,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.solver_solve))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                stringResource(
                    if (source == SolverSource.CAMERA) {
                        R.string.solver_retake
                    } else {
                        R.string.solver_pick_again
                    }
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun SolvingContent(file: File) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CapturedImage(file)
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.solver_solving),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SolutionContent(
    state: SolverUiState.Solved,
    onNewTask: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CapturedImage(state.file)

        Spacer(Modifier.height(18.dp))
        SectionTitle(stringResource(R.string.solver_recognized_problem))
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = state.problem,
                modifier = Modifier.padding(14.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        }

        if (state.steps.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle(stringResource(R.string.solver_steps))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    state.steps.forEach { step ->
                        Text(
                            text = step,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle(stringResource(R.string.solver_solution))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.solution,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onNewTask,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.solver_new_task))
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onSave,
                enabled = !state.saved,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = stringResource(
                        if (state.saved) R.string.solver_saved else R.string.solver_save_to_history
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MessageContent(
    messageRes: Int,
    file: File?,
    primaryLabelRes: Int,
    onPrimary: () -> Unit,
    secondaryLabelRes: Int,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (file != null && file.exists()) {
            CapturedImage(file)
            Spacer(Modifier.height(18.dp))
        } else {
            Spacer(Modifier.height(40.dp))
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(messageRes),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(primaryLabelRes))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(secondaryLabelRes))
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    messageRes: Int,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(messageRes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_grant_permission))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_open_settings))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun LoadingContent(textRes: Int?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        if (textRes != null) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(textRes))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun CapturedImage(file: File) {
    AsyncImage(
        model = file,
        contentDescription = stringResource(R.string.cd_captured_image),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(18.dp))
    )
}

private fun android.content.Context.openAppSettings() {
    runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
