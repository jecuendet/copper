package ch.mno.copper.collect.connectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Created by dutoitc on 15.02.2016.
 */
public class JmxConnectorTest {

    private static JMXConnectorServer connectorServer;

    @BeforeClass
    public static void setup() throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        java.rmi.registry.LocateRegistry.createRegistry(39055);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:39055/server");

        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
        connectorServer.start();
    }

    @AfterClass
    public static void done() throws IOException {
        connectorServer.stop();
    }

    @Test
    public void testX() throws IOException, MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, InterruptedException {
        JmxConnector conn = new JmxConnector("service:jmx:rmi:///jndi/rmi://localhost:39055/server");
        String aValue = conn.getObject("java.lang:type=Runtime", "SpecName");
        Assert.assertTrue(aValue.contains("Java"));
        conn.close();
    }

}
