@Grab('io.cloudevents:cloudevents-core:2.4.0')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.0')
@Grab('com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0')

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



//println "MINDWM_MANAGER_HTTP_ENDPOINT: ${MINDWM_MANAGER_HTTP_ENDPOINT}"

class MindwmManager {

  String endpoint = System.getenv('MINDWM_MANAGER_HTTP_ENDPOINT') ?: 'http://localhost:38080'

  CloudEvent sendCloudEventHTTP(CloudEvent event) {  
    def post = new URL(endpoint + "/freeplane").openConnection();
    def mapper = new ObjectMapper()
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    mapper.registerModule(new JavaTimeModule())
    def message = mapper.writeValueAsString(event)
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(message.getBytes("UTF-8"));
    def postRC = post.getResponseCode();
    println(postRC);
    if (postRC.equals(200)) {
        println(post.getInputStream().getText());
    }
  } 
  CloudEvent sendCloudEvent(String source, String subject, String type, String dataPayload) {  

    def objectMapper = new ObjectMapper()

    def map = objectMapper.readValue(dataPayload, Map)

    CloudEventData cloudEventData = PojoCloudEventData.wrap(map) { obj ->
      objectMapper.writeValueAsBytes(obj)
    }

    /*
    println cloudEventData
    */
  
    def cloudEvent = new CloudEventBuilder()
        .withId(UUID.randomUUID().toString())
        .withDataContentType('application/json')
        .withSource(URI.create(source))
        .withType(type)
        .withTime(OffsetDateTime.now())
        .withSubject(subject)
        .withData("application/json", cloudEventData)
        .build()
    return sendCloudEventHTTP(cloudEvent)
  }
} 
