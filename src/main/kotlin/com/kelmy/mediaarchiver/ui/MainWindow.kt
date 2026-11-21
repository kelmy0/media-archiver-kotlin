package com.kelmy.mediaarchiver.ui

import com.kelmy.mediaarchiver.model.ChannelModel
import com.kelmy.mediaarchiver.model.NewChannel
import com.kelmy.mediaarchiver.repositories.ChannelsRepositoryProvider
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.time.Instant
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities

class MainWindow(
    private val channelRepository: ChannelsRepositoryProvider,
    private val onRunArchiving: () -> Unit
) : JFrame("Media Archiver") {
    private val runButton = JButton("Run Archiving")
    private val addChannelButton = JButton("Add Channel")
    private val settingsButton = JButton("Settings")
    private val clearLogButton = JButton("Clear Log")
    private val statusLabel = JLabel("Ready")
    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 600)
        minimumSize = Dimension(400, 300)
        setLocationRelativeTo(null)
        layout = BorderLayout(10, 10)

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10)).apply {
            add(runButton)
            add(addChannelButton)
            add(settingsButton)
            add(clearLogButton)
        }

        val logPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
            add(JLabel("Activity Log"), BorderLayout.NORTH)
            add(JScrollPane(logArea), BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }

        add(topPanel, BorderLayout.NORTH)
        add(logPanel, BorderLayout.CENTER)

        runButton.addActionListener { startArchivingInBackground() }
        addChannelButton.addActionListener { openAddChannelDialog() }
        settingsButton.addActionListener { openSettingsDialog() }
        clearLogButton.addActionListener { clearLogs() }

        GuiLogAppender.sink = { line -> SwingUtilities.invokeLater { appendLog(line) } }
    }

    private fun startArchivingInBackground() {
        runButton.isEnabled = false
        setStatus("Running archiving routine...")
        appendLog("Starting archiving routine in background thread...")

        Thread({
            try {
                onRunArchiving()
                SwingUtilities.invokeLater {
                    appendLog("Archiving routine finished.")
                    setStatus("Completed")
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    appendLog("Error during archiving: ${e.message}")
                    setStatus("Error")
                    JOptionPane.showMessageDialog(
                        this,
                        e.message ?: "Unexpected error while running archiving.",
                        "Archiving Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } finally {
                SwingUtilities.invokeLater {
                    runButton.isEnabled = true
                }
            }
        }, "MediaArchiver-Worker").start()
    }

    private fun openAddChannelDialog() {
        val dialog = JDialog(this, "Add Channel", true).apply {
            setSize(420, 260)
            setLocationRelativeTo(this@MainWindow)
            layout = BorderLayout(10, 10)
        }

        val nameField = JTextField()
        val platformCombo = JComboBox(arrayOf("youtube"))
        val feedUrlField = JTextField()
        val folderNameField = JTextField()
        val activeCheck = JCheckBox("Active", true)

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

        val saveButton = JButton("Save")
        val cancelButton = JButton("Cancel")

        saveButton.addActionListener {
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
                appendLog("Added channel: '$name' ($platform)")
                dialog.dispose()
            } catch (e: IllegalArgumentException) {
                JOptionPane.showMessageDialog(
                    dialog,
                    e.message ?: "Invalid channel data.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    dialog,
                    e.message ?: "Failed to save channel.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        cancelButton.addActionListener { dialog.dispose() }

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(cancelButton)
            add(saveButton)
        }

        dialog.add(formPanel, BorderLayout.CENTER)
        dialog.add(buttonsPanel, BorderLayout.SOUTH)
        dialog.isVisible = true
    }

    private fun openSettingsDialog() {
        val dialog = JDialog(this, "Settings", true).apply {
            setSize(400, 180)
            setLocationRelativeTo(this@MainWindow)
            layout = BorderLayout(10, 10)
        }

        val infoLabel = JLabel(
            "<html><body style='padding:12px;'>" +
                    "Configuration is currently managed via ConfigManager.<br/>" +
                    "You can wire editable settings (download directory, cooldown) here next." +
                    "</body></html>"
        )
        val closeButton = JButton("Close").apply {
            addActionListener { dialog.dispose() }
        }

        dialog.add(infoLabel, BorderLayout.CENTER)
        dialog.add(JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply { add(closeButton) }, BorderLayout.SOUTH)
        dialog.isVisible = true
    }

    private fun appendLog(message: String) {
        logArea.append("$message\n")
        logArea.caretPosition = logArea.document.length
    }

    private fun setStatus(status: String) {
        statusLabel.text = "Status: $status"
    }

    private fun clearLogs() {
        logArea.text = ""
        setStatus("Log cleared")
    }
}
