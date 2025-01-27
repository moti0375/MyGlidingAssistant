package com.bartovapps.gpstriprec.core.trip_manager
import com.google.android.gms.maps.model.LatLng
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

interface KmlParser{
    fun parsKmlString(kmlPath: String) : List<LatLng>
}

class KmlParserImpl @Inject constructor(private val builder: SAXBuilder) : KmlParser {
    private var doc: Document? = null
    private var rootNode: Element? = null

    override fun parsKmlString(kmlPath: String): List<LatLng> {
        val xmlFile = File(kmlPath)
        openTripKml(xmlFile)
        return extractLocationsFromKml()
    }

    private fun openTripKml(xmlFile: File): Int {
        var status = KML_OPENED
        try {
            val fis = FileInputStream(xmlFile)
            fis.use {
                doc = builder.build(fis)
                rootNode = doc?.rootElement
                extractLocationsFromKml()
            }
        } catch (e: JDOMException) {
            e.printStackTrace()
            status = FAIL_TO_OPEN_KML
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            status = FAIL_TO_OPEN_KML
        } catch (e: IOException) {
            e.printStackTrace()
            status = FAIL_TO_OPEN_KML
        }

        return status
    }

    private fun extractLocationsFromKml(): List<LatLng> {
            var lineString: Element? = null
            var docElement: Element? = null
            try {
                docElement = rootNode?.getChild(
                    "Document",
                    Namespace.getNamespace(KML_NS)
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            val coordinatesStr: String?
            return try {
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
                coordinatesStr = lineString?.getChild(
                    "coordinates",
                    Namespace.getNamespace(KML_NS)
                )?.value
                coordinatesStr?.let {
                    coorStrToLocaitons(it)
                }?: emptyList()
            } catch (e: Exception) {
                e.message
                emptyList()
            }
        }

    companion object {
        const val KML_OPENED: Int = 0
        const val FAIL_TO_OPEN_KML: Int = 1

        //	private static final String LOG_TAG = "KML Parser";
        private const val KML_NS = "http://www.opengis.net/kml/2.2"
        fun getLocationsFromKml(kmlFileName: String): List<LatLng>? {
            var LineString: Element? = null
            val doc: Document
            val rootNode: Element
            var locations: List<LatLng>? = null


            val builder = SAXBuilder()

            var fis: FileInputStream? = null
            val coordinatesStr: String

            try {
                fis = FileInputStream(kmlFileName)
                doc = builder.build(fis)
                rootNode = doc.rootElement
                val docElement = rootNode.getChild(
                    "Document",
                    Namespace.getNamespace(KML_NS)
                )

                if (docElement != null) {
                    val placemarks = docElement.getChildren(
                        "Placemark",
                        Namespace.getNamespace(KML_NS)
                    )

                    //				Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
                    for (node in placemarks) {
//					Log.i(LOG_TAG, "Placemark : " + node.getAttribute("id"));
                        if (node.getAttribute("id").value == "route") {
                            LineString = node.getChild(
                                "LineString",
                                Namespace.getNamespace(KML_NS)
                            )
                            break
                        }
                    }
                }

                coordinatesStr = LineString!!.getChild(
                    "coordinates",
                    Namespace.getNamespace(KML_NS)
                ).value
                locations = coorStrToLocaitons(coordinatesStr)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: JDOMException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } finally {
                try {
                    fis?.close()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return locations
        }

        private fun coorStrToLocaitons(coordinatesStr: String): List<LatLng> {
            val list: MutableList<LatLng> = ArrayList()

            var lat: Double
            var lng: Double

            val array =
                coordinatesStr.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (temp in array) {
                lat = temp.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].trim { it <= ' ' }.toDouble()
                lng = temp.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].trim { it <= ' ' }.toDouble()
                list.add(LatLng(lat, lng))
            }

            return list
        }
    }


}