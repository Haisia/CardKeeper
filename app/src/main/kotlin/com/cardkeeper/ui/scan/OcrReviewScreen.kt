package com.cardkeeper.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    viewModel: ScanViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadOcrResult()
    }

    LaunchedEffect(scanState) {
        if (scanState == ScanState.Saved) {
            viewModel.resetState()
            onSaved()
        }
    }

    LaunchedEffect(scanState) {
        if (scanState is ScanState.Error) {
            snackbarHostState.showSnackbar((scanState as ScanState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewTextField(
                    label = "이름 (Name)",
                    value = formState.name,
                    onValueChange = viewModel::updateName
                )
                ReviewTextField(
                    label = "회사 (Company)",
                    value = formState.company,
                    onValueChange = viewModel::updateCompany
                )
                ReviewTextField(
                    label = "직책 (Job Title)",
                    value = formState.jobTitle,
                    onValueChange = viewModel::updateJobTitle
                )
                ReviewTextField(
                    label = "전화 (Phone)",
                    value = formState.phone,
                    onValueChange = viewModel::updatePhone,
                    keyboardType = KeyboardType.Phone
                )
                ReviewTextField(
                    label = "이메일 (Email)",
                    value = formState.email,
                    onValueChange = viewModel::updateEmail,
                    keyboardType = KeyboardType.Email
                )
                ReviewTextField(
                    label = "주소 (Address)",
                    value = formState.address,
                    onValueChange = viewModel::updateAddress,
                    singleLine = false,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveCard() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = scanState != ScanState.Saving && scanState != ScanState.LoadingOcr,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (scanState == ScanState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("저장 (Save)")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (scanState == ScanState.LoadingOcr) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "텍스트 인식 중...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
