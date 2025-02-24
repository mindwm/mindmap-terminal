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
import java.util.UUID
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.charset.StandardCharsets
import groovy.json.JsonGenerator
import groovy.json.JsonOutput

import static org.freeplane.api.NodeChanged.ChangedElement

import groovy.json.JsonOutput
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import java.awt.Rectangle
import org.freeplane.api.Node
import org.freeplane.plugin.script.proxy.Proxy
import org.freeplane.plugin.script.proxy.ScriptUtils
import javax.swing.SwingUtilities
import org.freeplane.core.ui.components.UITools

class TerminalNodeChangeListenerForModified implements NodeChangeListener {
    public static canReact = true

    def MindwmUser = System.getenv('MINDWM_USER');
    def MindwmHost = System.getenv('MINDWM_HOST'); 
    def Pid = ProcessHandle.current().pid().toString()

    void nodeChanged(NodeChanged event) {
        if (!canReact)
            return
        canReact = false
        switch (event.changedElement) {
            case [ChangedElement.TEXT, ChangedElement.NOTE, ChangedElement.ICON]:
                def node = event.node
                println "${event.node}"
                def manager = new MindwmManager()
                def source = "org.mindwm.v1.${MindwmUser}.${MindwmHost}.freeplane.${Pid}" 
                def type = "org.mindwm.v1.mindmap.node.update.${event.changedElement}" 
                def subject = "org.mindwm.v1.mindmap.${node.map.getName()}.node.${node.getId()}" 

                def nodeData = [
                  title      : node.getText(),
                  details    : node.getDetails(),
                  attributes : node.getAttributes(),
                  notes      : node.getNote()
                ]
                  
                // def g = new JsonGenerator.Options().disableUnicodeEscaping().build()
                // def jsonString = g.toJson(nodeData)

                def String jsonString = ""
                try { 
                    JsonGenerator.Options options = new JsonGenerator.Options()
                    JsonGenerator generator = options.build()
                    jsonString = generator.toJson( nodeData )
                    println JsonOutput.prettyPrint( jsonString )
                } catch( Exception e){
                       println "Unable to save the settings : $e"
                }

                manager.sendCloudEvent(source, subject, type, jsonString)
        }
        canReact = true
    }
}



