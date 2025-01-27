package com.bartovapps.gpstriprec.core.files.kml

import android.content.res.Resources
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.core.files.path_provider.PathProvider
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

@Singleton
class KmlManager @Inject constructor(// File xmlFile = new File("/sdcard/route.kml");
    private val resources: Resources,
    private val pathProvider: PathProvider,
    private val saxBuilder: SAXBuilder
) {
    private var doc: Document? = null
    private var rootNode: Element? = null

    fun openRawDocument() {
        try {
            resources.openRawResource(R.raw.trip_raw).use {
                doc = saxBuilder.build(it) as Document
                rootNode = doc?.rootElement
            }
        } catch (e: JDOMException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun updateStartPoint(point: String?) {
        var startPoint: Element? = null
        val docElement = rootNode?.getChild(
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
        @Suppress("unused") val coordinated = startPoint!!.getChild(
            "coordinates",
            Namespace.getNamespace(KML_NS)
        ).setText(point)
    }

    fun updateEndPoint(point: String?) {
        var startPoint: Element? = null
        val docElement = rootNode!!.getChild(
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

    private fun updateRouteMarks(route: String) {
        var lineString: Element? = null
        val docElement = rootNode!!.getChild(
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
                    // Log.i(LOG_TAG, "Placemark : " + node.getAttribute("id"));
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

    fun updateTrip(locations: List<String>): String? {
        val route = StringBuilder()

        var mapFile: String? = null

        if (locations.size > 1) {
            updateStartPoint(locations[0])
            updateEndPoint(locations[locations.size - 1])
            for (mark in locations) {
                route.append(mark + "\n")
            }
            updateRouteMarks(route.toString())
            mapFile = writeFile()
        }

        return mapFile
    }

    fun updateTripLatLng(latlngs: List<LatLng>): String? {
        val route = StringBuilder()
        var mapFile: String? = null


        if (latlngs.size > 1) {
            val startPoint = latlngs[0]
            val stopPoint = latlngs[latlngs.size - 1]

            updateStartPoint(startPoint.longitude.toString() + "," + startPoint.latitude)
            updateEndPoint(stopPoint.longitude.toString() + "," + stopPoint.latitude)
            for (latlng in latlngs) {
                route.append(latlng.longitude.toString() + "," + latlng.latitude + "\n")
            }
            updateRouteMarks(route.toString())
            mapFile = writeFile()
        }
        return mapFile
    }

    private fun writeFile(): String {
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