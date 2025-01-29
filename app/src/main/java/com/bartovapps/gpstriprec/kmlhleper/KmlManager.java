//package com.bartovapps.gpstriprec.kmlhleper;
//
//
//import static core.trip_manager.TripManagerImpl.TRIPS_DIR;
//
//import android.content.Context;
//import android.util.Log;
//
//import com.bartovapps.gpstriprec.R;
//import com.bartovapps.gpstriprec.utils.Utils;
//import com.google.android.gms.maps.model.LatLng;
//
//import org.jdom2.Document;
//import org.jdom2.Element;
//import org.jdom2.JDOMException;
//import org.jdom2.Namespace;
//import org.jdom2.input.SAXBuilder;
//import org.jdom2.output.Format;
//import org.jdom2.output.XMLOutputter;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//import dagger.hilt.android.qualifiers.ApplicationContext;
//
//@Singleton
//public class com.bartovapps.gpstriprec.core.files.kml.com.bartovapps.gpstriprec.domain.files.kml.KmlManager {
//
////	private static final String LOG_TAG = "GPS RECORDER";
//	private static final String KML_NS = "http://www.opengis.net/kml/2.2";
//	// File xmlFile = new File("/sdcard/route.kml");
//
//	Context context;
//	private InputStream stream;
//	private SAXBuilder builder;
//	private Document doc;
//	private Element rootNode;
//
//	@Inject
//	public com.bartovapps.gpstriprec.core.files.kml.com.bartovapps.gpstriprec.domain.files.kml.KmlManager(@ApplicationContext  Context context) {
//		this.context = context;
//		this.stream = context.getResources().openRawResource(R.raw.trip_raw);
//		builder = new SAXBuilder();
//		// Log.i(LOG_TAG, "KML Helper created");
//	}
//
//	public void openRawDocument() {
//		try {
//			doc = (Document) builder.build(stream);
//			rootNode = doc.getRootElement();
//			// Log.i(LOG_TAG, "Raw Document Opened: root element:" +
//			// rootNode.getName());
//		} catch (JDOMException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void updateStartPoint(String point) {
//		Element startPoint = null;
//		Element docElement = rootNode.getChild("Document",
//				Namespace.getNamespace(KML_NS));
//		try {
//
//			if (docElement != null) {
//				List<Element> placemarks = docElement.getChildren("Placemark",
//						Namespace.getNamespace(KML_NS));
//				// Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
//
//				for (Element node : placemarks) {
//					// Log.i(LOG_TAG, "Placemark : " +
//					// node.getAttribute("id").getIntValue());
//					if (node.getAttribute("id").getIntValue() == 1) {
//						startPoint = node.getChild("Point",
//								Namespace.getNamespace(KML_NS));
//						break;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.getMessage();
//		}
//		@SuppressWarnings("unused")
//		Element coordinated = startPoint.getChild("coordinates",
//				Namespace.getNamespace(KML_NS)).setText(point);
//		// Log.i(LOG_TAG, "start point coordinates: " + coordinated.getValue());
//
//	}
//
//	public void updateEndPoint(String point) {
//		Element startPoint = null;
//		Element docElement = rootNode.getChild("Document",
//				Namespace.getNamespace(KML_NS));
//		try {
//
//			if (docElement != null) {
//				List<Element> placemarks = docElement.getChildren("Placemark",
//						Namespace.getNamespace(KML_NS));
//				// Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
//
//				for (Element node : placemarks) {
//					// Log.i(LOG_TAG, "Placemark : " +
//					// node.getAttribute("id").getIntValue());
//					if (node.getAttribute("id").getIntValue() == 2) {
//						startPoint = node.getChild("Point",
//								Namespace.getNamespace(KML_NS));
//						break;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.getMessage();
//		}
//		@SuppressWarnings("unused")
//		Element coordinated = startPoint.getChild("coordinates",
//				Namespace.getNamespace(KML_NS)).setText(point);
//
//		// Log.i(LOG_TAG, "end point coordinates: " + coordinated.getValue());
//
//	}
//
//	private void updateRouteMarks(String route) {
//		Element LineString = null;
//		Element docElement = rootNode.getChild("Document",
//				Namespace.getNamespace(KML_NS));
//		try {
//
//			if (docElement != null) {
//				List<Element> placemarks = docElement.getChildren("Placemark",
//						Namespace.getNamespace(KML_NS));
//				// Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
//
//				for (Element node : placemarks) {
//					// Log.i(LOG_TAG, "Placemark : " + node.getAttribute("id"));
//					if (node.getAttribute("id").getValue().equals("route")) {
//						LineString = node.getChild("LineString",
//								Namespace.getNamespace(KML_NS));
//						break;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.getMessage();
//		}
//
//		@SuppressWarnings("unused")
//		Element coordinated = LineString.getChild("coordinates",
//				Namespace.getNamespace(KML_NS)).setText(route);
//
//		// Log.i(LOG_TAG, "end point coordinates: " + coordinated.getValue());
//
//	}
//
//	public String updateTrip(ArrayList<String> locations) {
//		StringBuilder route = new StringBuilder();
//
//		String mapFile = null;
//
//		if (locations.size() > 1) {
//			updateStartPoint(locations.get(0));
//			updateEndPoint(locations.get(locations.size() - 1));
//			for (String mark : locations) {
//				route.append(mark + "\n");
//			}
//			updateRouteMarks(route.toString());
//			mapFile = writeFile();
//		}
//
//		closeKml();
//		return mapFile;
//	}
//
//	public String updateTripLatLng(List<LatLng> latlngs) {
//		StringBuilder route = new StringBuilder();
//		String mapFile = null;
//
//
//		if (latlngs.size() > 1) {
//			LatLng startPoint = latlngs.get(0);
//			LatLng stopPoint = latlngs.get(latlngs.size() - 1);
//
//			updateStartPoint(startPoint.longitude + "," + startPoint.latitude);
//			updateEndPoint(stopPoint.longitude + "," + stopPoint.latitude);
//			for (LatLng latlng : latlngs) {
//				route.append(latlng.longitude + "," + latlng.latitude + "\n");
//			}
//			// Log.i(LOG_TAG, "route coordinates: ");
//			updateRouteMarks(route.toString());
//			mapFile = writeFile();
//		}
//		return mapFile;
//	}
//
//	private String writeFile() {
//
//		long timestamp = System.currentTimeMillis();
//
//		if (Utils.checkExternalStorageState()) {
//			File fileDir = new File(context.getExternalFilesDir(null).getPath() + TRIPS_DIR );
//
//			if (!fileDir.exists()) {
//				boolean mkdirs = fileDir.mkdirs();
//				Log.i("com.bartovapps.gpstriprec.core.files.kml.com.bartovapps.gpstriprec.domain.files.kml.KmlManager", "fileDir make dir result: " + mkdirs);
//			}
//
//			String fileName = "/trip_" + timestamp + ".kml";
//
//			File kmlFile = new File(fileDir, fileName);
//
//			XMLOutputter xmlOutput = new XMLOutputter();
//			xmlOutput.setFormat(Format.getPrettyFormat());
//
//			try {
//				FileWriter fr = new FileWriter(kmlFile);
//				xmlOutput.output(doc, fr);
//				fr.flush();
//				fr.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//				return "Can't save trip file";
//			}
//
//
//			return kmlFile.toString();
//		} else {
//			return "Can't save trip file";
//		}
//	}
//
//	public void closeKml(){
//		try {
//			stream.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//}
