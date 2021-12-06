package st.alzo.zio.docker.testutils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import st.alzo.zio.docker.{ZDocker, ZDockerLive}
import zio.*

object ZioDockerLayers {

  private def makeClient(): TaskManaged[DockerClient] = {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    val builder = new ApacheDockerHttpClient.Builder()

    ZManaged
      .fromAutoCloseable(
        Task(
          builder
            .connectionTimeout(2.seconds)
            .responseTimeout(5.seconds)
            .dockerHost(config.getDockerHost)
            .build()
        )
      )
      .flatMap { dockerHttpClient =>
        ZManaged.fromAutoCloseable(Task {
          DockerClientImpl.getInstance(config, dockerHttpClient)
        })
      }

  }

  def default: ZLayer[Any, Throwable, ZDocker] = ZLayer.fromManaged(
    makeClient().map { client =>
      new ZDockerLive(client)
    }
  )


}
