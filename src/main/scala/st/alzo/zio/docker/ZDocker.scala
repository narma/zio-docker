package st.alzo.zio.docker

import com.github.dockerjava.api.command.{CreateContainerCmd, ListContainersCmd, LogContainerCmd}
import com.github.dockerjava.api.model.Container
import zio.stream.{Stream, ZStream}
import zio.{Chunk, Task, TaskManaged, ZIO, ZManaged}

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

  /** Create & start container and stop & remove it on release.
    * @param image
    *   parsed image name see [[st.alzo.zio.docker.ImageName]]
    * @param pullStrategy
    *   see [[st.alzo.zio.docker.PullStrategy]]
    * @param setup
    *   configure start container cmd, see [[com.github.dockerjava.api.command.CreateContainerCmd]]
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
    *   Advanced configure LogContainerCmd. Default configuration is following stdout and stderr see
    *   [[com.github.dockerjava.api.command.LogContainerCmd]] for advance.
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

  def listContainers(setup: ListContainersCmd => ListContainersCmd = identity): ZIO[ZDocker, Throwable, Chunk[Container]] =
    ZIO.serviceWithZIO(_.listContainers(setup))
}
