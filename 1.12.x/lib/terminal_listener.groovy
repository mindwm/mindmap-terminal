@Grab('io.cloudevents:cloudevents-core:2.4.0')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.0')
@Grab('com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0')

import org.freeplane.api.Node
import org.freeplane.api.NodeChangeListener
import org.freeplane.api.NodeChanged
import org.freeplane.core.util.TextUtils
import org.mindwm.manager.Manager

import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

import io.cloudevents.CloudEventData
import io.cloudevents.core.data.PojoCloudEventData
import io.cloudevents.core.data.BytesCloudEventData

import java.time.OffsetDateTime

import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.charset.StandardCharsets


import static org.freeplane.api.NodeChanged.ChangedElement

class TerminalNodeChangeListenerForModified implements NodeChangeListener {
    public static canReact = true

    void nodeChanged(NodeChanged event) {
        if (!canReact)
            return
        canReact = false
        switch (event.changedElement) {
            case [ChangedElement.TEXT, ChangedElement.NOTE, ChangedElement.ICON]:
                def manager = new MindwmManager()
                manager.sendCloudEvent('source', 'subject', 'type', '{"hello": "world"}')
        }
        canReact = true
    }
}
