//import java.io.Serializable;
//
//@SuppressWarnings("serial")
//public class Trip implements Serializable {
//
//    private String date;
//    private double avSpeed = 0.0;
//    private float distance = 0;
//    private String kmlFile;
//    private long tripId;
//    private long duration = 0;
//    private long moveTime = 0;
//    private long stopTime = 0;
//    private double move_average_speed = 0;
//
//    private String startAddress;
//    private String stopAddress;
//    private double maxSpeed = 0;
//    private double maxAltitude = 0;
//    private String name;
//    private String imageFileName;
//
//    public Trip(){
//    }
//
//    public Trip(String map, String date, float distance, double speed){
//        setKml(map);
//        setDate(date);
//        setDistance(distance);
//        setSpeed(speed);
//    }
//
//    public void setSpeed(double speed) {
//        this.avSpeed = speed;
//    }
//
//    public double getSpeed(){
//        return this.avSpeed;
//    }
//
//    public void setDistance(float distance) {
//        this.distance = distance;
//    }
//
//    public float getDistance(){
//        return this.distance;
//    }
//
//
//
//    public void setDate(String date) {
//        this.date = date;
//    }
//
//    public String getDate(){
//        return this.date;
//    }
//
//    public void setKml(String kmlStr){
//        this.kmlFile = kmlStr;
//    }
//    public String getKml(){
//        return this.kmlFile;
//    }
//
//    public void setId(Long id){
//        this.tripId = id;
//    }
//
//    public long getId(){
//        return this.tripId;
//    }
//
//    public void setDuration(long duration){
//        this.duration = duration;
//    }
//
//    public long getDuration(){
//        return this.duration;
//    }
//
//    public void setStartAddress(String address){
//        this.startAddress = address;
//    }
//
//    public String getStartAddress(){
//        return this.startAddress;
//    }
//
//    public void setStopAddress(String address){
//        this.stopAddress = address;
//    }
//
//    public String getStopAddress(){
//        return this.stopAddress;
//    }
//
//    public void setMaxSpeed(double speed){
//        this.maxSpeed = speed;
//    }
//
//    public double getMaxSpeed(){
//        return this.maxSpeed;
//    }
//
//    public void setMaxAlt(double altitude){
//        this.maxAltitude = altitude;
//    }
//
//    public double getMaxAlt(){
//        return this.maxAltitude;
//    }
//
//    public void setTripName(String name){
//        this.name = name;
//    }
//
//    public String getTripName(){
//        return this.name;
//    }
//
//    public void setImageFileName(String filename){
//        this.imageFileName = filename;
//    }
//
//    public String getImageFileName(){
//        return this.imageFileName;
//    }
//
//    public double getMove_average_speed() {
//        return move_average_speed;
//    }
//
//    public void setMove_average_speed(double move_average_speed) {
//        this.move_average_speed = move_average_speed;
//    }
//
//
//    public long getMoveTime() {
//        return moveTime;
//    }
//
//    public void setMoveTime(long moveTime) {
//        this.moveTime = moveTime;
//    }
//
//    public long getStopTime() {
//        return stopTime;
//    }
//
//    public void setStopTime(long stopTime) {
//        this.stopTime = stopTime;
//    }
//
//    @Override
//    public String toString() {
////		String distance = (this.distance < 1000 ? String.format("%.1f", this.distance) + " m": String.format("%.2f", this.distance/1000) + " Km") ;
//        return "Date: " + this.date + "\nDuration: " + ((duration > 60 ? (int)duration/60 + ":" + (int)duration%60: (int)duration) + (duration > 60 ? " min" : " sec"));
//    }
//
//}
