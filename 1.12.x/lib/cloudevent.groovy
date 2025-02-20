@Grab('io.cloudevents:cloudevents-core:2.4.0')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.0')
@Grab('com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0')

import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

import java.time.OffsetDateTime


//import io.cloudevents.core.builder.CloudEventBuilder
//import io.cloudevents.http.HttpMessageFactory
//import org.apache.hc.client5.http.fluent.Request
//import org.apache.hc.core5.http.ContentType

import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule


def sendCloudEvent(CloudEvent event) {
  def post = new URL("http://127.0.0.1:8080/post").openConnection();
  def mapper = new ObjectMapper()
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


// Example of building a CloudEvent in Groovy
def cloudEvent = new CloudEventBuilder()
    .withId("example-id")
    .withSource(URI.create("https://example.com"))
    .withType("example.type")
    .withTime(OffsetDateTime.now())
    .build()

//println "CloudEvent: ${cloudEvent}"

sendCloudEvent(cloudEvent)

