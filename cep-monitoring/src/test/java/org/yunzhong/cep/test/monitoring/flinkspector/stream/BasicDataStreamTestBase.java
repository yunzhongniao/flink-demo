package org.yunzhong.cep.test.monitoring.flinkspector.stream;

import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.MapPartitionFunction;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.util.Collector;
import org.junit.Test;

import io.flinkspector.core.collection.ExpectedRecords;
import io.flinkspector.core.input.InputBuilder;
import io.flinkspector.datastream.DataStreamTestBase;
import io.flinkspector.datastream.input.EventTimeInputBuilder;
import io.flinkspector.shade.com.google.common.collect.Lists;

public class BasicDataStreamTestBase extends DataStreamTestBase {

	@Test
	public void test() {
		DataStreamSource<String> dataStream = createTestStream(Lists.newArrayList("first", "second"));
		SingleOutputStreamOperator<Integer> newMap = dataStream.map((MapFunction<String, Integer>) (value) -> {
			return value.length();
		});
		ExpectedRecords<Integer> expected = new ExpectedRecords<Integer>().expectAll(Lists.newArrayList(5, 6));
		assertStream(newMap, expected);
	}

	@Test
	public void testInputBuilder() {
		DataStreamSource<String> dataStream = createTestStream(InputBuilder.startWith("first").emit("second"));
		SingleOutputStreamOperator<Integer> newMap = dataStream.map((MapFunction<String, Integer>) (value) -> {
			return value.length();
		});
		ExpectedRecords<Integer> expected = new ExpectedRecords<Integer>().expectAll(Lists.newArrayList(5, 6));
		assertStream(newMap, expected);
	}

	@Test
	public void testEventTimeInputBuilder() {
		DataStreamSource<String> dataStream = createTestStream(
				EventTimeInputBuilder.startWith("first", System.currentTimeMillis()).emit("second", 3));
		SingleOutputStreamOperator<Integer> newMap = dataStream.map((MapFunction<String, Integer>) (value) -> {
			return value.length();
		});
		ExpectedRecords<Integer> expected = new ExpectedRecords<Integer>().expectAll(Lists.newArrayList(5, 6, 6, 6));
		assertStream(newMap, expected);
	}

	public static class MyMapPartitionFunction implements MapPartitionFunction<String, Integer> {
		private static final long serialVersionUID = -2104128743210741922L;

		@Override
		public void mapPartition(Iterable<String> values, Collector<Integer> out) throws Exception {

		}

	}

	public static class MyPatternFlatSelectFunction implements PatternFlatSelectFunction<String, String> {
		private static final long serialVersionUID = 3823891337213802913L;

		@Override
		public void flatSelect(Map<String, List<String>> pattern, Collector<String> out) throws Exception {
			out.collect(pattern.get("start").iterator().next());
		}
	}

	public static class MySimpleCondition extends SimpleCondition<String> {
		private static final long serialVersionUID = 6432873685318656903L;

		@Override
		public boolean filter(String value) throws Exception {
			return true;
		}
	}

	@Test
	public void testPattern() {
		DataStreamSource<String> dataStream = createTestStream(Lists.newArrayList("first", "second"));
		ExpectedRecords<String> expected = new ExpectedRecords<String>()
				.expectAll(Lists.newArrayList("first", "second"));
		Pattern<String, String> pattern = Pattern.<String>begin("start").subtype(String.class)
				.where(new MySimpleCondition());
		SingleOutputStreamOperator<String> patternStream = CEP.pattern(dataStream, pattern)
				.flatSelect(new MyPatternFlatSelectFunction());
		assertStream(patternStream, expected);
	}
}
