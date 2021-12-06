package st.alzo.zdocker

import com.github.dockerjava.api.exception.NotFoundException
import st.alzo.zdocker.testutils.ZioDockerLayers
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.timeout

object ZDockerSpec extends DefaultRunnableSpec {
  def spec = suite("ZioDockerSpec")(
    test("start managed container and collect logs") {
      for {
        img <- ImageName("hello-world")
        output <- ZDocker.startManagedContainer(img).use { containerId =>
          ZDocker
            .collectLogs(containerId)
            .filterNot(_.value.isBlank)
            .take(1)
            .runCollect
        }
      } yield assertTrue(output.map(_.value) == Chunk("Hello from Docker!"))
    } @@ timeout(10.seconds),
    suite("getFileFromContainer")(
      test("works with existing file") {
        for {
          img <- ImageName("alpine:3.14.3")
          output <- ZDocker.startManagedContainer(img).use { containerId =>
            ZDocker
              .getFileFromContainer(containerId, "/etc/alpine-release")
              .runCollect
          }
        } yield assertTrue(output == Chunk.fromArray("3.14.3\n".getBytes))
      } @@ timeout(10.seconds),
      test("not existing file should raise error") {
        val eff = for {
          img <- ImageName("alpine:3.14.3")
          output <- ZDocker.startManagedContainer(img).use { containerId =>
            ZDocker
              .getFileFromContainer(containerId, "/etc/404filenotfound")
              .runCollect
          }
        } yield output
        assertM(eff.either)(isLeft(isSubtype[NotFoundException](anything)))
      } @@ timeout(10.seconds),
      test("not existing container should raise error") {
        val eff = for {
          output <- ZDocker
            .getFileFromContainer("404container-id", "/etc/404filenotfound")
            .runCollect
        } yield output
        assertM(eff.either)(isLeft(isSubtype[NotFoundException](anything)))
      } @@ timeout(10.seconds)
    )
  ).provideCustom(ZioDockerLayers.default.mapError(TestFailure.die))

}
