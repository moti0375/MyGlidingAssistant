//package com.bartovapps.gpstriprec.kmlhleper;
//
//import android.content.Context;
//
//import com.google.android.gms.maps.model.LatLng;
//
//import org.jdom2.Document;
//import org.jdom2.Element;
//import org.jdom2.JDOMException;
//import org.jdom2.Namespace;
//import org.jdom2.input.SAXBuilder;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class com.bartovapps.gpstriprec.core.trip_manager.KmlParser {
//
//    public static final int KML_OPENED = 0;
//    public static final int FAIL_TO_OPEN_KML = 1;
////	private static final String LOG_TAG = "KML Parser";
//
//    private static final String KML_NS = "http://www.opengis.net/kml/2.2";
//    File xmlFile;
//
//    Context context;
//    private FileInputStream fis;
//    private SAXBuilder builder;
//    private Document doc;
//    private Element rootNode;
//    private List<LatLng> locations;
//
//    public com.bartovapps.gpstriprec.core.trip_manager.KmlParser(String fileName) {
//        this.xmlFile = new File(fileName);
//        builder = new SAXBuilder();
//    }
//
//    public int openTripKml() {
//        int status = KML_OPENED;
//        try {
//            fis = new FileInputStream(xmlFile);
//            doc = builder.build(fis);
//            rootNode = doc.getRootElement();
////			Log.i(LOG_TAG, "Document Opened: root element: " + rootNode.getName());
//        } catch (JDOMException e) {
//            e.printStackTrace();
//            status = FAIL_TO_OPEN_KML;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            status = FAIL_TO_OPEN_KML;
//        } catch (IOException e) {
//            e.printStackTrace();
//            status = FAIL_TO_OPEN_KML;
//        }
//
//        return status;
//    }
//
//    public List<LatLng> getTripLocations() {
//        Element LineString = null;
//        Element docElement = null;
//        try{
//            docElement  = rootNode.getChild("Document",
//                    Namespace.getNamespace(KML_NS));
//
//        }catch (NullPointerException e){
//            e.printStackTrace();
//        }
//
//        String coordinatesStr;
//        try {
//
//            if (docElement != null) {
//                List<Element> placemarks = docElement.getChildren("Placemark",
//                        Namespace.getNamespace(KML_NS));
////				Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
//
//                for (Element node : placemarks) {
////					Log.i(LOG_TAG, "Placemark : " + node.getAttribute("id"));
//                    if (node.getAttribute("id").getValue().equals("route")) {
//                        LineString = node.getChild("LineString",
//                                Namespace.getNamespace(KML_NS));
//                        break;
//                    }
//                }
//            }
//           coordinatesStr = LineString.getChild("coordinates",
//                    Namespace.getNamespace(KML_NS)).getValue();
//            locations = coorStrToLocaitons(coordinatesStr);
//
//        } catch (Exception e) {
//            e.getMessage();
//        }
//
//        return locations;
//    }
//
//
//    public static List<LatLng> getLocationsFromKml(String kmlFileName) {
//        Element LineString = null;
//        SAXBuilder builder;
//        Document doc;
//        Element rootNode;
//        List<LatLng> locations = null;
//
//
//        builder = new SAXBuilder();
//
//        FileInputStream fis = null;
//        String coordinatesStr;
//
//        try {
//            fis = new FileInputStream(kmlFileName);
//            doc = builder.build(fis);
//            rootNode = doc.getRootElement();
//            Element docElement = rootNode.getChild("Document",
//                    Namespace.getNamespace(KML_NS));
//
//            if (docElement != null) {
//                List<Element> placemarks = docElement.getChildren("Placemark",
//                        Namespace.getNamespace(KML_NS));
////				Log.i(LOG_TAG, "number of placemarks " + placemarks.size());
//
//                for (Element node : placemarks) {
////					Log.i(LOG_TAG, "Placemark : " + node.getAttribute("id"));
//                    if (node.getAttribute("id").getValue().equals("route")) {
//                        LineString = node.getChild("LineString",
//                                Namespace.getNamespace(KML_NS));
//                        break;
//                    }
//                }
//            }
//
//            coordinatesStr = LineString.getChild("coordinates",
//                    Namespace.getNamespace(KML_NS)).getValue();
//            locations = coorStrToLocaitons(coordinatesStr);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (JDOMException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (NullPointerException e){
//            e.printStackTrace();
//        }
//        finally {
//            try {
//                fis.close();
//            } catch (NullPointerException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//       return locations;
//    }
//
//    public static List<LatLng> coorStrToLocaitons(String coordinatesStr) {
//        List<LatLng> list = new ArrayList<LatLng>();
//
//        double lat;
//        double lng;
//
//        String[] array = coordinatesStr.split("\n");
//        for (String temp : array) {
//            lat = Double.parseDouble(temp.split(",")[1].trim());
//            lng = Double.parseDouble(temp.split(",")[0].trim());
//            list.add(new LatLng(lat, lng));
//        }
//
//        return list;
//    }
//
//    public int getLocationsSize() {
//        return locations.size();
//    }
//
//    public LatLng getLoaction(int index) {
//        if (index > locations.size()) {
//            return locations.get(locations.size() - 1);
//        }
//
//        return locations.get(index);
//    }
//
//    public LatLng getfirstLocation() {
//        return locations.get(0);
//    }
//
//    public LatLng getLastLocation() {
//        return locations.get(locations.size() - 1);
//    }
//
//    public void closeKml() {
//        try {
//            fis.reset();
//            fis.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//}
