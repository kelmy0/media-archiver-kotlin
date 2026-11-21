package com.kelmy.mediaarchiver.ui

import com.kelmy.mediaarchiver.config.ConfigProvider
import com.kelmy.mediaarchiver.schemas.AppSettingsSchema
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

class SettingsDialog(
    owner: Frame,
    private val configProvider: ConfigProvider,
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

    private fun loadCurrentValues() {
        val current = configProvider.getConfigs()
        downloadPathField.text = current.path.downloadPath
        cookiesPathField.text = current.path.cookiesPath
        cooldownMinutesField.text = current.cooldownMinutes.toString()
        enableOnStartupCheck.isSelected = current.scheduler.enableOnStartup
        intervalMinutesField.text = current.scheduler.intervalMinutes.toString()
    }

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

    private fun buildButtonsPanel(): JPanel {
        val saveButton = JButton("Save").apply { addActionListener { onSave() } }
        val cancelButton = JButton("Cancel").apply { addActionListener { dispose() } }

        return JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
            add(cancelButton)
            add(saveButton)
        }
    }

    private fun onSave() {
        try {
            val cooldown = cooldownMinutesField.text.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Cooldown must be a valid integer.")
            val interval = intervalMinutesField.text.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Interval must be a valid integer.")

            require(cooldown >= 0) { "Cooldown cannot be negative." }
            require(interval >= 0) { "Interval cannot be negative." }

            val updatedConfig = AppSettingsSchema(
                path = AppSettingsSchema.PathConfig(
                    downloadPath = downloadPathField.text.trim(),
                    cookiesPath = cookiesPathField.text.trim()
                ),
                cooldownMinutes = cooldown,
                scheduler = AppSettingsSchema.SchedulerConfig(
                    enableOnStartup = enableOnStartupCheck.isSelected,
                    intervalMinutes = interval
                )
            )

            configProvider.saveConfig(updatedConfig)
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