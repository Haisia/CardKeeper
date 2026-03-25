package com.cardkeeper.ui.carddetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
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
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }

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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                onImageClick = { fullscreenImagePath = absolutePath },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (fullscreenImagePath != null) {
        FullscreenImageViewer(
            imagePath = fullscreenImagePath!!,
            onDismiss = { fullscreenImagePath = null }
        )
    }
}

@Composable
private fun ViewMode(
    card: CardWithTags,
    imagePath: String?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onImageClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Card image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = card.card.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.card.company.isNotBlank() || card.card.jobTitle.isNotBlank()) {
                    Text(
                        text = listOfNotNull(
                            card.card.company.takeIf { it.isNotBlank() },
                            card.card.jobTitle.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(
                    icon = Icons.Rounded.Call,
                    label = "전화 (Phone)",
                    value = formatPhoneNumber(card.card.phone)
                )
                InfoRow(
                    icon = Icons.Rounded.Mail,
                    label = "이메일 (Email)",
                    value = card.card.email
                )
                InfoRow(
                    label = "주소 (Address)",
                    value = card.card.address
                )
            }
        }

        if (card.card.memo.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "메모 (Memo)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.card.memo,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (card.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card.tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FullscreenImageViewer(imagePath: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Card image fullscreen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatPhoneNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when (digits.length) {
        11 -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
        10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
        else -> phone
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            var tagExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = tagExpanded,
                onExpandedChange = { tagExpanded = it }
            ) {
                OutlinedTextField(
                    value = availableTags.filter { it.id in selectedTagIds }.joinToString(", ") { it.name }.ifEmpty { "태그 없음" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("태그 (Tags)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                ExposedDropdownMenu(
                    expanded = tagExpanded,
                    onDismissRequest = { tagExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    availableTags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        DropdownMenuItem(
                            text = { Text(tag.name) },
                            leadingIcon = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                            },
                            onClick = {
                                selectedTagIds = if (isSelected) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            }
                        )
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
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(CardEditData(name, company, jobTitle, phone, email, address, memo, selectedTagIds)) },
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
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
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
