package com.kelmy.mediaarchiver.presentation.gui

import com.kelmy.mediaarchiver.library.domain.model.NewChannel
import com.kelmy.mediaarchiver.library.application.ChannelService
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

/**
 * Modal dialog used to register a new channel.
 *
 * If the folder name is left empty, the channel name is used instead. Validation is done by
 * [NewChannel] itself, and any problem is shown to the user instead of being saved.
 *
 * @property channelService    where the new channel is created.
 * @param platforms         platforms offered in the combo box (one per extractor).
 * @property onChannelAdded    called with the data that was saved, after the insertion succeeded.
 */
internal class AddChannelDialog(
    owner: Frame,
    private val channelService: ChannelService,
    platforms: List<String>,
    private val onChannelAdded: (NewChannel) -> Unit = {}
) : JDialog(owner, "Add Channel", true) {
    private val nameField = JTextField()
    private val platformCombo = JComboBox(platforms.toTypedArray())
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

    /** Builds the label/field grid. */
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

    /** Builds the Save/Cancel button row. */
    private fun buildButtonsPanel(): JPanel {
        val saveButton = JButton("Save").apply { addActionListener { onSave() } }
        val cancelButton = JButton("Cancel").apply { addActionListener { dispose() } }

        return JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(cancelButton)
            add(saveButton)
        }
    }

    /** Builds a [NewChannel] from the form, inserts it and closes the dialog. Errors are shown and the dialog stays open. */
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

            channelService.create(channel)
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
