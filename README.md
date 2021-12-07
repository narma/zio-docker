# zio-docker

| CI ZIO2 | CI ZIO1 | 
|     --- |     --- | 
| ![CI Badge ZIO2] | ![CI Badge ZIO1] |

High-level zio interface to [docker-java][docker-java]

zio-docker is currently available for Scala 2.13 and 3.1.1-RC1 both for ZIO 1.x and 2.x

## How to use
1. Add zio-docker

For ZIO 2.x (current for ZIO 2.0.0-M6-2)
```scala
"st.alzo" %% "zio-docker" % "2.0.1-M1"
```

For ZIO 1.x
```scala
"st.alzo" %% "zio-docker" % "1.0.1"
```

2. Choose docker-java transport
from [official documentation](https://github.com/docker-java/docker-java/blob/master/docs/transports.md)
and add this transport dependency to your project:

For example apache http transport:

```scala
"com.github.docker-java" % "docker-java-transport-httpclient5" % dockerJavaVersion
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


[CI Badge ZIO2]: https://github.com/narma/zio-docker/workflows/CI/badge.svg?branch=main
[CI Badge ZIO1]: https://github.com/narma/zio-docker/workflows/CI/badge.svg?branch=zio1
[docker-java]: https://github.com/docker-java/docker-java
