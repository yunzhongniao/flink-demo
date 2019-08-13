package org.yunzhong.demo.util;

import java.io.IOException;

import org.yunzhong.demo.model.OrderAnotherBean;
import org.yunzhong.demo.model.OrderBean;

import net.sf.cglib.beans.BeanCopier;

public class PropertyAssembler {
	private static BeanCopier copyer = BeanCopier.create(OrderBean.class, OrderAnotherBean.class, false);

	public static <T, R> void assemble(T source, R target) throws IOException {
		if (source == null || target == null) {
			throw new IOException("param is null.");
		}
		String key = String.format("%s.%s", source.getClass().getName(), target.getClass().getName());
		copyer.copy(source, target, null);
	}
}
