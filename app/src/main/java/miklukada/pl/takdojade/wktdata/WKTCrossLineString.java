package miklukada.pl.takdojade.wktdata;

/**
 * Created by Konrad on 2016-03-03.
 */
public class WKTCrossLineString {

    private final String TAG = getClass().getSimpleName();

    private WKTLinestring intersection;

    private WKTLinestring firstWay;

    private WKTLinestring secondWay;

    public WKTCrossLineString(WKTLinestring intersection, WKTLinestring firstWay, WKTLinestring secondWay) {
        this.intersection = intersection;
        this.firstWay = firstWay;
        this.secondWay = secondWay;
    }


    public WKTLinestring getIntersection() {
        return intersection;
    }

    public void setIntersection(WKTLinestring intersection) {
        this.intersection = intersection;
    }



    public WKTLinestring getFirstWay() {
        return firstWay;
    }

    public void setFirstWay(WKTLinestring firstWay) {
        this.firstWay = firstWay;
    }

    public WKTLinestring getSecondWay() {
        return secondWay;
    }

    public void setSecondWay(WKTLinestring secondWay) {
        this.secondWay = secondWay;
    }
}
