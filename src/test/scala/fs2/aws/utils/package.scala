package fs2
package aws

import java.io.{ByteArrayInputStream, InputStream}

import cats.effect.{Effect, IO}
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal.Internal.S3Client
import org.apache.http.client.methods.HttpRequestBase

package object utils {
  val testJson : String =
    """{"test": 1}
      |{"test": 2}
      |{"test": 3}
      |{"test": 4}
      |{"test": 5}
      |{"test": 6}
      |{"test": 7}
      |{"test": 8}""".stripMargin
  val testAvro : String =
    """
      | hey
    """.stripMargin
  val testJsonGzip : String = """??[Zfile.json?V*I-.Q?R0?媆?????Hl$?)?
                                        |                                     ?m?Ķ??x?`""".stripMargin

  val s3TestClient: S3Client[IO] = new S3Client[IO] {
    override def getObjectContent(getObjectRequest: GetObjectRequest)(implicit e: Effect[IO]) : IO[S3ObjectInputStream] = getObjectRequest match {
      case goe: GetObjectRequest => {
        val is : InputStream = {
          val fileContent: String =
            if(goe.getBucketName == "json" && goe.getKey == "file")
              testJson
            else if (goe.getBucketName == "avro" && goe.getKey == "file")
              testAvro
            else if (goe.getBucketName == "jsongzip" && goe.getKey == "file")
              testJsonGzip
            else
              throw new AmazonS3Exception("File not found")
          goe.getRange match {
            case Array(x, y) =>
              if (x >= fileContent.length) throw new AmazonS3Exception("Invalid range")
              else if (y > fileContent.length) new ByteArrayInputStream(fileContent.substring(x.toInt, fileContent.length).getBytes)
              else new ByteArrayInputStream(fileContent.substring(x.toInt, y.toInt).getBytes)
          }
        }

        IO {
          Thread.sleep(500)  // simulate a call to S3
          new S3ObjectInputStream(is, new HttpRequestBase { def getMethod = "" })
        }
      }
      case _ => throw new SdkClientException("Invalid GetObjectRequest")
    }
  }
}
