package org.yunzhong.cep.monitoring.test;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.IngestionTimeExtractor;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.flink.util.Collector;
import org.junit.Test;
import org.yunzhong.cep.monitoring.events.MonitoringEvent;
import org.yunzhong.cep.monitoring.sources.MonitoringEventSourceSingle;

public class SimpleTest extends AbstractTestBase {
	private static final int MAX_RACK_ID = 10;
	private static final long PAUSE = 100;
	private static final double TEMPERATURE_RATIO = 0.5;
	private static final double POWER_STD = 10;
	private static final double POWER_MEAN = 100;
	private static final double TEMP_STD = 20;
	private static final double TEMP_MEAN = 80;

	@Test
	public void test() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		DataStream<MonitoringEvent> inputEventStream = env.addSource(new MonitoringEventSourceSingle(MAX_RACK_ID, PAUSE,
				TEMPERATURE_RATIO, POWER_STD, POWER_MEAN, TEMP_STD, TEMP_MEAN))
				.assignTimestampsAndWatermarks(new IngestionTimeExtractor<>()).setParallelism(1);

		inputEventStream.flatMap(new FlatMapFunction<MonitoringEvent, String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void flatMap(MonitoringEvent value, Collector<String> out) throws Exception {
				StringBuilder builder = new StringBuilder();
				builder.append(value.getRackID());
				builder.append(value.getClass().getName());
				out.collect(builder.toString());
			}
		}).writeAsText("D://temp/flink-test/test.out", WriteMode.OVERWRITE).setParallelism(1);
		env.execute();
		Thread.sleep(1000L);
	}
}
