package org.yunzhong.cep.monitoring.sources;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.yunzhong.cep.monitoring.events.PowerEvent;

public class PowerEventSourceRollingSingle extends RichSourceFunction<PowerEvent> {
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

	private final double powerStd;

	private final double powerMean;

	private int shard;

	private int offset;
	private int count = 100;
	private int number = 0;
	private double voltage = 0D;

	public PowerEventSourceRollingSingle() {
		this(MAX_RACK_ID, PAUSE, TEMPERATURE_RATIO, POWER_STD, POWER_MEAN, TEMP_STD, TEMP_MEAN);
	}

	public PowerEventSourceRollingSingle(int maxRackId, long pause, double temperatureRatio, double powerStd,
			double powerMean, double temperatureStd, double temperatureMean) {
		this.maxRackId = maxRackId;
		this.pause = pause;
		this.powerMean = powerMean;
		this.powerStd = powerStd;
	}

	@Override
	public void open(Configuration configuration) {
		int numberTasks = getRuntimeContext().getNumberOfParallelSubtasks();
		int index = getRuntimeContext().getIndexOfThisSubtask();

		offset = (int) ((double) maxRackId / numberTasks * index);
		shard = (int) ((double) maxRackId / numberTasks * (index + 1)) - offset;
	}

	public void run(SourceContext<PowerEvent> sourceContext) throws Exception {
		while (running) {
			PowerEvent powerEvent;

			final ThreadLocalRandom random = ThreadLocalRandom.current();

			if (shard > 0) {
				int rackId = random.nextInt(shard) + offset;
				double power = random.nextGaussian() * powerStd + powerMean;
				powerEvent = new PowerEvent(rackId, power);
				powerEvent.setNumber(number++);
				powerEvent.setVoltage(voltage++);
				powerEvent.setType((number % 10) > 5 ? "a" : "b");
				if (voltage >= 20D) {
					voltage = 0D;
				}
				sourceContext.collect(powerEvent);
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
