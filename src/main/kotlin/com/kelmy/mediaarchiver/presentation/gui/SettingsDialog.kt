package com.kelmy.mediaarchiver.presentation.gui

import com.kelmy.mediaarchiver.settings.domain.model.AppSettings
import com.kelmy.mediaarchiver.settings.domain.model.PathSettings
import com.kelmy.mediaarchiver.settings.domain.model.SchedulerSettings
import com.kelmy.mediaarchiver.settings.domain.port.SettingsStore
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Modal dialog to view and edit the [com.kelmy.mediaarchiver.settings.domain.model.AppSettings].
 *
 * Values are validated by the domain model itself when the settings object is built, so
 * invalid input is reported to the user instead of being saved.
 *
 * @property settingsStore    source of the current values and destination of the new ones.
 * @property onSettingsSaved  called after the settings were saved successfully.
 */
internal class SettingsDialog(
    owner: Frame,
    private val settingsStore: SettingsStore,
    private val onSettingsSaved: () -> Unit = {}
) : JDialog(owner, "Settings", true) {
    private val downloadPathField = JTextField()
    private val cookiesPathField = JTextField()
    private val cooldownMinutesField = JTextField()
    private val enableOnStartupCheck = JCheckBox("Enable on startup")
    private val intervalMinutesField = JTextField()

    init {
        setSize(480, 300)
        setLocationRelativeTo(owner)
        layout = BorderLayout(10, 10)

        loadCurrentValues()
        add(buildFormPanel(), BorderLayout.CENTER)
        add(buildButtonsPanel(), BorderLayout.SOUTH)
    }

    /** Fills the form with the values currently stored. */
    private fun loadCurrentValues() {
        val current = settingsStore.current()
        downloadPathField.text = current.path.downloadPath
        cookiesPathField.text = current.path.cookiesPath
        cooldownMinutesField.text = current.cooldownMinutes.toString()
        enableOnStartupCheck.isSelected = current.scheduler.enableOnStartup
        intervalMinutesField.text = current.scheduler.intervalMinutes.toString()
    }

    /** Builds the label/field grid. */
    private fun buildFormPanel(): JPanel =
        JPanel(GridLayout(5, 2, 8, 8)).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 6, 12)
            add(JLabel("Download Path:"))
            add(downloadPathField)
            add(JLabel("Cookies Path:"))
            add(cookiesPathField)
            add(JLabel("Cooldown (minutes):"))
            add(cooldownMinutesField)
            add(JLabel("Scheduler Startup:"))
            add(enableOnStartupCheck)
            add(JLabel("Scheduler Interval (minutes):"))
            add(intervalMinutesField)
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

    /**
     * Validates the form and saves it. Validation problems (not a number, negative value, blank path)
     * are shown as a warning and keep the dialog open; any other failure is shown as an error.
     */
    private fun onSave() {
        try {
            val cooldown = cooldownMinutesField.text.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Cooldown must be a valid integer.")
            val interval = intervalMinutesField.text.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Interval must be a valid integer.")

            require(cooldown >= 0) { "Cooldown cannot be negative." }
            require(interval >= 0) { "Interval cannot be negative." }

            val updatedConfig = AppSettings(
                path = PathSettings(
                    downloadPath = downloadPathField.text.trim(),
                    cookiesPath = cookiesPathField.text.trim()
                ),
                cooldownMinutes = cooldown,
                scheduler = SchedulerSettings(
                    enableOnStartup = enableOnStartupCheck.isSelected,
                    intervalMinutes = interval
                )
            )

            settingsStore.save(updatedConfig)
            onSettingsSaved()
            dispose()
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(
                this,
                e.message ?: "Invalid configuration values.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                e.message ?: "Failed to save settings.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
