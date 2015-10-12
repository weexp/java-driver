/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.util.concurrent.Futures;
import org.scassandra.Scassandra;
import org.scassandra.ScassandraFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.WhiteListPolicy;

/**
 * A number of static fields/methods handy for tests.
 */
public abstract class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    public static final String CREATE_KEYSPACE_SIMPLE_FORMAT = "CREATE KEYSPACE %s WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : %d }";
    public static final String CREATE_KEYSPACE_GENERIC_FORMAT = "CREATE KEYSPACE %s WITH replication = { 'class' : '%s', %s }";

    public static final String SIMPLE_KEYSPACE = "ks";
    public static final String SIMPLE_TABLE = "test";

    public static final String SELECT_ALL_FORMAT = "SELECT * FROM %s";

    public static final int TEST_BASE_NODE_WAIT = SystemProperties.getInt("com.datastax.driver.TEST_BASE_NODE_WAIT", 60);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static BoundStatement setBoundValue(BoundStatement bs, String name, DataType type, Object value) {
        switch (type.getName()) {
            case ASCII:
                bs.setString(name, (String)value);
                break;
            case BIGINT:
                bs.setLong(name, (Long)value);
                break;
            case BLOB:
                bs.setBytes(name, (ByteBuffer)value);
                break;
            case BOOLEAN:
                bs.setBool(name, (Boolean)value);
                break;
            case COUNTER:
                // Just a no-op, we shouldn't handle counters the same way than other types
                break;
            case DECIMAL:
                bs.setDecimal(name, (BigDecimal)value);
                break;
            case DOUBLE:
                bs.setDouble(name, (Double)value);
                break;
            case FLOAT:
                bs.setFloat(name, (Float)value);
                break;
            case INET:
                bs.setInet(name, (InetAddress)value);
                break;
            case TINYINT:
                bs.setByte(name, (Byte) value);
                break;
            case SMALLINT:
                bs.setShort(name, (Short) value);
                break;
            case INT:
                bs.setInt(name, (Integer) value);
                break;
            case TEXT:
                bs.setString(name, (String) value);
                break;
            case TIMESTAMP:
                bs.setTimestamp(name, (Date) value);
                break;
            case DATE:
                bs.setDate(name, (LocalDate)value);
                break;
            case TIME:
                bs.setTime(name, (Long) value);
                break;
            case UUID:
                bs.setUUID(name, (UUID)value);
                break;
            case VARCHAR:
                bs.setString(name, (String)value);
                break;
            case VARINT:
                bs.setVarint(name, (BigInteger)value);
                break;
            case TIMEUUID:
                bs.setUUID(name, (UUID)value);
                break;
            case LIST:
                bs.setList(name, (List)value);
                break;
            case SET:
                bs.setSet(name, (Set)value);
                break;
            case MAP:
                bs.setMap(name, (Map)value);
                break;
            default:
                throw new RuntimeException("Missing handling of " + type);
        }
        return bs;
    }

    public static Object getValue(Row row, String name, DataType type, CodecRegistry codecRegistry) {
        switch (type.getName()) {
            case ASCII:
                return row.getString(name);
            case BIGINT:
                return row.getLong(name);
            case BLOB:
                return row.getBytes(name);
            case BOOLEAN:
                return row.getBool(name);
            case COUNTER:
                return row.getLong(name);
            case DECIMAL:
                return row.getDecimal(name);
            case DOUBLE:
                return row.getDouble(name);
            case FLOAT:
                return row.getFloat(name);
            case INET:
                return row.getInet(name);
            case TINYINT:
                return row.getByte(name);
            case SMALLINT:
                return row.getShort(name);
            case INT:
                return row.getInt(name);
            case TEXT:
                return row.getString(name);
            case TIMESTAMP:
                return row.getTimestamp(name);
            case DATE:
                return row.getDate(name);
            case TIME:
                return row.getTime(name);
            case UUID:
                return row.getUUID(name);
            case VARCHAR:
                return row.getString(name);
            case VARINT:
                return row.getVarint(name);
            case TIMEUUID:
                return row.getUUID(name);
            case LIST:
                Class<?> listEltClass = codecRegistry.codecFor(type.getTypeArguments().get(0)).getJavaType().getRawType();
                return row.getList(name, listEltClass);
            case SET:
                Class<?> setEltClass = codecRegistry.codecFor(type.getTypeArguments().get(0)).getJavaType().getRawType();
                return row.getSet(name, setEltClass);
            case MAP:
                Class<?> keyClass = codecRegistry.codecFor(type.getTypeArguments().get(0)).getJavaType().getRawType();
                Class<?> valueClass = codecRegistry.codecFor(type.getTypeArguments().get(1)).getJavaType().getRawType();
                return row.getMap(name, keyClass, valueClass);
        }
        throw new RuntimeException("Missing handling of " + type);
    }

    // Always return the "same" value for each type
    @SuppressWarnings("serial")
    public static Object getFixedValue(final DataType type) {
        try {
            switch (type.getName()) {
                case ASCII:
                    return "An ascii string";
                case BIGINT:
                    return 42L;
                case BLOB:
                    return ByteBuffer.wrap(new byte[]{ (byte)4, (byte)12, (byte)1 });
                case BOOLEAN:
                    return true;
                case COUNTER:
                    throw new UnsupportedOperationException("Cannot 'getSomeValue' for counters");
                case DECIMAL:
                    return new BigDecimal("3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");
                case DOUBLE:
                    return 3.142519;
                case FLOAT:
                    return 3.142519f;
                case INET:
                    return InetAddress.getByAddress(new byte[]{ (byte)127, (byte)0, (byte)0, (byte)1 });
                case TINYINT:
                    return (byte)25;
                case SMALLINT:
                    return (short)26;
                case INT:
                    return 24;
                case TEXT:
                    return "A text string";
                case TIMESTAMP:
                    return new Date(1352288289L);
                case DATE:
                    return LocalDate.fromDaysSinceEpoch(0);
                case TIME:
                    return 54012123450000L;
                case UUID:
                    return UUID.fromString("087E9967-CCDC-4A9B-9036-05930140A41B");
                case VARCHAR:
                    return "A varchar string";
                case VARINT:
                    return new BigInteger("123456789012345678901234567890");
                case TIMEUUID:
                    return UUID.fromString("FE2B4360-28C6-11E2-81C1-0800200C9A66");
                case LIST:
                    return new ArrayList<Object>(){{ add(getFixedValue(type.getTypeArguments().get(0))); }};
                case SET:
                    return new HashSet<Object>(){{ add(getFixedValue(type.getTypeArguments().get(0))); }};
                case MAP:
                    return new HashMap<Object, Object>(){{ put(getFixedValue(type.getTypeArguments().get(0)), getFixedValue(type.getTypeArguments().get(1))); }};
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Missing handling of " + type);
    }

    // Always return the "same" value for each type
    @SuppressWarnings("serial")
    public static Object getFixedValue2(final DataType type) {
        try {
            switch (type.getName()) {
                case ASCII:
                    return "A different ascii string";
                case BIGINT:
                    return Long.MAX_VALUE;
                case BLOB:
                    ByteBuffer bb = ByteBuffer.allocate(64);
                    bb.putInt(0xCAFE);
                    bb.putShort((short) 3);
                    bb.putShort((short) 45);
                    return bb;
                case BOOLEAN:
                    return false;
                case COUNTER:
                    throw new UnsupportedOperationException("Cannot 'getSomeValue' for counters");
                case DECIMAL:
                    return new BigDecimal("12.3E+7");
                case DOUBLE:
                    return Double.POSITIVE_INFINITY;
                case FLOAT:
                    return Float.POSITIVE_INFINITY;
                case INET:
                    return InetAddress.getByName("123.123.123.123");
                case TINYINT:
                    return Byte.MAX_VALUE;
                case SMALLINT:
                    return Short.MAX_VALUE;
                case INT:
                    return Integer.MAX_VALUE;
                case TEXT:
                    return "résumé";
                case TIMESTAMP:
                    return new Date(872835240000L);
                case DATE:
                    return LocalDate.fromDaysSinceEpoch(0);
                case TIME:
                    return 54012123450000L;
                case UUID:
                    return UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00");
                case VARCHAR:
                    return "A different varchar résumé";
                case VARINT:
                    return new BigInteger(Integer.toString(Integer.MAX_VALUE) + "000");
                case TIMEUUID:
                    return UUID.fromString("FE2B4360-28C6-11E2-81C1-0800200C9A66");
                case LIST:
                    return new ArrayList<Object>(){{ add(getFixedValue2(type.getTypeArguments().get(0))); }};
                case SET:
                    return new HashSet<Object>(){{ add(getFixedValue2(type.getTypeArguments().get(0))); }};
                case MAP:
                    return new HashMap<Object, Object>(){{ put(getFixedValue2(type.getTypeArguments().get(0)), getFixedValue2(type.getTypeArguments().get(1))); }};
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Missing handling of " + type);
    }

    // Wait for a node to be up and running
    // This is used because there is some delay between when a node has been
    // added through ccm and when it's actually available for querying
    public static void waitFor(String node, Cluster cluster) {
        waitFor(node, cluster, TEST_BASE_NODE_WAIT, false, false);
    }

    public static void waitFor(String node, Cluster cluster, int maxTry) {
        waitFor(node, cluster, maxTry, false, false);
    }

    public static void waitForDown(String node, Cluster cluster) {
        waitFor(node, cluster, TEST_BASE_NODE_WAIT * 3, true, false);
    }

    public static void waitForDownWithWait(String node, Cluster cluster, int waitTime) {
        waitForDown(node, cluster);

        // FIXME: Once stop() works, remove this line
        try {
            Thread.sleep(waitTime * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void waitForDown(String node, Cluster cluster, int maxTry) {
        waitFor(node, cluster, maxTry, true, false);
    }

    public static void stopAndWait(CCMBridge.CCMCluster c, int node) {
        c.cassandraCluster.stop(node);
        waitForDownWithWait(CCMBridge.IP_PREFIX + node, c.cluster, 5);
    }

    public static void waitForDecommission(String node, Cluster cluster) {
        waitFor(node, cluster, TEST_BASE_NODE_WAIT / 2, true, true);
    }

    public static void waitForDecommission(String node, Cluster cluster, int maxTry) {
        waitFor(node, cluster, maxTry, true, true);
    }

    private static void waitFor(String node, Cluster cluster, int maxTry, boolean waitForDead, boolean waitForOut) {
        if (waitForDead || waitForOut)
            if (waitForDead)
                logger.info("Waiting for stopped node: " + node);
            else if (waitForOut)
                logger.info("Waiting for decommissioned node: " + node);
        else
            logger.info("Waiting for upcoming node: " + node);

        // In the case where the we've killed the last node in the cluster, if we haven't
        // tried doing an actual query, the driver won't realize that last node is dead until
        // keep alive kicks in, but that's a fairly long time. So we cheat and trigger a force
        // the detection by forcing a request.
        if (waitForDead || waitForOut) {
            Futures.getUnchecked(cluster.manager.submitSchemaRefresh(null, null, null, null));
        }

        InetAddress address;
        try {
             address = InetAddress.getByName(node);
        } catch (Exception e) {
            // That's a problem but that's not *our* problem
            return;
        }

        Metadata metadata = cluster.getMetadata();
        for (int i = 0; i < maxTry; ++i) {
            for (Host host : metadata.getAllHosts()) {
                if (host.getAddress().equals(address) && testHost(host, waitForDead)) {
                    try { Thread.sleep(10000); } catch (Exception e) {}
                    return;
                }
            }
            try { Thread.sleep(1000); } catch (Exception e) {}
        }

        for (Host host : metadata.getAllHosts()) {
            if (host.getAddress().equals(address)) {
                if (testHost(host, waitForDead)) {
                    return;
                } else {
                    // logging it because this give use the timestamp of when this happens
                    logger.info(node + " is not " + (waitForDead ? "DOWN" : "UP") + " after " + maxTry + 's');
                    throw new IllegalStateException(node + " is not " + (waitForDead ? "DOWN" : "UP") + " after " + maxTry + 's');
                }
            }
        }

        if (waitForOut){
            return;
        } else {
            logger.info(node + " is not part of the cluster after " + maxTry + 's');
            throw new IllegalStateException(node + " is not part of the cluster after " + maxTry + 's');
        }
    }

    private static boolean testHost(Host host, boolean testForDown) {
        return testForDown ? !host.isUp() : host.isUp();
    }

    public static void versionCheck(double majorCheck, int minorCheck, String skipString) {
        String version = System.getProperty("cassandra.version");
        String[] versionArray = version.split("\\.|-");
        double major = Double.parseDouble(versionArray[0] + "." + versionArray[1]);
        int minor = Integer.parseInt(versionArray[2]);

        if (major < majorCheck || (major == majorCheck && minor < minorCheck)) {
            throw new SkipException("Version >= " + majorCheck + "." + minorCheck + " required.  Description: " + skipString);
        }
    }

    public static Host findHost(Cluster cluster, int hostNumber) {
        return findHost(cluster, CCMBridge.ipOfNode(hostNumber));
    }

    public static Host findHost(Cluster cluster, String address) {
        // Note: we can't rely on ProtocolOptions.getPort() to build an InetSocketAddress and call metadata.getHost,
        // because the port doesn't have the correct value if addContactPointsWithPorts was used to create the Cluster
        // (JAVA-860 will solve that)
        for (Host host : cluster.getMetadata().allHosts()) {
            if (host.getAddress().getHostAddress().equals(address))
                return host;
        }
        return null;
    }

    public static HostConnectionPool poolOf(Session session, int hostNumber) {
        SessionManager sessionManager = (SessionManager)session;
        return sessionManager.pools.get(findHost(session.getCluster(), hostNumber));
    }

    public static int numberOfLocalCoreConnections(Cluster cluster) {
        Configuration configuration = cluster.getConfiguration();
        return configuration.getPoolingOptions().getCoreConnectionsPerHost(HostDistance.LOCAL);
    }

    /**
     * @return A Scassandra instance with an arbitrarily chosen binary port from 8042-8142 and admin port from
     * 8052-8152.
     */
    public static Scassandra createScassandraServer() {
        int binaryPort = findAvailablePort(8042);
        int adminPort = findAvailablePort(8052);
        return ScassandraFactory.createServer(binaryPort, adminPort);
    }

    /**
     * @param startingWith The first port to try, if unused will keep trying the next port until one is found up to
     *                     100 subsequent ports.
     * @return A local port that is currently unused.
     */
    public static int findAvailablePort(int startingWith) {
        IOException last = null;
        for (int port = startingWith; port < startingWith + 100; port++) {
            try {
                ServerSocket s = new ServerSocket(port);
                s.close();
                return port;
            } catch (IOException e) {
                last = e;
            }
        }
        // If for whatever reason a port could not be acquired throw the last encountered exception.
        throw new RuntimeException("Could not acquire an available port", last);
    }

    /**
     * @return The desired target protocol version based on the 'cassandra.version' System property.
     */
    public static ProtocolVersion getDesiredProtocolVersion() {
        String version = System.getProperty("cassandra.version");
        String[] versionArray = version.split("\\.|-");
        double major = Double.parseDouble(versionArray[0] + "." + versionArray[1]);
        if(major < 2.0) {
            return ProtocolVersion.V1;
        } else if (major < 2.1) {
            return ProtocolVersion.V2;
        } else if (major < 2.2) {
            return ProtocolVersion.V3;
        } else {
            return ProtocolVersion.V4;
        }
    }

    /**
     * @param maximumAllowed The maximum protocol version to use.
     * @return The desired protocolVersion or maximumAllowed if {@link #getDesiredProtocolVersion} is greater.
     */
    public static ProtocolVersion getDesiredProtocolVersion(ProtocolVersion maximumAllowed) {
        ProtocolVersion versionToUse = getDesiredProtocolVersion();
        return versionToUse.compareTo(maximumAllowed) > 0 ? maximumAllowed : versionToUse;
    }

    /**
     * @return a {@Cluster} instance that connects only to the control host of the given cluster.
     */
    public static Cluster buildControlCluster(Cluster cluster) {
        Host controlHost = cluster.manager.controlConnection.connectedHost();
        List<InetSocketAddress> singleAddress = Collections.singletonList(controlHost.getSocketAddress());
        return Cluster.builder()
            .addContactPointsWithPorts(singleAddress)
            .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), singleAddress))
            .build();
    }

    /**
     * @return a {@QueryOptions} that disables debouncing by setting intervals to 0ms.
     */
    public static QueryOptions nonDebouncingQueryOptions() {
        return new QueryOptions().setRefreshNodeIntervalMillis(0)
            .setRefreshNodeListIntervalMillis(0)
            .setRefreshSchemaIntervalMillis(0);
    }
}