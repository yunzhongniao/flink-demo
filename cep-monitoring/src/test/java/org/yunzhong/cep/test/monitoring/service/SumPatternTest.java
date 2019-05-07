package org.yunzhong.cep.test.monitoring.service;

import java.util.List;
import java.util.Map;

import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.flink.util.CollectionUtil;
import org.apache.flink.util.Collector;
import org.junit.Test;
import org.yunzhong.cep.monitoring.events.MonitoringEvent;
import org.yunzhong.cep.monitoring.events.PowerEvent;
import org.yunzhong.cep.monitoring.sources.MonitoringEventSourceSingle;

public class SumPatternTest extends AbstractTestBase {

	public static class StartCondition extends SimpleCondition<MonitoringEvent> {
		private static final long serialVersionUID = 6809574900728721544L;

		@Override
		public boolean filter(MonitoringEvent value) throws Exception {
			if (value instanceof PowerEvent) {
				return true;
			}
			return false;
		}
	}

	public static class FlatSelectFunction implements PatternFlatSelectFunction<MonitoringEvent, String> {
		private static final long serialVersionUID = -7932710864411648825L;

		@Override
		public void flatSelect(Map<String, List<MonitoringEvent>> pattern, Collector<String> out) throws Exception {
			List<MonitoringEvent> events = pattern.get("start");
			List<MonitoringEvent> followEvents = pattern.get("follow");
			List<MonitoringEvent> endEvents = pattern.get("end");
			if (!CollectionUtil.isNullOrEmpty(events)) {
				if (events.size() != 1) {
					throw new Exception("start event count error :" + events.size());
				}
				StringBuilder builder = new StringBuilder("select value:");
				for (MonitoringEvent event : events) {
					builder.append(
							"[ number :" + event.getNumber() + ",class:" + event.getClass().getSimpleName() + "]");
				}
				builder.append(" | following| ");
				int followCount = 0;
				int numSum = 0;
				if (!CollectionUtil.isNullOrEmpty(followEvents)) {
					for (MonitoringEvent event : followEvents) {
						followCount++;
						numSum += event.getNumber();
						builder.append(
								"[ number :" + event.getNumber() + ",class:" + event.getClass().getSimpleName() + "]");
					}
				}
				builder.append(" | end | ");
				if (!CollectionUtil.isNullOrEmpty(endEvents)) {
					for (MonitoringEvent event : endEvents) {
						builder.append(
								"[ number :" + event.getNumber() + ",class:" + event.getClass().getSimpleName() + "]");
					}
				}
				if (followCount > 1 && numSum > 5) {
					builder.append(" | followcount:" + followCount + " numsum:" + numSum);
				}
				out.collect(builder.toString());
			} else {
				throw new Exception("no start event.");
			}
		}
	}

	public static class FollowCondition extends IterativeCondition<MonitoringEvent> {
		private static final long serialVersionUID = 927480647512740544L;

		@Override
		public boolean filter(MonitoringEvent value, Context<MonitoringEvent> ctx) throws Exception {
			MonitoringEvent start = ctx.getEventsForPattern("start").iterator().next();
			if (start.getRackID() == value.getRackID()) {
				return true;
			}
			return false;
		}

	}

	public static class EndCondition extends IterativeCondition<MonitoringEvent> {
		private static final long serialVersionUID = 3957769588781274515L;

		@Override
		public boolean filter(MonitoringEvent value, Context<MonitoringEvent> ctx) throws Exception {
			MonitoringEvent start = ctx.getEventsForPattern("start").iterator().next();
			if (value.getTimestamp().getTime() - start.getTimestamp().getTime() > 700) {
				return true;
			}
			return false;
		}
	}

	@Test
	public void testUtilC() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<MonitoringEvent> stream = env.addSource(new MonitoringEventSourceSingle());
		stream.setParallelism(1);
		Pattern<MonitoringEvent, MonitoringEvent> endPattern = Pattern.<MonitoringEvent>begin("end")
				.where(new EndCondition()).times(0, 1);
		Pattern<MonitoringEvent, MonitoringEvent> followPattern = Pattern.<MonitoringEvent>begin("follow")
				.where(new FollowCondition()).oneOrMore().greedy().followedBy(endPattern);
		Pattern<MonitoringEvent, MonitoringEvent> pattern = Pattern
				.<MonitoringEvent>begin("start", AfterMatchSkipStrategy.skipPastLastEvent())
				.subtype(MonitoringEvent.class).where(new StartCondition()).followedBy(followPattern)
				.within(Time.milliseconds(1000L));
		PatternStream<MonitoringEvent> pStream = CEP.pattern(stream, pattern);
		pStream.flatSelect(new FlatSelectFunction()).setParallelism(1).print();
		env.execute();
	}
}
