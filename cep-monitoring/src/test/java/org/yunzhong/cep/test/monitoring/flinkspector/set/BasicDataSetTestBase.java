package org.yunzhong.cep.test.monitoring.flinkspector.set;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.operators.MapOperator;
import org.junit.Test;

import io.flinkspector.core.collection.ExpectedRecords;
import io.flinkspector.dataset.DataSetTestBase;
import io.flinkspector.shade.com.google.common.collect.Lists;

public class BasicDataSetTestBase extends DataSetTestBase {

	@Test
	public void test() {
		DataSet<String> dataSet = createTestDataSet(Lists.newArrayList("first", "second"));
		MapOperator<String, Integer> newMap = dataSet.map((MapFunction<String, Integer>) (value) -> {
			return value.length();
		});
		ExpectedRecords<Integer> expected = new ExpectedRecords<Integer>()
				.expectAll(Lists.asList(5, new Integer[] { 6 }));

		assertDataSet(newMap, expected);
	}
}
