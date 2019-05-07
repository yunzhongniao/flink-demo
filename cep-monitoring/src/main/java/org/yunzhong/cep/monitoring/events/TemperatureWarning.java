package org.yunzhong.cep.monitoring.events;

public class TemperatureWarning {

    private int rackID;
    private double averageTemperature;

    public TemperatureWarning(int rackID, double averageTemperature) {
        this.rackID = rackID;
        this.averageTemperature = averageTemperature;
    }

    public TemperatureWarning() {
        this(-1, -1);
    }

    public int getRackID() {
        return rackID;
    }

    public void setRackID(int rackID) {
        this.rackID = rackID;
    }

    public double getAverageTemperature() {
        return averageTemperature;
    }

    public void setAverageTemperature(double averageTemperature) {
        this.averageTemperature = averageTemperature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TemperatureWarning) {
            TemperatureWarning other = (TemperatureWarning) obj;

            return rackID == other.rackID && averageTemperature == other.averageTemperature;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 41 * rackID + Double.hashCode(averageTemperature);
    }

    @Override
    public String toString() {
        return "TemperatureWarning(" + getRackID() + ", " + averageTemperature + ")";
    }
}
