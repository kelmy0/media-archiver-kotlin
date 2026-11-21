package com.kelmy.mediaarchiver.ui.gui

import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * Main window of the application: action buttons, the "Activity Log" tab and the "Channels" tab.
 *
 * The window never runs the archiving itself; it only calls [onRunArchiving], so it stays
 * independent from the application layer.
 *
 * @property channelRepository used by the channels tab and by the "Add Channel" dialog.
 * @property settingsStore     used by the settings dialog.
 * @property onRunArchiving    runs one archiving routine. It is called from a background thread
 *                             and may block for a long time.
 */
class MainWindow(
    private val channelRepository: ChannelsRepository,
    private val settingsStore: SettingsStore,
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
    private val channelsPanel = ChannelsPanel(
        owner = this,
        channelRepository = channelRepository,
        onLog = { message -> appendLog(message) }
    )

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
            border = BorderFactory.createEmptyBorder(8, 10, 10, 10)
            add(JScrollPane(logArea), BorderLayout.CENTER)
        }

        val tabbedPane = JTabbedPane().apply {
            addTab("Activity Log", logPanel)
            addTab("Channels", channelsPanel)
        }

        val centerPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
            add(tabbedPane, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)

        runButton.addActionListener { startArchivingInBackground() }
        addChannelButton.addActionListener { openAddChannelDialog() }
        settingsButton.addActionListener { openSettingsDialog() }
        clearLogButton.addActionListener { clearLogs() }

        tabbedPane.addChangeListener {
            if (tabbedPane.selectedComponent === channelsPanel) {
                channelsPanel.refreshChannels()
            }
        }

        GuiLogAppender.sink = { line -> SwingUtilities.invokeLater { appendLog(line) } }
    }

    /**
     * Runs [onRunArchiving] on a dedicated thread so the window does not freeze. The controls stay
     * disabled until it ends, and the final status is shown whether it succeeds or fails.
     */
    private fun startArchivingInBackground() {
        setControlsEnabled(false)
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
                    setControlsEnabled(true)
                }
            }
        }, "MediaArchiver-Worker").start()
    }

    /** Opens the modal dialog used to register a new channel. */
    private fun openAddChannelDialog() {
        AddChannelDialog(this, channelRepository) { channel ->
            appendLog("Added channel: '${channel.name}' (${channel.platform})")
        }.isVisible = true
    }

    /** Enables/disables every control that must not be used while an archiving routine is running. */
    private fun setControlsEnabled(enabled: Boolean) {
        runButton.isEnabled = enabled
        addChannelButton.isEnabled = enabled
        settingsButton.isEnabled = enabled
        channelsPanel.setCrudEnabled(enabled)
    }

    /** Opens the modal settings dialog. */
    private fun openSettingsDialog() {
        SettingsDialog(this, settingsStore) {
            appendLog("Settings saved.")
        }.isVisible = true
    }

    /** Adds a line to the activity log and scrolls to the end. Must be called from the Swing thread. */
    private fun appendLog(message: String) {
        logArea.append("$message\n")
        logArea.caretPosition = logArea.document.length
    }

    /** Updates the status bar text. Must be called from the Swing thread. */
    private fun setStatus(status: String) {
        statusLabel.text = "Status: $status"
    }

    /** Clears the activity log. */
    private fun clearLogs() {
        logArea.text = ""
        setStatus("Log cleared")
    }
}
