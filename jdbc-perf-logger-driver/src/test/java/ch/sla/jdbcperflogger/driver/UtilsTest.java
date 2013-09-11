package ch.sla.jdbcperflogger.driver;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import ch.sla.jdbcperflogger.driver.Utils;

public class UtilsTest {

	@Test
	public void testExtractAllInterfaces() {
		final Class<?>[] intfs = Utils.extractAllInterfaces(HashSet.class);
		final Set<Class<?>> coll = new HashSet<Class<?>>();
		Collections.addAll(coll, intfs);
		Assert.assertTrue(coll.contains(Collection.class));
		Assert.assertTrue(coll.contains(Serializable.class));
	}

}
