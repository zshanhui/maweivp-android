package com.v2ray.ang.ui.block

import android.app.Application
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.BlockList
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.block.BlockStore
import com.v2ray.ang.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the blocked-sites screen.
 *
 * Every change is written to [BlockStore] and then marks the tunnel for a restart, because the
 * generated block rule only exists in the configuration the core is built from.
 */
class BlockSitesViewModel(application: Application) : BaseViewModel(application) {

    private val _list = MutableStateFlow(BlockStore.instance.load())
    val list: StateFlow<BlockList> = _list.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        if (_list.value.enabled == enabled) return
        applyChange { BlockStore.instance.saveEnabled(enabled) }
    }

    fun addSites(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { BlockStore.instance.addSites(text) }
            _list.value = result.list
            when {
                result.added > 0 -> toastSuccess(getString(R.string.block_sites_added, result.added))
                result.rejected > 0 -> toastError(getString(R.string.block_sites_rejected, result.rejected))
                else -> toast(getString(R.string.block_sites_unchanged))
            }
            if (result.added > 0) SettingsChangeManager.makeRestartService()
        }
    }

    fun removeSite(site: String) {
        applyChange { BlockStore.instance.removeSite(site) }
    }

    private fun applyChange(change: () -> BlockList) {
        viewModelScope.launch {
            _list.value = withContext(Dispatchers.IO) { change() }
            SettingsChangeManager.makeRestartService()
        }
    }
}
