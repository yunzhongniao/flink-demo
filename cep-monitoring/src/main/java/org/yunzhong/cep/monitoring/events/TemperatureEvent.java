
package org.yunzhong.cep.monitoring.events;

public class TemperatureEvent extends MonitoringEvent {
    private double temperature;

    public TemperatureEvent(int rackID, double temperature) {
        super(rackID);

        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TemperatureEvent) {
            TemperatureEvent other = (TemperatureEvent) obj;

            return other.canEquals(this) && super.equals(other) && temperature == other.temperature;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 41 * super.hashCode() + Double.hashCode(temperature);
    }

    @Override
    public boolean canEquals(Object obj){
        return obj instanceof TemperatureEvent;
    }

    @Override
    public String toString() {
        return "TemperatureEvent(" + getRackID() + ", " + temperature + ")";
    }
}
