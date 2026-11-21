package com.kelmy.mediaarchiver.ui.gui

import com.kelmy.mediaarchiver.core.domain.model.NewChannel
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
import javax.swing.JTextField

class AddChannelDialog(
    owner: Frame,
    private val channelRepository: ChannelsRepository,
    private val onChannelAdded: (NewChannel) -> Unit = {}
) : JDialog(owner, "Add Channel", true) {
    private val nameField = JTextField()
    private val platformCombo = JComboBox(arrayOf("youtube"))
    private val feedUrlField = JTextField()
    private val folderNameField = JTextField()
    private val activeCheck = JCheckBox("Active", true)

    init {
        setSize(420, 260)
        setLocationRelativeTo(owner)
        layout = BorderLayout(10, 10)

        add(buildFormPanel(), BorderLayout.CENTER)
        add(buildButtonsPanel(), BorderLayout.SOUTH)
    }

    private fun buildFormPanel(): JPanel =
        JPanel(GridLayout(5, 2, 8, 8)).apply {
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

    private fun buildButtonsPanel(): JPanel {
        val saveButton = JButton("Save").apply { addActionListener { onSave() } }
        val cancelButton = JButton("Cancel").apply { addActionListener { dispose() } }

        return JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(cancelButton)
            add(saveButton)
        }
    }

    private fun onSave() {
        val name = nameField.text.trim()
        val platform = platformCombo.selectedItem?.toString()?.trim().orEmpty()
        val feedUrl = feedUrlField.text.trim()
        val folderName = folderNameField.text.trim().ifEmpty { name }

        try {
            val channel = NewChannel(
                name = name,
                platform = platform,
                feedUrl = feedUrl,
                folderName = folderName,
                active = activeCheck.isSelected,
                optionsJson = "{}"
            )

            channelRepository.insert(channel)
            onChannelAdded(channel)
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
                e.message ?: "Failed to save channel.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}