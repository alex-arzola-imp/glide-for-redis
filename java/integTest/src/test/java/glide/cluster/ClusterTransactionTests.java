/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.cluster;

import static glide.TransactionTestUtilities.transactionTest;
import static glide.TransactionTestUtilities.transactionTestResult;
import static glide.api.BaseClient.OK;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute.RANDOM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.TestConfiguration;
import glide.api.RedisClusterClient;
import glide.api.models.ClusterTransaction;
import glide.api.models.ClusterValue;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10) // seconds
public class ClusterTransactionTests {

    private static RedisClusterClient clusterClient = null;

    @BeforeAll
    @SneakyThrows
    public static void init() {
        clusterClient =
                RedisClusterClient.CreateClient(
                                RedisClusterClientConfiguration.builder()
                                        .address(NodeAddress.builder().port(TestConfiguration.CLUSTER_PORTS[0]).build())
                                        .requestTimeout(5000)
                                        .build())
                        .get();
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        clusterClient.close();
    }

    @Test
    @SneakyThrows
    public void custom_command_info() {
        ClusterTransaction transaction = new ClusterTransaction().customCommand(new String[] {"info"});
        Object[] result = clusterClient.exec(transaction).get();
        assertTrue(((String) result[0]).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void WATCH_transaction_failure_returns_null() {
        ClusterTransaction transaction = new ClusterTransaction();
        transaction.get("key");
        assertEquals(
                OK, clusterClient.customCommand(new String[] {"WATCH", "key"}).get().getSingleValue());
        assertEquals(OK, clusterClient.set("key", "foo").get());
        assertNull(clusterClient.exec(transaction).get());
    }

    @Test
    @SneakyThrows
    public void info_simple_route_test() {
        ClusterTransaction transaction = new ClusterTransaction().info().info();
        ClusterValue<Object>[] result = clusterClient.exec(transaction, RANDOM).get();

        // check single-value result
        assertTrue(result[0].hasSingleData());
        assertTrue(((String) result[0].getSingleValue()).contains("# Stats"));

        assertTrue(result[1].hasSingleData());
        assertTrue(((String) result[1].getSingleValue()).contains("# Stats"));
    }

    @SneakyThrows
    @Test
    public void test_cluster_transactions() {
        ClusterTransaction transaction = (ClusterTransaction) transactionTest(new ClusterTransaction());
        Object[] expectedResult = transactionTestResult();

        ClusterValue<Object>[] clusterValues = clusterClient.exec(transaction, RANDOM).get();
        Object[] results =
                Arrays.stream(clusterValues)
                        .map(v -> v.hasSingleData() ? v.getSingleValue() : v.getMultiValue())
                        .toArray(Object[]::new);
        assertArrayEquals(expectedResult, results);
    }
}
