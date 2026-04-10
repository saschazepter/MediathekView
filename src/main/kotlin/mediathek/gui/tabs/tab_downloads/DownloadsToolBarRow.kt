package mediathek.gui.tabs.tab_downloads

import com.jidesoft.popup.JidePopup
import mediathek.swing.IconUtils
import org.kordamp.ikonli.materialdesign2.MaterialDesignC
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.SwingUtilities

class DownloadsToolBarRow(
    private val primaryToolBar: JToolBar,
    private vararg val overflowToolBars: JToolBar
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
    private val overflowButton = JButton(IconUtils.of(MaterialDesignC.CHEVRON_DOUBLE_RIGHT)).apply {
        toolTipText = "Weitere Download-Optionen"
        isFocusable = false
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        margin = Insets(0, 4, 0, 4)
        addActionListener { showOverflowPopup() }
    }
    private val overflowContent = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    private val overflowPopup = JidePopup().apply {
        contentPane.layout = BorderLayout()
        contentPane.add(overflowContent, BorderLayout.CENTER)
        owner = overflowButton
        isMovable = false
        isResizable = false
        isAttachable = false
        isTransient = true
        isFocusable = true
        isKeepPreviousSize = false
        defaultMoveOperation = JidePopup.HIDE_ON_MOVED
    }
    private var visibleOverflowToolBarCount = overflowToolBars.size
    private var updatePending = false

    init {
        add(primaryToolBar)
        overflowToolBars.forEach(::add)
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateOverflowStateLater()
            }
        })
    }

    override fun doLayout() {
        super.doLayout()
        updateOverflowStateLater()
    }

    private fun updateOverflowStateLater() {
        if (updatePending) {
            return
        }

        updatePending = true
        SwingUtilities.invokeLater {
            updatePending = false
            updateOverflowState()
        }
    }

    private fun updateOverflowState() {
        if (width <= 0) {
            return
        }

        val newVisibleOverflowToolBarCount = calculateVisibleOverflowToolBarCount()
        if (newVisibleOverflowToolBarCount == visibleOverflowToolBarCount) {
            return
        }

        visibleOverflowToolBarCount = newVisibleOverflowToolBarCount
        overflowPopup.hidePopup()
        removeAll()
        add(primaryToolBar)
        visibleOverflowToolBars().forEach(::add)
        if (hiddenOverflowToolBars().isNotEmpty()) {
            add(overflowButton)
        }

        revalidate()
        repaint()
    }

    private fun calculateVisibleOverflowToolBarCount(): Int {
        val fullWidth = primaryToolBar.preferredSize.width +
                overflowToolBars.sumOf { it.preferredSize.width }
        if (fullWidth <= width) {
            return overflowToolBars.size
        }

        var visibleToolBarCount = 0
        var usedWidth = primaryToolBar.preferredSize.width + overflowButton.preferredSize.width
        for (toolbar in overflowToolBars) {
            if (usedWidth + toolbar.preferredSize.width > width) {
                break
            }

            usedWidth += toolbar.preferredSize.width
            visibleToolBarCount++
        }
        return visibleToolBarCount
    }

    private fun showOverflowPopup() {
        overflowContent.removeAll()
        hiddenOverflowToolBars().forEach { toolbar ->
            overflowContent.add(toolbar)
        }
        overflowContent.revalidate()
        overflowContent.repaint()
        overflowPopup.owner = overflowButton
        overflowPopup.showPopup(overflowButton)
    }

    private fun visibleOverflowToolBars(): List<JToolBar> =
        overflowToolBars.take(visibleOverflowToolBarCount)

    private fun hiddenOverflowToolBars(): List<JToolBar> =
        overflowToolBars.drop(visibleOverflowToolBarCount)
}
