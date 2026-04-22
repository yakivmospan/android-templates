package com.yakivmospan.autocompletesample.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage

/**
 * Factory / entry point — convenience for the common case
 */
@Composable
fun rememberGitHubAutoCompleteViewModel(): AutoCompleteViewModel<GitHubItem> = viewModel(
    factory = AutoCompleteViewModel.Factory(GitHubAutoCompleteDataSource())
)

// ── Public overload — ViewModel-connected ─────────────────────────────────────

/**
 * Opinionated GitHub autocomplete component.
 *
 * Wraps [AutoCompleteComponent] with a pre-built [GitHubItemRow] and surfaces
 * item selection via [onItemSelected]. For full UI customisation, use
 * [AutoCompleteComponent] directly and provide your own [itemContent].
 *
 * @param viewModel the ViewModel driving this component.
 * @param onItemSelected callback fired when the user taps a result row.
 * @param modifier optional modifier applied to the root column.
 */
@Composable
fun GitHubAutoCompleteComponent(
    viewModel: AutoCompleteViewModel<GitHubItem>,
    onItemSelected: (GitHubItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    GitHubAutoCompleteComponent(
        query = query,
        state = state,
        onEvent = viewModel::onEvent,
        onItemSelected = onItemSelected,
        modifier = modifier,
    )
}

// ── Stateless overload ────────────────────────────────────────────────────────

@Composable
fun GitHubAutoCompleteComponent(
    query: String,
    state: AutoCompleteState<GitHubItem>,
    onEvent: (AutoCompleteEvent) -> Unit,
    onItemSelected: (GitHubItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AutoCompleteComponent(
        query = query,
        state = state,
        onEvent = onEvent,
        modifier = modifier,
        itemContent = { item ->
            GitHubItemRow(item = item, onClick = { onItemSelected(item) })
        },
    )
}

// ── GitHub item row ───────────────────────────────────────────────────────────

@Composable
internal fun GitHubItemRow(
    item: GitHubItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel = when (item) {
        is GitHubItem.User -> stringResource(R.string.autocomplete_item_type_user)
        is GitHubItem.Repository -> stringResource(R.string.autocomplete_item_type_repository)
    }
    val avatarUrl = when (item) {
        is GitHubItem.User -> item.avatarUrl
        is GitHubItem.Repository -> item.avatarUrl
    }
    val avatarDescription = stringResource(R.string.autocomplete_avatar_description, item.name)
    val rowDescription = "$typeLabel: ${item.name}"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = rowDescription
                role = Role.Button
            },
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = avatarDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "GitHub — Idle")
@Composable
private fun GitHubAutoCompleteIdlePreview() {
    MaterialTheme {
        GitHubAutoCompleteComponent(
            query = "",
            state = AutoCompleteState.Idle,
            onEvent = {},
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true, name = "GitHub — Results")
@Composable
private fun GitHubAutoCompleteSuccessPreview() {
    MaterialTheme {
        GitHubAutoCompleteComponent(
            query = "kotlin",
            state = AutoCompleteState.Success(
                items = listOf(
                    GitHubItem.User(id = 1L, login = "torvalds", avatarUrl = null),
                    GitHubItem.Repository(id = 2L, fullName = "kotlin/kotlin", avatarUrl = null),
                    GitHubItem.User(id = 3L, login = "gvanrossum", avatarUrl = null),
                ),
            ),
            onEvent = {},
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true, name = "GitHub — Empty")
@Composable
private fun GitHubAutoCompleteEmptyPreview() {
    MaterialTheme {
        GitHubAutoCompleteComponent(
            query = "xyzxyz",
            state = AutoCompleteState.Empty,
            onEvent = {},
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true, name = "GitHub — Error")
@Composable
private fun GitHubAutoCompleteErrorPreview() {
    MaterialTheme {
        GitHubAutoCompleteComponent(
            query = "kotlin",
            state = AutoCompleteState.Error("Network unavailable"),
            onEvent = {},
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true, name = "User row")
@Composable
private fun GitHubItemRowUserPreview() {
    MaterialTheme {
        GitHubItemRow(
            item = GitHubItem.User(id = 1L, login = "torvalds", avatarUrl = null),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Repository row")
@Composable
private fun GitHubItemRowRepositoryPreview() {
    MaterialTheme {
        GitHubItemRow(
            item = GitHubItem.Repository(id = 1L, fullName = "kotlin/kotlin", avatarUrl = null),
            onClick = {},
        )
    }
}

