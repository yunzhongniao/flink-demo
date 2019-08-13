package org.yunzhong.demo.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.yunzhong.demo.model.OrderAnotherBean;
import org.yunzhong.demo.model.OrderBean;

public class PropertyAssemblerTest {

	@Test
	public void test() {
		OrderBean source = new OrderBean();
		source.setId("id11");
		source.setName("name333");
		source.setOrderId(100L);
		OrderAnotherBean target = new OrderAnotherBean();
		try {
			PropertyAssembler.assemble(source, target);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		assertEquals(source.getId(), target.getId());
		assertEquals(source.getName(), target.getName());
		assertEquals(source.getOrderId(), target.getOrderId());

	}

	@Test
	public void testBatch() throws IOException {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			OrderBean source = new OrderBean();
			source.setId("id11");
			source.setName("name333");
			source.setOrderId(100L);
			OrderAnotherBean target = new OrderAnotherBean();
			PropertyAssembler.assemble(source, target);
		}
		long end = System.currentTimeMillis();
		System.out.println("cost:" + (end - start));
	}

	@Test
	public void testBatch2() {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			OrderBean source = new OrderBean();
			source.setId("id11");
			source.setName("name333");
			source.setOrderId(100L);
			OrderAnotherBean target = new OrderAnotherBean();
			target.setId(source.getId());
			target.setName(source.getName());
			target.setOrderId(source.getOrderId());
		}
		long end = System.currentTimeMillis();
		System.out.println("cost:" + (end - start));
	}

}
