# zio-docker

High-level zio interface to [docker-java](https://github.com/docker-java/docker-java)

zio-docker is currently available for Scala 2.13 and 3.1.1-RC1

## How to use

Choose docker-java transport
from [official documentation](https://github.com/docker-java/docker-java/blob/master/docs/transports.md)
and add this transport dependency to your project.:
For example

```scala
"com.github.docker-java" % "docker-java-transport-httpclient5" % dockerJavaVersion
```

Add zio-docker

```scala
"st.alzo" %% "zio-docker" % "0.1.1"
```

### Create a ZDocker instance

For example with apache httpclient transport
```scala
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import st.alzo.zdocker.{ZDocker, ZDockerLive}
import zio.*

def makeClient(): TaskManaged[ZDocker] = {

  ZDocker.fromDockerHttpClient(
    DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  )(config => Task {
    new ApacheDockerHttpClient.Builder()
      .connectionTimeout(2.seconds)
      .responseTimeout(5.seconds)
      .dockerHost(config.getDockerHost)
      .build()
  })
}
```

### Usage

```scala
import zio.*
import st.alzo.zdocker.*
import zio.Runtime.default as rt


def helloWorld(): Task[Unit] =
  ImageName("hello-world").flatMap { img =>
    ZDocker.startManagedContainer(img).use { containerId =>
      ZDocker.collectLogs(containerId)
        .map(_.value)
        .tap(s => ZIO.debug(s))
        .runDrain
    }
  }.provideSome(makeClient().toLayer)

rt.unsafeRun(helloWorld())


```



