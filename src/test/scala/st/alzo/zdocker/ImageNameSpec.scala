package st.alzo.zdocker

import zio.test.*
import zio.test.Assertion.*

object ImageNameSpec extends DefaultRunnableSpec {
  def spec = suite("ImageNameSpec")(
    test("correctly parsed image without tag and repo") {
      ImageName("ubuntu").map(out => assertTrue(out == ImageName("ubuntu", "latest")))
    },
    test("correctly parsed image with tag") {
      ImageName("ubuntu:16").map(out => assertTrue(out == ImageName("ubuntu", "16")))
    },
    test("correctly parsed image with repo and tag") {
      ImageName("repo.ubuntu.com/ubuntu:16").map(out =>
        assertTrue(out == ImageName("ubuntu", "16", Some("repo.ubuntu.com")))
      )
    },
    test("correctly parsed image with repo only") {
      ImageName("repo.ubuntu.com/ubuntu").map(out =>
        assertTrue(out == ImageName("ubuntu", "latest", Some("repo.ubuntu.com")))
      )
    },
    test("fail on invalid name") {
      assertM(ImageName("ubuntu:1.12:13").either)(isLeft(isSubtype[ImageName.ImageNameInvalid](anything)))
    }
  )
}
