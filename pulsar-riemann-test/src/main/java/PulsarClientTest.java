import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

public class PulsarClientTest {
    public static void main(String[] args) throws PulsarClientException {
        //建造者模式
        PulsarClient client = PulsarClient.builder()
                .listenerThreads(1)
                .ioThreads(1)
                .serviceUrl("pulsar://127.0.0.1:6650")
                .build();
        System.out.println(client.toString());
    }
}
