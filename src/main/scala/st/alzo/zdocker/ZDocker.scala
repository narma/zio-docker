package st.alzo.zdocker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.{CreateContainerCmd, ListContainersCmd, LogContainerCmd}
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.core.{DockerClientConfig, DockerClientImpl}
import com.github.dockerjava.transport.DockerHttpClient
import zio.stream.{Stream, ZStream}
import zio.{Chunk, Task, TaskManaged, ZIO, ZLayer, ZManaged}

/** High-level API for docker-java.
  *
  * API is subject for change.
  */
trait ZDocker {

  def startManagedContainer(
      image: ImageName,
      pullStrategy: PullStrategy = PullStrategy(),
      setup: CreateContainerCmd => CreateContainerCmd = identity
  ): TaskManaged[String]

  def collectLogs(
      containerId: String,
      setup: LogContainerCmd => LogContainerCmd = identity
  ): Stream[Throwable, LogEntry]

  def getFileFromContainer(containerId: String, path: String): Stream[Throwable, Byte]

  def listContainers(setup: ListContainersCmd => ListContainersCmd = identity): Task[Chunk[Container]]
}

object ZDocker {

  def fromDockerHttpClient(clientConfig: DockerClientConfig)(
      dockerHttpClient: DockerClientConfig => Task[DockerHttpClient]
  ): TaskManaged[ZDocker] =
    ZManaged
      .fromAutoCloseable(dockerHttpClient(clientConfig))
      .flatMap { dockerHttpClient =>
        ZManaged.fromAutoCloseable(ZIO.attempt {
          DockerClientImpl.getInstance(clientConfig, dockerHttpClient)
        })
      }
      .map(fromDockerClient)

  def fromDockerClient(dockerClient: DockerClient): ZDocker = new ZDockerLive(dockerClient)

  def make: ZLayer[DockerHttpClient & DockerClientConfig, Throwable, ZDocker] = (for {
    httpClient <- ZIO.service[DockerHttpClient].toManaged
    clientConfig <- ZIO.service[DockerClientConfig].toManaged
    zdocker <- fromDockerHttpClient(clientConfig)(_ => Task.succeed(httpClient))
  } yield zdocker).toLayer


  /** Create & start container and stop & remove it on release.
    * @param image
    *   parsed image name see [[ImageName]]
    * @param pullStrategy
    *   see [[PullStrategy]], default pull only if image not exists.
    * @param setup
    *   configure start container cmd, for example: _.withHostConfig(hostConfig) .withCmd(cmd) .withStopTimeout(3)
    * @return
    *   containerId
    */
  def startManagedContainer(
      image: ImageName,
      pullStrategy: PullStrategy = PullStrategy(),
      setup: CreateContainerCmd => CreateContainerCmd = identity
  ): ZManaged[ZDocker, Throwable, String] =
    ZManaged.serviceWithManaged(_.startManagedContainer(image, pullStrategy, setup))

  /** Get logs from running container
    * @param containerId
    *   valid containerId
    * @param setup
    *   Advanced configure LogContainerCmd. Default configuration is following stdout and stderr.
    */
  def collectLogs(
      containerId: String,
      setup: LogContainerCmd => LogContainerCmd = identity
  ): ZStream[ZDocker, Throwable, LogEntry] =
    ZStream.serviceWithStream(_.collectLogs(containerId, setup))

  /** Grab file from running container
    */
  def getFileFromContainer(containerId: String, path: String): ZStream[ZDocker, Throwable, Byte] =
    ZStream.serviceWithStream(_.getFileFromContainer(containerId, path))

  def listContainers(
      setup: ListContainersCmd => ListContainersCmd = identity
  ): ZIO[ZDocker, Throwable, Chunk[Container]] =
    ZIO.serviceWithZIO(_.listContainers(setup))
}
