package com.dunihuliapps.myglidingassistnat.domain.files.kml
import android.content.res.Resources
import com.dunihuliapps.myglidingassistant.R
import com.dunihuliapps.myglidingassistnat.domain.files.path_provider.PathProvider
import com.google.android.gms.maps.model.LatLng
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface KmlManager {
    fun generateKmlFromLocationList(locations: List<LatLng>) : String?
    fun getLocationsFromKml(kmlPath: String) : List<LatLng>
}

@Singleton
class KmlManagerImpl @Inject constructor(// File xmlFile = new File("/sdcard/route.kml");
    private val resources: Resources,
    private val pathProvider: PathProvider,
    private val saxBuilder: SAXBuilder,
    private val kmlParser: KmlParser,
) : KmlManager {

    private fun openRawDocument() : Document? {
        try {
            return resources.openRawResource(R.raw.trip_raw).use {
                saxBuilder.build(it) as Document
            }
        } catch (e: JDOMException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun updateStartPoint(rootNode: Element, point: String?) {
        var startPoint: Element? = null
        val docElement = rootNode.getChild("Document", Namespace.getNamespace(KML_NS))
        try {
            if (docElement != null) {
                val placemarks = docElement.getChildren(
                    "Placemark",
                    Namespace.getNamespace(KML_NS)
                )

                for (node in placemarks) {
                    if (node.getAttribute("id").intValue == 1) {
                        startPoint = node.getChild(
                            "Point",
                            Namespace.getNamespace(KML_NS)
                        )
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.message
        }
        @Suppress("unused") val coordinated = startPoint?.getChild(
            "coordinates",
            Namespace.getNamespace(KML_NS)
        )?.setText(point)
    }

    private fun updateEndPoint(rootNode: Element, point: String?) {
        var startPoint: Element? = null
        val docElement = rootNode.getChild("Document", Namespace.getNamespace(KML_NS))
        try {
            if (docElement != null) {
                val placemarks = docElement.getChildren("Placemark", Namespace.getNamespace(KML_NS))
                for (node in placemarks) {
                    if (node.getAttribute("id").intValue == 2) {
                        startPoint = node.getChild(
                            "Point",
                            Namespace.getNamespace(KML_NS)
                        )
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.message
        }
        @Suppress("unused") val coordinated = startPoint?.getChild(
            "coordinates",
            Namespace.getNamespace(KML_NS)
        )?.setText(point)
    }

    private fun updateRouteMarks(rootNode: Element, route: String) {
        var lineString: Element? = null
        val docElement = rootNode.getChild(
            "Document",
            Namespace.getNamespace(KML_NS)
        )
        try {
            if (docElement != null) {
                val placemarks = docElement.getChildren(
                    "Placemark",
                    Namespace.getNamespace(KML_NS)
                )

                for (node in placemarks) {
                    if (node.getAttribute("id").value == "route") {
                        lineString = node.getChild(
                            "LineString",
                            Namespace.getNamespace(KML_NS)
                        )
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.message
        }

        @Suppress("unused") val coordinated = lineString?.getChild(
            "coordinates",
            Namespace.getNamespace(KML_NS)
        )?.setText(route)

        // Log.i(LOG_TAG, "end point coordinates: " + coordinated.getValue());
    }

    override fun generateKmlFromLocationList(locations: List<LatLng>): String? {
        val d = openRawDocument()
        return d?.let {  doc ->
            val rootNode = doc.rootElement
            val route = StringBuilder()
            if (locations.size > 1) {
                val startPoint = locations.first()
                val stopPoint = locations.last()

                updateStartPoint(rootNode ,startPoint.longitude.toString() + "," + startPoint.latitude)
                updateEndPoint(rootNode, stopPoint.longitude.toString() + "," + stopPoint.latitude)
                for (latlng in locations) {
                    route.append(latlng.longitude.toString() + "," + latlng.latitude + "\n")
                }
                updateRouteMarks(rootNode, route.toString())
                writeFile(doc)
            } else {
                null
            }
        }


    }


    override fun getLocationsFromKml(kmlPath: String): List<LatLng> {
        return kmlParser.parsKmlString(kmlPath)
    }

    private fun writeFile(doc: Document): String {
        val timestamp = System.currentTimeMillis()

        val fileName = "/trip_$timestamp.kml"
        val kmlFile = File(pathProvider.provideTripKmlFilesPath(), fileName)

        val xmlOutput = XMLOutputter()
        xmlOutput.format = Format.getPrettyFormat()
        val fr = FileWriter(kmlFile)
        try {
            xmlOutput.output(doc, fr)
        } catch (e: IOException) {
            e.printStackTrace()
            return "Can't save trip file"
        }finally {
            fr.flush()
            fr.close()
        }
        return kmlFile.toString()
    }

    companion object {
        //	private static final String LOG_TAG = "GPS RECORDER";
        private const val KML_NS = "http://www.opengis.net/kml/2.2"
    }
}