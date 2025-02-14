import org.freeplane.api.Node
import org.freeplane.api.NodeChangeListener
import org.freeplane.api.NodeChanged
import org.freeplane.core.util.TextUtils

import static org.freeplane.api.NodeChanged.ChangedElement

class TerminalNodeChangeListenerForModified implements NodeChangeListener {
    public static canReact = true

    void nodeChanged(NodeChanged event) {
        if (!canReact)
            return
        canReact = false
        switch (event.changedElement) {
            case [ChangedElement.TEXT, ChangedElement.NOTE, ChangedElement.ICON]:
                def tmux_session_name = event.node.attributes.get("tmux session name")
                def cmd = "bin/send-keys.sh " + tmux_session_name + " " + event.node.plainText
                Runtime.runtime.exec(cmd)
        }
        canReact = true
    }
}
