package com.v2ray.ang.ui.block

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.BlockList
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.InputDialog
import com.v2ray.ang.ui.compose.InputField
import com.v2ray.ang.ui.compose.ItemDivider
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsSwitchItem

/**
 * Screen for the single blocked-site list: one switch, a list of sites, and an add action.
 */
class BlockSitesActivity : BaseComponentActivity() {

    private val viewModel: BlockSitesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        val list by viewModel.list.collectAsStateWithLifecycle()

        BlockSitesScreen(
            list = list,
            onBackClick = { finish() },
            onEnabledChange = { viewModel.setEnabled(it) },
            onAddSites = { viewModel.addSites(it) },
            onRemoveSite = { viewModel.removeSite(it) }
        )
    }
}

@Composable
fun BlockSitesScreen(
    list: BlockList,
    onBackClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAddSites: (String) -> Unit,
    onRemoveSite: (String) -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    // Removal asks first: dropping a site silently restores access to it.
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    // Switching the whole list off does the same for every entry at once.
    var pendingDisable by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.block_sites_title),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.block_sites_add)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsSwitchItem(
                icon = painterResource(R.drawable.ic_block_24dp),
                title = stringResource(R.string.block_sites_switch_title),
                summary = stringResource(R.string.block_sites_summary, list.sites.size),
                checked = list.enabled,
                onCheckedChange = { checked ->
                    if (!checked && list.sites.isNotEmpty()) {
                        pendingDisable = true
                    } else {
                        onEnabledChange(checked)
                    }
                }
            )
            AppDivider()

            if (list.sites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.block_sites_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list.sites, key = { it }) { site ->
                        BlockSiteRow(
                            site = site,
                            onRemove = { pendingRemoval = site }
                        )
                        ItemDivider()
                    }
                }
            }

            NavigationBarsSpacer()
        }
    }

    if (pendingDisable) {
        ConfirmDialog(
            title = stringResource(R.string.block_sites_disable_title),
            message = stringResource(R.string.block_sites_disable_message, list.sites.size),
            confirmText = stringResource(R.string.block_sites_disable_action),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = { onEnabledChange(false) },
            onDismiss = { pendingDisable = false }
        )
    }

    pendingRemoval?.let { site ->
        ConfirmDialog(
            title = stringResource(R.string.block_sites_remove_title),
            message = stringResource(R.string.block_sites_remove_message, site),
            confirmText = stringResource(R.string.action_remove),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = { onRemoveSite(site) },
            onDismiss = { pendingRemoval = null }
        )
    }

    if (showAddDialog) {
        InputDialog(
            title = stringResource(R.string.block_sites_add_title),
            fields = listOf(
                InputField(
                    label = stringResource(R.string.block_sites_add_hint),
                    value = draft,
                    singleLine = false
                )
            ),
            onFieldChange = { _, value -> draft = value },
            confirmText = stringResource(R.string.action_ok),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showAddDialog = false
                onAddSites(draft)
                draft = ""
            },
            onDismiss = {
                showAddDialog = false
                draft = ""
            }
        )
    }
}

@Composable
private fun BlockSiteRow(site: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = site,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24dp),
                contentDescription = stringResource(R.string.block_sites_remove, site)
            )
        }
    }
}
