package org.yunzhong.cep.monitoring.sources;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.yunzhong.cep.monitoring.events.MonitoringEvent;
import org.yunzhong.cep.monitoring.events.PowerEvent;
import org.yunzhong.cep.monitoring.events.TemperatureEvent;

public class MonitoringEventSource extends RichParallelSourceFunction<MonitoringEvent> {
	private static final long serialVersionUID = -1646025546418243651L;

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
	private int count = 10;

	public MonitoringEventSource(int maxRackId, long pause, double temperatureRatio, double powerStd, double powerMean,
			double temperatureStd, double temperatureMean) {
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
				} else {
					double temperature = random.nextGaussian() * temperatureStd + temperatureMean;
					monitoringEvent = new TemperatureEvent(rackId, temperature);
				}

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
