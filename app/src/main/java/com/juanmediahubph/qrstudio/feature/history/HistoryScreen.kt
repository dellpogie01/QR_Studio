@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.juanmediahubph.qrstudio.feature.history

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.core.ui.components.EmptyState
import com.juanmediahubph.qrstudio.core.ui.iconRes
import com.juanmediahubph.qrstudio.core.ui.labelRes
import com.juanmediahubph.qrstudio.domain.model.HistoryFilter
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpen: (QrType, Long) -> Unit,
    onCreate: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val selectionMode = state.selectedIds.isNotEmpty()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.Deleted -> {
                    val message = if (event.count == 1) {
                        context.getString(R.string.snack_deleted_one)
                    } else {
                        context.getString(R.string.snack_deleted_many, event.count)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            if (selectionMode) {
                SelectionHeader(
                    selected = state.selectedIds,
                    items = state.items,
                    onClose = viewModel::clearSelection,
                    onFavorite = viewModel::setSelectedFavorite,
                    onDelete = viewModel::deleteSelected,
                )
            } else {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = viewModel::clearSearch) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_close),
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 12.dp, end = 20.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.filter == HistoryFilter.All,
                            onClick = { viewModel.onFilterChange(HistoryFilter.All) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.filter.favoritesOnly,
                            onClick = { viewModel.onFilterChange(HistoryFilter.Favorites) },
                            label = { Text(stringResource(R.string.filter_favorites)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_favorite_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                    items(QrType.entries) { type ->
                        val label = stringResource(type.labelRes())
                        FilterChip(
                            selected = state.filter.type == type,
                            onClick = { viewModel.onFilterChange(HistoryFilter.ofType(type)) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(type.iconRes()),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = context.getString(
                                    R.string.filter_type_content_desc,
                                    label,
                                )
                            },
                        )
                    }
                }
            }

            when {
                state.isLoading && state.items.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.items.isEmpty() -> EmptyHistory(
                    state = state,
                    onCreate = onCreate,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items, key = { it.id }) { qr ->
                        HistoryListItem(
                            qr = qr,
                            viewModel = viewModel,
                            selectionMode = selectionMode,
                            selected = qr.id in state.selectedIds,
                            onOpen = { onOpen(qr.type, qr.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    selected: Set<Long>,
    items: List<SavedQr>,
    onClose: () -> Unit,
    onFavorite: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val allFavorite = items.isNotEmpty() &&
        items.filter { it.id in selected }.all { it.isFavorite }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_action_close),
                contentDescription = stringResource(R.string.exit_selection),
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.selected_count,
                selected.size,
                selected.size,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onFavorite(!allFavorite) }) {
            Icon(
                painter = painterResource(
                    if (allFavorite) {
                        R.drawable.ic_action_favorite_filled
                    } else {
                        R.drawable.ic_action_favorite
                    },
                ),
                contentDescription = stringResource(
                    if (allFavorite) R.string.unfavorite_selected else R.string.favorite_selected,
                ),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_action_delete),
                contentDescription = stringResource(R.string.delete_selected),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EmptyHistory(
    state: HistoryUiState,
    onCreate: () -> Unit,
) {
    val queryActive = state.query.isNotBlank()
    when {
        state.filter.favoritesOnly && !queryActive -> EmptyState(
            iconRes = R.drawable.ic_action_favorite,
            title = stringResource(R.string.empty_favorites_title),
            body = stringResource(R.string.empty_favorites_body),
            actionLabel = stringResource(R.string.empty_action_create),
            onAction = onCreate,
        )

        queryActive || state.filter.type != null -> EmptyState(
            iconRes = R.drawable.ic_action_search,
            title = stringResource(R.string.empty_search_title),
            body = stringResource(R.string.empty_search_body),
        )

        else -> EmptyState(
            iconRes = R.drawable.ic_empty_qr,
            title = stringResource(R.string.empty_history_title),
            body = stringResource(R.string.empty_history_body),
            actionLabel = stringResource(R.string.empty_action_create),
            onAction = onCreate,
        )
    }
}

@Composable
private fun HistoryListItem(
    qr: SavedQr,
    viewModel: HistoryViewModel,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val itemDescription = stringResource(
        R.string.history_item_desc,
        stringResource(qr.type.labelRes()),
        qr.title,
    )
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                viewModel.delete(qr)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier.clip(shape),
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (selectionMode) viewModel.toggleSelection(qr.id) else onOpen()
                    },
                    onLongClick = {
                        if (!selectionMode) viewModel.toggleSelection(qr.id)
                    },
                )
                .semantics {
                    contentDescription = itemDescription
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistoryThumbnail(qr = qr, viewModel = viewModel)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = qr.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val timestamp = remember(qr.updatedAt) {
                        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(qr.updatedAt))
                    }
                    Text(
                        text = "${stringResource(qr.type.labelRes())} · $timestamp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { viewModel.toggleSelection(qr.id) },
                    )
                } else {
                    IconButton(onClick = { viewModel.toggleFavorite(qr) }) {
                        Icon(
                            painter = painterResource(
                                if (qr.isFavorite) {
                                    R.drawable.ic_action_favorite_filled
                                } else {
                                    R.drawable.ic_action_favorite
                                },
                            ),
                            contentDescription = stringResource(
                                if (qr.isFavorite) R.string.unfavorite else R.string.favorite,
                            ),
                            tint = if (qr.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(qr: SavedQr, viewModel: HistoryViewModel) {
    val bitmap by produceState<Bitmap?>(initialValue = null, qr.id, qr.updatedAt) {
        value = viewModel.thumbnail(qr)
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = stringResource(R.string.qr_thumbnail_desc, qr.title),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_empty_qr),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
