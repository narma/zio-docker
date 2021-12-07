package st.alzo.zdocker.testutils

import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import st.alzo.zdocker.ZDocker
import zio.*

object ZioDockerLayers {

  private def makeClient(dockerClientConfig: DockerClientConfig): Task[DockerHttpClient] = {
    Task(
      new ApacheDockerHttpClient.Builder()
        .connectionTimeout(2.seconds)
        .responseTimeout(5.seconds)
        .dockerHost(dockerClientConfig.getDockerHost)
        .build()
    )
  }

  def default: ZLayer[Any, Throwable, ZDocker] = ZDocker
    .fromDockerHttpClient(
      DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    )(makeClient)
    .toLayer

}
