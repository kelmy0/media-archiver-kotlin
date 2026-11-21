package com.kelmy.mediaarchiver.ui.gui

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

class ChannelsPanel(
    private val owner: Frame,
    private val channelRepository: ChannelsRepository,
    private val onLog: (String) -> Unit
) : JPanel(BorderLayout(8, 8)) {
    private val columns = arrayOf("ID", "Name", "Platform", "Feed URL", "Folder", "Active")
    private val tableModel = object : DefaultTableModel(columns, 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JTable(tableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    }
    private val addButton = JButton("Add Channel")
    private val editButton = JButton("Edit Channel")
    private val deleteButton = JButton("Delete Channel")
    private val refreshButton = JButton("Refresh")

    private var currentChannels: List<Channel> = emptyList()

    init {
        border = BorderFactory.createEmptyBorder(8, 10, 10, 10)

        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(addButton)
            add(editButton)
            add(deleteButton)
            add(refreshButton)
        }

        add(actionsPanel, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)

        addButton.addActionListener { openAddDialog() }
        editButton.addActionListener { openEditDialog() }
        deleteButton.addActionListener { deleteSelectedChannel() }
        refreshButton.addActionListener { refreshChannels() }

        refreshChannels()
    }

    fun refreshChannels() {
        Thread({
            try {
                val list = channelRepository.getChannels()
                SwingUtilities.invokeLater {
                    currentChannels = list
                    tableModel.rowCount = 0
                    for ((id, name1, platform, feedUrl, folderName, active) in list) {
                        tableModel.addRow(
                            arrayOf<Any>(id, name1, platform, feedUrl, folderName, active)
                        )
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        this,
                        e.message ?: "Failed to load channels.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }, "Channels-Loader").start()
    }

    fun setCrudEnabled(enabled: Boolean) {
        addButton.isEnabled = enabled
        editButton.isEnabled = enabled
        deleteButton.isEnabled = enabled
        refreshButton.isEnabled = enabled
    }

    private fun getSelectedChannel(): Channel? {
        val row = table.selectedRow
        if (row < 0 || row >= currentChannels.size) {
            JOptionPane.showMessageDialog(
                this,
                "Select a channel in the table first.",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE
            )
            return null
        }
        return currentChannels[row]
    }

    private fun openAddDialog() {
        AddChannelDialog(owner, channelRepository) { channel ->
            onLog("Added channel: '${channel.name}' (${channel.platform})")
            refreshChannels()
        }.isVisible = true
    }

    private fun openEditDialog() {
        val selected = getSelectedChannel() ?: return
        EditChannelDialog(owner, selected) { updated ->
            val ok = channelRepository.updateChannel(updated)
            if (ok) {
                onLog("Updated channel #${updated.id}: '${updated.name}'")
                refreshChannels()
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Channel #${updated.id} could not be updated.",
                    "Update Failed",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }.isVisible = true
    }

    private fun deleteSelectedChannel() {
        val selected = getSelectedChannel() ?: return
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete channel '${selected.name}' (#${selected.id})?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (confirm != JOptionPane.YES_OPTION) return

        val deleted = channelRepository.deleteChannel(selected.id)
        if (deleted) {
            onLog("Deleted channel #${selected.id}: '${selected.name}'")
            refreshChannels()
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Channel #${selected.id} could not be deleted.",
                "Delete Failed",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }
}

private class EditChannelDialog(
    owner: Frame,
    private val original: Channel,
    private val onConfirmUpdate: (Channel) -> Unit
) : JDialog(owner, "Edit Channel", true) {
    private val nameField = JTextField(original.name)
    private val platformCombo = JComboBox(arrayOf("youtube")).apply {
        selectedItem = original.platform
        if (selectedItem != original.platform) {
            addItem(original.platform)
            selectedItem = original.platform
        }
    }
    private val feedUrlField = JTextField(original.feedUrl)
    private val folderNameField = JTextField(original.folderName)
    private val activeCheck = JCheckBox("Active", original.active)

    init {
        setSize(420, 260)
        setLocationRelativeTo(owner)
        layout = BorderLayout(10, 10)

        val formPanel = JPanel(GridLayout(5, 2, 8, 8)).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 6, 12)
            add(JLabel("Name:"))
            add(nameField)
            add(JLabel("Platform:"))
            add(platformCombo)
            add(JLabel("Feed / Channel URL:"))
            add(feedUrlField)
            add(JLabel("Folder Name:"))
            add(folderNameField)
            add(JLabel("Status:"))
            add(activeCheck)
        }

        val saveButton = JButton("Save").apply { addActionListener { onSave() } }
        val cancelButton = JButton("Cancel").apply { addActionListener { dispose() } }

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(cancelButton)
            add(saveButton)
        }

        add(formPanel, BorderLayout.CENTER)
        add(buttonsPanel, BorderLayout.SOUTH)
    }

    private fun onSave() {
        try {
            val name = nameField.text.trim()
            val platform = platformCombo.selectedItem?.toString()?.trim().orEmpty()
            val feedUrl = feedUrlField.text.trim()
            val folderName = folderNameField.text.trim().ifEmpty { name }

            val updated = Channel(
                original.id,
                name,
                platform,
                feedUrl,
                folderName,
                activeCheck.isSelected,
                original.lastChecked,
                original.optionsJson
            )

            onConfirmUpdate(updated)
            dispose()
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(
                this,
                e.message ?: "Invalid channel data.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                e.message ?: "Failed to update channel.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}