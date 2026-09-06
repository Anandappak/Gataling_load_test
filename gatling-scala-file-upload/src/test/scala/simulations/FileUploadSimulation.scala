package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
* Gatling performance test for the File Upload API.
*
* Test flow:
  * 1. Configure the application base URL.
* 2. Configure HTTP protocol settings.
  * 3. Upload a text file using multipart/form-data.
  * 4. Gradually increase users and maintain a constant load.
  * 5. Validate failure rate and response-time performance.
*/
class FileUploadSimulation extends Simulation {

  // Read the base URL from the command line or use localhost by default.
  private val baseUrl =
    System.getProperty(
      "baseUrl",
      "http://localhost:8080"
    )

  // Configure common HTTP settings for all requests.
  private val httpProtocol =
    http
      .baseUrl(baseUrl)
      .acceptHeader("application/json")
      .userAgentHeader("Gatling-Scala")

  // Define the file used for the upload request.
  private val uploadFile =
    RawFileBody("files/test-file.txt")

  // Define the user scenario for uploading a file.
  private val uploadScenario =
    scenario("File Upload API")
      .exec(
        http("Upload File")
          .post("/api/v1/files/upload")
          .header("Accept", "application/json")

          // Send the file as multipart/form-data.
          .bodyPart(
            RawFileBodyPart("file", "files/test-file.txt")
              .fileName("test-file.txt")
              .contentType("text/plain")
          )
          .asMultipartForm

          // Verify that the API returns HTTP 200.
          .check(
            status.is(200)
          )
      )

  // Define the load pattern and performance assertions.
  setUp(
    uploadScenario.inject(
      // Gradually increase the load to 10 users over 10 seconds.
      rampUsers(10).during(10.seconds),

      // Maintain 5 new users per second for 30 seconds.
      constantUsersPerSec(5).during(30.seconds)
    )
  )
    .protocols(httpProtocol)

    // Validate overall test results.
    .assertions(
      // Less than 5% of requests should fail.
      global
        .failedRequests
        .percent
        .lt(5.0),

      // 95th percentile response time should be below 1 second.
      global
        .responseTime
        .percentile3
        .lt(1000)
    )
}
