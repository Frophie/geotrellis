package geotrellis.spark.io.hadoop.cog

import java.net.URI

import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io.{InvalidLayerIdError, AttributeStore, COGLayerType}
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT
import geotrellis.spark.io.cog.vrt.VRT.IndexedSimpleSource
import geotrellis.spark.io.hadoop.{HadoopAttributeStore, HadoopLayerHeader, HdfsUtils, SerializableConfiguration}
import geotrellis.spark.io.index._
import geotrellis.util.ByteReader
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.{ClassTag, classTag}

class HadoopCOGLayerWriter(
  rootPath: String,
  val attributeStore: AttributeStore
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid: GeoTiffReader: ClassTag](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]],
    mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]] = None
  ): Unit = {
    /** Collect VRT into accumulators, to write everything and to collect VRT at the same time */
    val layerId0 = LayerId(layerName, 0)
    def sc = cogLayer.layers.head._2.sparkContext
    val config = SerializableConfiguration(sc.hadoopConfiguration)
    val samplesAccumulator = sc.collectionAccumulator[IndexedSimpleSource](VRT.accumulatorName(layerName))

    implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, config.value)

    def catalogPath = new Path(rootPath)
    try {
      new Path(catalogPath, s"${layerId0.name}/${layerId0.zoom}")
    } catch {
      case e: Exception =>
        throw new InvalidLayerIdError(layerId0).initCause(e)
    }

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = new URI(rootPath),
        layerType = COGLayerType
      )

    attributeStore.writeCOGLayerAttributes(layerId0, header, storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val keyPath =
        (key: K) =>
          s"${catalogPath.toString}/${layerName}/" +
          s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
          s"${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      cogLayer.layers(zoomRange).foreach { case (key, cog) =>
        val path = new Path(s"${keyPath(key)}.${Extension}")

        mergeFunc match {
          case None =>
            HdfsUtils.write(path, config.value) { new GeoTiffWriter(cog, _).write(true) }
            // collect VRT metadata
            (0 until cog.bandCount)
              .map { b =>
                val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
                (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent))
              }
              .foreach(samplesAccumulator.add)

          case Some(_) if !HdfsUtils.pathExists(path, config.value) =>
            HdfsUtils.write(path, config.value) { new GeoTiffWriter(cog, _).write(true) }
            // collect VRT metadata
            (0 until cog.bandCount)
              .map { b =>
                val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
                (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent))
              }
              .foreach(samplesAccumulator.add)

          case Some(merge) if HdfsUtils.pathExists(path, config.value) =>
            val old = GeoTiffReader[V].read(path.toUri(), decompress = false, streaming = true)
            val merged = merge(cog, old)
            HdfsUtils.write(path, config.value) { new GeoTiffWriter(merged, _).write(true) }
            // collect VRT metadata
            (0 until merged.bandCount)
              .map { b =>
                val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
                (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, merged.cols, merged.rows, merged.extent))
              }
              .foreach(samplesAccumulator.add)
        }
      }

      val os =
        vrt
          .fromAccumulator(samplesAccumulator)
          .outputStream

      HdfsUtils.write(
        new Path(s"${catalogPath.toString}/${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}/vrt.xml"),
        config.value
      ) { _.write(os.toByteArray) }

      samplesAccumulator.reset
    }
  }
}

object HadoopCOGLayerWriter {
  def apply(rootPath: Path, attributeStore: AttributeStore): HadoopCOGLayerWriter =
    new HadoopCOGLayerWriter(rootPath.toString, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGLayerWriter =
    apply(rootPath, HadoopAttributeStore(rootPath))
}
