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
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.flink.util.CollectionUtil;
import org.apache.flink.util.Collector;
import org.junit.Test;
import org.yunzhong.cep.monitoring.events.PowerEvent;
import org.yunzhong.cep.monitoring.sources.PowerEventSourceRollingSingle;

public class EndConditionPatternTest extends AbstractTestBase {

	public static class EventCondition extends SimpleCondition<PowerEvent> {
		private static final long serialVersionUID = 6809574900728721544L;

		@Override
		public boolean filter(PowerEvent value) throws Exception {
			return value.getType().startsWith("a");
		}
	}

	public static class FlatSelectFunction implements PatternFlatSelectFunction<PowerEvent, String> {
		private static final long serialVersionUID = -7932710864411648825L;

		@Override
		public void flatSelect(Map<String, List<PowerEvent>> pattern, Collector<String> out) throws Exception {
			List<PowerEvent> events = pattern.get("start");
			List<PowerEvent> followEvents = pattern.get("follow");
			if (!CollectionUtil.isNullOrEmpty(events)) {
				StringBuilder builder = new StringBuilder("select value:");
				for (PowerEvent event : events) {
					builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",type:"
							+ event.getType() + "]");
				}
				builder.append(" | following| ");
				if (!CollectionUtil.isNullOrEmpty(followEvents)) {
					for (PowerEvent event : followEvents) {
						builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",type:"
								+ event.getType() + "]");
					}
				}
				out.collect(builder.toString());
			} else {
				throw new Exception("no start event.");
			}
		}
	}

	public static class FlatSelectFunction2 implements PatternFlatSelectFunction<PowerEvent, String> {
		private static final long serialVersionUID = -7932710864411648825L;

		@Override
		public void flatSelect(Map<String, List<PowerEvent>> pattern, Collector<String> out) throws Exception {
			List<PowerEvent> events = pattern.get("start");
			List<PowerEvent> followEvents = pattern.get("follow");
			List<PowerEvent> matchEvents = pattern.get("match");
			if (!CollectionUtil.isNullOrEmpty(events)) {
				StringBuilder builder = new StringBuilder("select value:");
				for (PowerEvent event : events) {
					builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",type:"
							+ event.getType() + "]");
				}
				builder.append(" | match | ");
				if (!CollectionUtil.isNullOrEmpty(matchEvents)) {
					for (PowerEvent event : matchEvents) {
						builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",type:"
								+ event.getType() + "]");
					}
				}
				builder.append(" | following| ");
				if (!CollectionUtil.isNullOrEmpty(followEvents)) {
					for (PowerEvent event : followEvents) {
						builder.append("[ number :" + event.getNumber() + " value:" + event.getVoltage() + ",type:"
								+ event.getType() + "]");
					}
				}
				out.collect(builder.toString());
			} else {
				throw new Exception("no start event.");
			}
		}
	}

	public static class FollowCondition extends SimpleCondition<PowerEvent> {
		private static final long serialVersionUID = -4418346283781995457L;

		@Override
		public boolean filter(PowerEvent value) throws Exception {
			return value.getType().startsWith("b");
		}
	}

	public static class UntilCondition extends IterativeCondition<PowerEvent> {
		private static final long serialVersionUID = -5831032701596221514L;

		@Override
		public boolean filter(PowerEvent value, Context<PowerEvent> ctx) throws Exception {
			PowerEvent startEvent = ctx.getEventsForPattern("start").iterator().next();
			if (value.getNumber() - startEvent.getNumber() > 2) {
				return true;
			}
			return false;
		}

	}

	public static class StartCondition extends IterativeCondition<PowerEvent> {
		private static final long serialVersionUID = 4979764422309695284L;

		@Override
		public boolean filter(PowerEvent value, Context<PowerEvent> ctx) throws Exception {
			if (value.getNumber() % 10 == 4) {
				return true;
			}
			return false;
		}

	}

	@Test
	public void testambpattern() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<PowerEvent> stream = env.addSource(new PowerEventSourceRollingSingle());
		stream.setParallelism(1);
		Pattern<PowerEvent, PowerEvent> followPattern = Pattern.<PowerEvent>begin("follow").where(new FollowCondition())
				.within(Time.milliseconds(500L));
		Pattern<PowerEvent, PowerEvent> pattern = Pattern
				.<PowerEvent>begin("start", AfterMatchSkipStrategy.skipPastLastEvent()).subtype(PowerEvent.class)
				.where(new EventCondition()).oneOrMore().greedy().followedBy(followPattern);
		PatternStream<PowerEvent> pStream = CEP.pattern(stream, pattern);
		pStream.flatSelect(new FlatSelectFunction()).setParallelism(1).print();
		env.execute();
	}

	@Test
	public void testabmcPattern() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<PowerEvent> stream = env.addSource(new PowerEventSourceRollingSingle());
		stream.setParallelism(1);
		Pattern<PowerEvent, PowerEvent> followPattern = Pattern.<PowerEvent>begin("follow")
				.where(new FollowCondition());
		Pattern<PowerEvent, PowerEvent> matchPattern = Pattern
				.<PowerEvent>begin("match", AfterMatchSkipStrategy.skipPastLastEvent()).subtype(PowerEvent.class)
				.where(new EventCondition()).oneOrMore().greedy().followedBy(followPattern);
		Pattern<PowerEvent, PowerEvent> start = Pattern.<PowerEvent>begin("start").subtype(PowerEvent.class)
				.where(new StartCondition()).followedBy(matchPattern).within(Time.milliseconds(700L));

		PatternStream<PowerEvent> pStream = CEP.pattern(stream, start);
		pStream.flatSelect(new FlatSelectFunction2()).setParallelism(1).print();
		env.execute();
	}

	@Test
	public void testabmcPatternWriteFile() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		DataStreamSource<PowerEvent> stream = env.addSource(new PowerEventSourceRollingSingle());
		stream.setParallelism(1);
		Pattern<PowerEvent, PowerEvent> followPattern = Pattern.<PowerEvent>begin("follow")
				.where(new FollowCondition());
		Pattern<PowerEvent, PowerEvent> matchPattern = Pattern
				.<PowerEvent>begin("match", AfterMatchSkipStrategy.skipPastLastEvent()).subtype(PowerEvent.class)
				.where(new EventCondition()).oneOrMore().greedy().followedBy(followPattern);
		Pattern<PowerEvent, PowerEvent> start = Pattern.<PowerEvent>begin("start").subtype(PowerEvent.class)
				.where(new StartCondition()).followedBy(matchPattern).within(Time.milliseconds(700L));

		PatternStream<PowerEvent> pStream = CEP.pattern(stream, start);
//		pStream.flatSelect(new FlatSelectFunction2()).print();
		pStream.flatSelect(new FlatSelectFunction2()).setParallelism(1).writeAsText("D:\\temp\\flink-test/out.log",
				WriteMode.OVERWRITE);
		env.execute();
	}
}
