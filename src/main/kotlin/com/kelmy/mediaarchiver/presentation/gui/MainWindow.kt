package com.kelmy.mediaarchiver.presentation.gui

import com.kelmy.mediaarchiver.presentation.AppServices
import java.nio.file.Path
import javax.swing.JFileChooser
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
 * Main window of the application: action buttons and the "Activity Log", "Channels" and "Videos" tabs.
 *
 * It holds no business rule: every action is delegated to the application services in [services]
 * (the archiving routine is called from a background thread because it may block for a long time).
 *
 * @property services the application services shared with every interface.
 */
internal class MainWindow(
    private val services: AppServices
) : JFrame("Media Archiver") {
    private val runButton = JButton("Run Archiving")
    private val addChannelButton = JButton("Add Channel")
    private val settingsButton = JButton("Settings")
    private val clearLogButton = JButton("Clear Log")
    private val saveDataButton = JButton("Save Data...")
    private val loadDataButton = JButton("Load Data...")
    private val statusLabel = JLabel("Ready")
    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val channelsPanel = ChannelsPanel(
        owner = this,
        channelService = services.channels,
        platforms = services.platforms,
        onLog = { message -> appendLog(message) }
    )
    private val videosPanel = VideosPanel(
        owner = this,
        channelService = services.channels,
        videoService = services.videos,
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
            add(saveDataButton)
            add(loadDataButton)
        }

        val logPanel = JPanel(BorderLayout(5, 5)).apply {
            border = BorderFactory.createEmptyBorder(8, 10, 10, 10)
            add(JScrollPane(logArea), BorderLayout.CENTER)
        }

        val tabbedPane = JTabbedPane().apply {
            addTab("Activity Log", logPanel)
            addTab("Channels", channelsPanel)
            addTab("Videos", videosPanel)
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
        saveDataButton.addActionListener { saveData() }
        loadDataButton.addActionListener { loadData() }

        tabbedPane.addChangeListener {
            if (tabbedPane.selectedComponent === channelsPanel) {
                channelsPanel.refreshChannels()
            } else if (tabbedPane.selectedComponent === videosPanel) {
                videosPanel.refresh()
            }
        }

        GuiLogAppender.sink = { line -> SwingUtilities.invokeLater { appendLog(line) } }
    }

    /**
     * Runs [AppServices.runArchiving] on a dedicated thread so the window does not freeze. The controls stay
     * disabled until it ends, and the final status is shown whether it succeeds or fails.
     */
    private fun startArchivingInBackground() {
        setControlsEnabled(false)
        setStatus("Running archiving routine...")
        appendLog("Starting archiving routine in background thread...")

        Thread({
            try {
                services.runArchiving()
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
        AddChannelDialog(this, services.channels, services.platforms) { channel ->
            appendLog("Added channel: '${channel.name}' (${channel.platform})")
            channelsPanel.refreshChannels()
        }.isVisible = true
    }

    /** Enables/disables every control that must not be used while an archiving routine is running. */
    private fun setControlsEnabled(enabled: Boolean) {
        runButton.isEnabled = enabled
        addChannelButton.isEnabled = enabled
        settingsButton.isEnabled = enabled
        saveDataButton.isEnabled = enabled
        loadDataButton.isEnabled = enabled
        channelsPanel.setCrudEnabled(enabled)
        videosPanel.setCrudEnabled(enabled)
    }

    /** Opens the modal settings dialog. */
    private fun openSettingsDialog() {
        SettingsDialog(this, services.settings) {
            appendLog("Settings saved.")
        }.isVisible = true
    }

    /** Asks for a file and saves every channel with its videos to it. */
    private fun saveData() {
        val chooser = JFileChooser().apply { selectedFile = java.io.File("mediaarchiver-backup.json") }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return

        try {
            val count = services.backup.saveTo(Path.of(chooser.selectedFile.absolutePath))
            appendLog("Saved $count channel(s) and their videos to ${chooser.selectedFile}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message ?: "Failed to save data.", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    /** Asks for a file, merges its content into the database and refreshes the tables. */
    private fun loadData() {
        val chooser = JFileChooser()
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return

        try {
            val r = services.backup.loadFrom(Path.of(chooser.selectedFile.absolutePath))
            appendLog(
                "Loaded ${chooser.selectedFile}: channels ${r.channelsAdded} added / ${r.channelsSkipped} existing, " +
                        "videos ${r.videosAdded} added / ${r.videosSkipped} existing"
            )
            channelsPanel.refreshChannels()
            videosPanel.refresh()
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, e.message ?: "Invalid file.", "Validation Error", JOptionPane.WARNING_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message ?: "Failed to load data.", "Error", JOptionPane.ERROR_MESSAGE)
        }
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
