package org.yunzhong.cep.test.monitoring.basic;

import java.util.List;
import java.util.Map;

import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.cep.PatternStream;
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
import org.yunzhong.cep.monitoring.events.PowerEvent;
import org.yunzhong.cep.monitoring.sources.PowerEventSourceSingle;

public class FollowByPatternTest extends AbstractTestBase {

	public static class EventCondition extends SimpleCondition<PowerEvent> {
		private static final long serialVersionUID = 6809574900728721544L;

		@Override
		public boolean filter(PowerEvent value) throws Exception {
			return value.getVoltage() > 10D;
		}
	}

	public static class FlatSelectFunction implements PatternFlatSelectFunction<PowerEvent, String> {
		private static final long serialVersionUID = -7932710864411648825L;

		@Override
		public void flatSelect(Map<String, List<PowerEvent>> pattern, Collector<String> out) throws Exception {
			List<PowerEvent> events = pattern.get("start");
			List<PowerEvent> followEvents = pattern.get("follow");
			if (!CollectionUtil.isNullOrEmpty(events)) {
				if (events.size() != 1) {
					throw new Exception("start event count error :" + events.size());
				}
				PowerEvent startEvent = events.get(0);
				StringBuilder builder = new StringBuilder("select value:");
				for (PowerEvent event : events) {
					builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",class:"
							+ event.getClass().getSimpleName() + "]");
				}
				builder.append(" | following| ");
				if (!CollectionUtil.isNullOrEmpty(followEvents)) {
					for (PowerEvent event : followEvents) {
						if (Double.compare(event.getVoltage(), startEvent.getVoltage()) <= 0) {
							throw new Exception("value error.start is bigger than follow.");
						}
						builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",class:"
								+ event.getClass().getSimpleName() + "]");
					}
				}
				out.collect(builder.toString());
			} else {
				throw new Exception("no start event.");
			}
		}
	}

	public static class FollowCondition extends IterativeCondition<PowerEvent> {
		private static final long serialVersionUID = -4418346283781995457L;

		@Override
		public boolean filter(PowerEvent value, Context<PowerEvent> ctx) throws Exception {
			PowerEvent startEvent = ctx.getEventsForPattern("start").iterator().next();
			if (Double.compare(value.getVoltage(), startEvent.getVoltage()) > 0) {
				return true;
			}
			return false;
		}
	}

	@Test
	public void testBegin() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<PowerEvent> stream = env.addSource(new PowerEventSourceSingle());
		Pattern<PowerEvent, PowerEvent> pattern = Pattern.<PowerEvent>begin("start").subtype(PowerEvent.class)
				.where(new EventCondition()).followedBy("follow").where(new FollowCondition()).oneOrMore().greedy()
				.within(Time.seconds(1L));
		PatternStream<PowerEvent> pStream = CEP.pattern(stream, pattern);
		pStream.flatSelect(new FlatSelectFunction()).setParallelism(1).print();
		env.execute();
	}
}
