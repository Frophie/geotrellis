package geotrellis.feature.json

import org.scalatest._
import geotrellis.feature._

import spray.json.DefaultJsonProtocol

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._
import spray.httpx.SprayJsonSupport


class FeatureFormatsSpec extends FlatSpec with ShouldMatchers with SprayJsonSupport {
  import DefaultJsonProtocol._
  import FeatureFormats._

  val pointFeature = PointFeature(Point(6.0,1.2), 123)
  val lineFeature = LineFeature(Line(Point(1,2) :: Point(1,3) :: Nil), 321)
  //val listOfFeatures: Seq[Feature[Geometry, Int]] = List(pointFeature, lineFeature)

  def jsonBody(blob: String) =
    HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), string = blob)

  "Feature" should "work single point feature" in {
    val body =
      jsonBody(
        """{
          |  "type": "Feature",
          |  "geometry": {
          |    "type": "Point",
          |    "coordinates": [6.0, 1.2]
          |  },
          |  "properties": 123
          |}""".stripMargin
      )

    marshal(pointFeature) should equal (Right(body))
    body.as[PointFeature[Int]] should equal(Right(pointFeature))
  }

  it should "work single line feature" in {
    val body =
      jsonBody(
        """{
          |  "type": "Feature",
          |  "geometry": {
          |    "type": "LineString",
          |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
          |  },
          |  "properties": 321
          |}""".stripMargin
      )

    marshal(lineFeature) should equal (Right(body))
    body.as[LineFeature[Int]] should equal(Right(lineFeature))
  }

  it should "knows how to heterogeneous collection" in {
    val body =
     jsonBody(
       """{
         |  "type": "FeatureCollection",
         |  "features": [{
         |    "type": "Feature",
         |    "geometry": {
         |      "type": "Point",
         |      "coordinates": [6.0, 1.2]
         |    },
         |    "properties": 123
         |  }, {
         |    "type": "Feature",
         |    "geometry": {
         |      "type": "LineString",
         |      "coordinates": [[1.0, 2.0], [1.0, 3.0]]
         |    },
         |    "properties": 321
         |  }]
         |}""".stripMargin
     )

    val jsonFeatures = new JsonFeatureCollection()
    jsonFeatures += lineFeature
    jsonFeatures += pointFeature

    marshal(jsonFeatures) should equal (Right(body))

    val fc = body.as[JsonFeatureCollection].right.get
    fc.getAll[PointFeature[Int]] should contain (pointFeature)
    fc.getAll[LineFeature[Int]] should contain (lineFeature)
  }
}
