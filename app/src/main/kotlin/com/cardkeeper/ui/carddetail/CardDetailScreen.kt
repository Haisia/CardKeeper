package com.cardkeeper.ui.carddetail

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.cardkeeper.data.datasource.ContactsDataSource
import com.cardkeeper.data.db.CardWithTags
import com.cardkeeper.data.db.TagEntity

data class CardEditData(
    val name: String, val company: String, val jobTitle: String,
    val phone: String, val email: String, val address: String,
    val memo: String, val tagIds: List<Long>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    onDeleted: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: CardDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.card) {
        if (uiState.card == null && !uiState.isLoading) {
            onDeleted()
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteDialog(false) },
            title = { Text("Delete Card?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCard() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDeleteDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) "Edit Card" else "Card Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!uiState.isEditing) {
                        IconButton(onClick = {
                            val card = uiState.card ?: return@IconButton
                            val contactsDataSource = ContactsDataSource(context)
                            val intent = contactsDataSource.createExportIntent(
                                name = card.card.name,
                                company = card.card.company,
                                jobTitle = card.card.jobTitle,
                                phone = card.card.phone,
                                email = card.card.email
                            )
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Export to Contacts")
                        }
                        IconButton(onClick = { viewModel.setEditing(true) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { viewModel.setShowDeleteDialog(true) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val card = uiState.card

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (card == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Card not found")
            }
        } else if (uiState.isEditing) {
            EditMode(
                card = card,
                availableTags = availableTags,
                isSaving = uiState.isSaving,
                onSave = { data ->
                    viewModel.saveCard(data.name, data.company, data.jobTitle, data.phone, data.email, data.address, data.memo, data.tagIds)
                },
                onCancel = { viewModel.setEditing(false) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
                val absolutePath = card.card.imagePath?.let { java.io.File(context.filesDir, it).absolutePath }
                ViewMode(
                    card = card,
                    imagePath = absolutePath,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ViewMode(card: CardWithTags, imagePath: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Card image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )

        DetailRow(label = "이름 (Name)", value = card.card.name)
        DetailRow(label = "회사 (Company)", value = card.card.company)
        DetailRow(label = "직책 (Job Title)", value = card.card.jobTitle)
        DetailRow(label = "전화 (Phone)", value = card.card.phone)
        DetailRow(label = "이메일 (Email)", value = card.card.email)
        DetailRow(label = "주소 (Address)", value = card.card.address)

        if (card.card.memo.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "메모 (Memo)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = card.card.memo,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (card.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card.tags.forEach { tag ->
                    FilledTonalButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text(tag.name)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value.ifEmpty { "-"},
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditMode(
    card: CardWithTags,
    availableTags: List<TagEntity>,
    isSaving: Boolean,
    onSave: (CardEditData) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(card.card.id) { mutableStateOf(card.card.name) }
    var company by remember(card.card.id) { mutableStateOf(card.card.company) }
    var jobTitle by remember(card.card.id) { mutableStateOf(card.card.jobTitle) }
    var phone by remember(card.card.id) { mutableStateOf(card.card.phone) }
    var email by remember(card.card.id) { mutableStateOf(card.card.email) }
    var address by remember(card.card.id) { mutableStateOf(card.card.address) }
    var memo by remember(card.card.id) { mutableStateOf(card.card.memo) }
    var selectedTagIds by remember(card.card.id) {
        mutableStateOf(card.tags.map { it.id })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditField(label = "이름 (Name)", value = name, onValueChange = { name = it })
        EditField(label = "회사 (Company)", value = company, onValueChange = { company = it })
        EditField(label = "직책 (Job Title)", value = jobTitle, onValueChange = { jobTitle = it })
        EditField(label = "전화 (Phone)", value = phone, onValueChange = { phone = it })
        EditField(label = "이메일 (Email)", value = email, onValueChange = { email = it })
        EditField(
            label = "주소 (Address)",
            value = address,
            onValueChange = { address = it },
            singleLine = false,
            minLines = 2
        )
        EditField(
            label = "메모 (Memo)",
            value = memo,
            onValueChange = { memo = it },
            singleLine = false,
            minLines = 3
        )

        if (availableTags.isNotEmpty()) {
            Text(
                text = "태그 (Tags)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag.id in selectedTagIds
                    FilledTonalButton(
                        onClick = {
                            selectedTagIds = if (isSelected) {
                                selectedTagIds - tag.id
                            } else {
                                selectedTagIds + tag.id
                            }
                        }
                    ) {
                        Text(if (isSelected) "✓ ${tag.name}" else tag.name)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(CardEditData(name, company, jobTitle, phone, email, address, memo, selectedTagIds)) },
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines
    )
}
