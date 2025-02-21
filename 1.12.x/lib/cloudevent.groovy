@Grab('io.cloudevents:cloudevents-core:2.4.0')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.0')
@Grab('com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0')

import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule


import java.time.OffsetDateTime

import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.charset.StandardCharsets


def MINDWM_MANAGER_HTTP_ENDPOINT = System.getenv('MINDWM_MANAGER_HTTP_ENDPOINT') ?: 'http://localhost:38080'

println "MINDWM_MANAGER_HTTP_ENDPOINT: ${MINDWM_MANAGER_HTTP_ENDPOINT}"


def sendCloudEvent(endpoint, CloudEvent event) {
  def post = new URL(endpoint + "/post").openConnection();
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

def jsonData = '{"hello": "world"}'.getBytes(StandardCharsets.UTF_8)
def cloudEvent = new CloudEventBuilder()
    .withId(UUID.randomUUID().toString())
    .withDataContentType('application/json')
    .withSource(URI.create("org.mindwm.v2.user.bebebeka.alice"))
    .withType("org.mindwm.v2.mindmap.node.title.change")
    .withTime(OffsetDateTime.now())
    .withSubject("org.mindwm.v2.mindmap.test")
    .withData('{"hello": "world"}')
    .build()

println "CloudEvent: ${cloudEvent}"

sendCloudEvent(MINDWM_MANAGER_HTTP_ENDPOINT, cloudEvent)



