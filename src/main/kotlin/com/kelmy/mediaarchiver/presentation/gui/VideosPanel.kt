package com.kelmy.mediaarchiver.presentation.gui

import com.kelmy.mediaarchiver.library.application.ChannelService
import com.kelmy.mediaarchiver.library.application.VideoService
import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.Video
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

/** Item of the channel combo boxes: shows a readable name while keeping the [Channel]. */
internal class ChannelChoice(val channel: Channel) {
    override fun toString(): String = "${channel.name} (${channel.platform})"
}

/**
 * Panel with the videos of one channel (chosen in a combo box) and the associate / edit / delete / refresh
 * actions. The list comes from the channel aggregate ([ChannelService.findWithVideos]).
 *
 * Database access never happens on the Swing thread for listing: it is loaded on a background thread.
 *
 * @property owner          parent frame, used to position the dialogs.
 * @property channelService source of the channels (and of their videos).
 * @property videoService   create / edit / delete of videos.
 * @property onLog          receives human-readable messages about what the user did.
 */
internal class VideosPanel(
    private val owner: Frame,
    private val channelService: ChannelService,
    private val videoService: VideoService,
    private val onLog: (String) -> Unit
) : JPanel(BorderLayout(8, 8)) {
    private val columns = arrayOf("Video ID", "Title", "Source URL", "File")
    private val tableModel = object : DefaultTableModel(columns, 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JTable(tableModel).apply { setSelectionMode(ListSelectionModel.SINGLE_SELECTION) }
    private val channelCombo = JComboBox<ChannelChoice>()
    private val associateButton = JButton("Associate Video")
    private val editButton = JButton("Edit Video")
    private val deleteButton = JButton("Delete Video")
    private val refreshButton = JButton("Refresh")

    private var channels: List<Channel> = emptyList()
    private var currentVideos: List<Video> = emptyList()
    private var loadingChannels = false

    init {
        border = BorderFactory.createEmptyBorder(8, 10, 10, 10)

        val top = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(JLabel("Channel:"))
            add(channelCombo)
            add(associateButton)
            add(editButton)
            add(deleteButton)
            add(refreshButton)
        }
        add(top, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)

        channelCombo.addActionListener { if (!loadingChannels) refreshVideos() }
        associateButton.addActionListener { openAssociateDialog() }
        editButton.addActionListener { openEditDialog() }
        deleteButton.addActionListener { deleteSelectedVideo() }
        refreshButton.addActionListener { refresh() }

        refresh()
    }

    /** Reloads the channels (keeping the selected one when possible) and then the videos. */
    fun refresh() {
        Thread({
            try {
                val list = channelService.list()
                SwingUtilities.invokeLater {
                    val selectedId = selectedChannel()?.id
                    loadingChannels = true
                    channels = list
                    channelCombo.removeAllItems()
                    list.forEach { channelCombo.addItem(ChannelChoice(it)) }
                    list.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }?.let { channelCombo.selectedIndex = it }
                    loadingChannels = false
                    refreshVideos()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { showError(e.message ?: "Failed to load channels.") }
            }
        }, "Videos-ChannelLoader").start()
    }

    /** Enables/disables every action (used while an archiving routine is running). */
    fun setCrudEnabled(enabled: Boolean) {
        listOf(associateButton, editButton, deleteButton, refreshButton, channelCombo).forEach { it.isEnabled = enabled }
    }

    private fun selectedChannel(): Channel? = (channelCombo.selectedItem as? ChannelChoice)?.channel

    /** Loads the videos of the selected channel on a background thread and refills the table. */
    private fun refreshVideos() {
        val channel = selectedChannel()
        if (channel == null) {
            tableModel.rowCount = 0
            currentVideos = emptyList()
            return
        }
        Thread({
            try {
                val videos = channelService.findWithVideos(channel.id)?.videos ?: emptyList()
                SwingUtilities.invokeLater {
                    currentVideos = videos
                    tableModel.rowCount = 0
                    videos.forEach { tableModel.addRow(arrayOf<Any>(it.videoId, it.title, it.sourceUrl, it.filePath)) }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { showError(e.message ?: "Failed to load videos.") }
            }
        }, "Videos-Loader").start()
    }

    private fun selectedVideo(): Video? {
        val row = table.selectedRow
        if (row < 0 || row >= currentVideos.size) {
            JOptionPane.showMessageDialog(this, "Select a video in the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE)
            return null
        }
        return currentVideos[row]
    }

    /** Opens the dialog that associates an existing video to a channel. */
    private fun openAssociateDialog() {
        if (channels.isEmpty()) {
            showError("Register a channel first.")
            return
        }
        VideoDialog(owner, "Associate Video", channels, selectedChannel()?.id, null) { form ->
            val video = videoService.associate(form.channelId, form.title, form.url, form.filePath)
            onLog("Associated video '${video.title}' to channel #${video.channelId}")
            refresh()
        }.isVisible = true
    }

    /** Opens the dialog that edits the title and the channel of the selected video. */
    private fun openEditDialog() {
        val selected = selectedVideo() ?: return
        VideoDialog(owner, "Edit Video", channels, selected.channelId, selected) { form ->
            val updated = videoService.edit(selected.platform, selected.videoId, form.title, form.channelId)
            onLog("Updated video '${updated.title}' (channel #${updated.channelId})")
            refresh()
        }.isVisible = true
    }

    private fun deleteSelectedVideo() {
        val selected = selectedVideo() ?: return
        val confirm = JOptionPane.showConfirmDialog(
            this, "Delete the record of '${selected.title}'? The file on disk is kept.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        )
        if (confirm != JOptionPane.YES_OPTION) return

        if (videoService.delete(selected.platform, selected.videoId)) {
            onLog("Deleted video '${selected.title}'")
            refreshVideos()
        } else {
            showError("Video could not be deleted.")
        }
    }

    private fun showError(message: String) =
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
}

/** What the user typed in a [VideoDialog]. */
internal data class VideoForm(val channelId: Int, val title: String, val url: String, val filePath: String)

/**
 * Modal dialog used both to associate a new video and to edit an existing one.
 *
 * When editing, the URL and the file are shown but locked: the URL is the video's identity.
 * It only builds a [VideoForm] and hands it to [onConfirm]; persisting is the caller's job.
 *
 * @param initial video being edited, or `null` when associating a new one.
 */
private class VideoDialog(
    owner: Frame,
    dialogTitle: String,
    channels: List<Channel>,
    selectedChannelId: Int?,
    initial: Video?,
    private val onConfirm: (VideoForm) -> Unit
) : JDialog(owner, dialogTitle, true) {
    private val channelCombo = JComboBox(channels.map { ChannelChoice(it) }.toTypedArray()).apply {
        channels.indexOfFirst { it.id == selectedChannelId }.takeIf { it >= 0 }?.let { selectedIndex = it }
    }
    private val titleField = JTextField(initial?.title ?: "")
    private val urlField = JTextField(initial?.sourceUrl ?: "").apply { isEditable = initial == null }
    private val fileField = JTextField(initial?.filePath ?: "").apply { isEditable = initial == null }

    init {
        setSize(520, 260)
        setLocationRelativeTo(owner)
        layout = BorderLayout(10, 10)

        val browse = JButton("...").apply {
            isEnabled = initial == null
            addActionListener {
                val chooser = JFileChooser()
                if (chooser.showOpenDialog(this@VideoDialog) == JFileChooser.APPROVE_OPTION) {
                    fileField.text = chooser.selectedFile.absolutePath
                }
            }
        }
        val filePanel = JPanel(BorderLayout(4, 0)).apply {
            add(fileField, BorderLayout.CENTER)
            add(browse, BorderLayout.EAST)
        }

        val form = JPanel(GridLayout(4, 2, 8, 8)).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 6, 12)
            add(JLabel("Channel:")); add(channelCombo)
            add(JLabel("Title:")); add(titleField)
            add(JLabel("Video URL:")); add(urlField)
            add(JLabel("File:")); add(filePanel)
        }
        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(JButton("Cancel").apply { addActionListener { dispose() } })
            add(JButton("Save").apply { addActionListener { onSave() } })
        }
        add(form, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
    }

    /** Hands the form to the caller. Validation errors are shown and the dialog stays open. */
    private fun onSave() {
        try {
            val channel = (channelCombo.selectedItem as? ChannelChoice)?.channel
                ?: throw IllegalArgumentException("Select a channel.")
            onConfirm(VideoForm(channel.id, titleField.text.trim(), urlField.text.trim(), fileField.text.trim()))
            dispose()
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, e.message ?: "Invalid video data.", "Validation Error", JOptionPane.WARNING_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message ?: "Failed to save video.", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}
