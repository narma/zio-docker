package st.alzo.zdocker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.command.{CreateContainerCmd, ListContainersCmd, LogContainerCmd}
import com.github.dockerjava.api.exception.{NotFoundException, NotModifiedException}
import com.github.dockerjava.api.model.{Container, Frame, StreamType}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import zio.stream.{Stream, ZStream}
import zio.{Chunk, Task, TaskManaged, ZIO, ZManaged}
import scala.jdk.CollectionConverters.*

import java.nio.charset.StandardCharsets.UTF_8

class ZDockerLive(dockerClient: DockerClient) extends ZDocker {

  private def pullImage(image: ImageName): Task[Unit] = {
    // use async features?
    Task.attemptBlocking(
      dockerClient
        .pullImageCmd(image.name)
        .withTag(image.tag)
        .start()
        .awaitCompletion()
    )
  }

  private def startContainer(containerId: String) = {
    ZManaged.acquireReleaseWith(
      Task.attemptBlocking {
        dockerClient.startContainerCmd(containerId).exec()
      }.unit
    ) { _ =>
      Task
        .attemptBlocking {
          dockerClient.stopContainerCmd(containerId).exec()
        }
        .catchSome { case _: NotModifiedException => // already stopped
          Task.unit
        }
        .orDie
    }
  }

  private def createContainer(
      image: ImageName,
      pullStrategy: PullStrategy,
      setup: CreateContainerCmd => CreateContainerCmd
  ): TaskManaged[String] = {
    ZManaged.acquireReleaseWith {
      val createContainer = Task.attemptBlocking {
        setup(
          dockerClient
            .createContainerCmd(image.fullName)
        ).exec()
      }

      val startEffect =
        if (pullStrategy.always || (image.tag == "latest" && pullStrategy.ifLatest))
          pullImage(image) *> createContainer
        else
          createContainer
            .catchSome {
              case _: NotFoundException if pullStrategy.ifNotExists =>
                pullImage(image) *> createContainer
            }

      startEffect.map(_.getId)
    } { containerId =>
      Task.attemptBlocking(dockerClient.removeContainerCmd(containerId).exec()).orDie
    }
  }

  /** @param image
    *   - parsed ImageName.
    * @param pullStrategy
    *   default - pull only if image not exists.
    * @param setup
    *   Configure docker container for example: _.withHostConfig(hostConfig) .withCmd(cmd) .withStopTimeout(3)
    * @return
    */
  override def startManagedContainer(
      image: ImageName,
      pullStrategy: PullStrategy = PullStrategy(),
      setup: CreateContainerCmd => CreateContainerCmd
  ): TaskManaged[String] =
    createContainer(image, pullStrategy, setup).tap(startContainer)

  override def collectLogs(
      containerId: String,
      setup: LogContainerCmd => LogContainerCmd
  ): Stream[Throwable, LogEntry] = {

    val logStream = Stream
      .async[Any, Throwable, LogEntry] { cb =>
        val logStream =
          setup(
            dockerClient
              .logContainerCmd(containerId)
              .withStdOut(true)
              .withStdErr(true)
              .withFollowStream(true)
          )

        logStream
          .exec(new Adapter[Frame] {
            override def onError(throwable: Throwable): Unit =
              cb(ZIO.fail(Some(throwable)))
            override def onComplete(): Unit = cb(ZIO.fail(None))

            override def onNext(frame: Frame): Unit = {
              val payload = new String(frame.getPayload, UTF_8).stripLineEnd
              frame.getStreamType match {
                case StreamType.STDOUT =>
                  val log = LogEntry.Out(payload)
                  cb(Task.succeed(Chunk.single(log)))
                case StreamType.STDERR =>
                  val log = LogEntry.Err(payload)
                  cb(Task.succeed(Chunk.single(log)))
                case _ => ()
              }
            }
          })
      }
    Stream.blocking(logStream)
  }

  override def getFileFromContainer(containerId: String, path: String): Stream[Throwable, Byte] =
    for {
      is <- Stream.fromZIO(Task.attemptBlocking(dockerClient.copyArchiveFromContainerCmd(containerId, path).exec()))
      tar = new TarArchiveInputStream(is)
      b <- ZStream.fromInputStreamZIO(ZIO.attemptBlockingIO(tar.getNextTarEntry).as(tar))
    } yield b


  override def listContainers(setup: ListContainersCmd => ListContainersCmd): Task[Chunk[Container]] =
    Task.attemptBlocking(Chunk.fromIterable(setup(dockerClient.listContainersCmd()).exec().asScala))
}
