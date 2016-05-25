package miklukada.pl.takdojade.datamodel;

/**
 * Created by Konrad on 2016-02-26.
 */
public class Street {

    private String cityName;
    private String streetName;
    private String sequence;

    public Street(String cityName, String streetName, String sequence) {
        this.cityName = cityName;
        this.streetName = streetName;
        this.sequence = sequence;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}
