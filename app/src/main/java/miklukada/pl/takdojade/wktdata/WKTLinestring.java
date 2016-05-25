package miklukada.pl.takdojade.wktdata;

/**
 * Created by Konrad on 2016-03-03.
 */
public class WKTLinestring {

    private String LINESTRING;

    private String name;

    public WKTLinestring(String LINESTRING, String name) {
        this.LINESTRING = LINESTRING;
        this.name = name;
    }

    public String getLINESTRING() {
        return LINESTRING;
    }

    public void setLINESTRING(String LINESTRING) {
        this.LINESTRING = LINESTRING;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
