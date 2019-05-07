package org.yunzhong.cep.monitoring.sources;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.yunzhong.cep.monitoring.events.MonitoringEvent;
import org.yunzhong.cep.monitoring.events.PowerEvent;
import org.yunzhong.cep.monitoring.events.TemperatureEvent;

public class MonitoringEventSourceSingle extends RichSourceFunction<MonitoringEvent> {
	private static final long serialVersionUID = -5050063805425066525L;

	private static final int MAX_RACK_ID = 10;
	private static final long PAUSE = 100;
	private static final double TEMPERATURE_RATIO = 0.5;
	private static final double POWER_STD = 10;
	private static final double POWER_MEAN = 100;
	private static final double TEMP_STD = 20;
	private static final double TEMP_MEAN = 80;

	private boolean running = true;

	private final int maxRackId;

	private final long pause;

	private final double temperatureRatio;

	private final double powerStd;

	private final double powerMean;

	private final double temperatureStd;

	private final double temperatureMean;

	private int shard;

	private int offset;
	private int count = 100;
	private int number = 0;

	public MonitoringEventSourceSingle() {
		this(MAX_RACK_ID, PAUSE, TEMPERATURE_RATIO, POWER_STD, POWER_MEAN, TEMP_STD, TEMP_MEAN);
	}

	public MonitoringEventSourceSingle(int maxRackId, long pause, double temperatureRatio, double powerStd,
			double powerMean, double temperatureStd, double temperatureMean) {
		this.maxRackId = maxRackId;
		this.pause = pause;
		this.temperatureRatio = temperatureRatio;
		this.powerMean = powerMean;
		this.powerStd = powerStd;
		this.temperatureMean = temperatureMean;
		this.temperatureStd = temperatureStd;
	}

	@Override
	public void open(Configuration configuration) {
		int numberTasks = getRuntimeContext().getNumberOfParallelSubtasks();
		int index = getRuntimeContext().getIndexOfThisSubtask();

		offset = (int) ((double) maxRackId / numberTasks * index);
		shard = (int) ((double) maxRackId / numberTasks * (index + 1)) - offset;
	}

	public void run(SourceContext<MonitoringEvent> sourceContext) throws Exception {
		while (running) {
			MonitoringEvent monitoringEvent;

			final ThreadLocalRandom random = ThreadLocalRandom.current();

			if (shard > 0) {
				int rackId = random.nextInt(shard) + offset;

				if (random.nextDouble() >= temperatureRatio) {
					double power = random.nextGaussian() * powerStd + powerMean;
					monitoringEvent = new PowerEvent(rackId, power);
					monitoringEvent.setNumber(number++);
				} else {
					double temperature = random.nextGaussian() * temperatureStd + temperatureMean;
					monitoringEvent = new TemperatureEvent(rackId, temperature);
					monitoringEvent.setNumber(number++);
				}
				monitoringEvent.setTimestamp(new Date());
				sourceContext.collect(monitoringEvent);
			}
			count--;
			if (count <= 0) {
				running = false;
			}
			Thread.sleep(pause);
		}
	}

	public void cancel() {
		running = false;
	}
}
