package com.nhn.pinpoint.collector.cluster.zookeeper;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhn.pinpoint.collector.config.CollectorConfiguration;
import com.nhn.pinpoint.rpc.packet.ControlEnableWorkerConfirmPacket;
import com.nhn.pinpoint.rpc.packet.RequestPacket;
import com.nhn.pinpoint.rpc.packet.SendPacket;
import com.nhn.pinpoint.rpc.packet.StreamPacket;
import com.nhn.pinpoint.rpc.server.ChannelContext;
import com.nhn.pinpoint.rpc.server.PinpointServerSocket;
import com.nhn.pinpoint.rpc.server.PinpointServerSocketStateCode;
import com.nhn.pinpoint.rpc.server.ServerMessageListener;
import com.nhn.pinpoint.rpc.server.ServerStreamChannel;
import com.nhn.pinpoint.rpc.server.SocketChannel;

public class ZookeeperWebClusterServiceTest {

	private static final String PINPOINT_CLUSTER_PATH = "/pinpoint-cluster";
	private static final String PINPOINT_WEB_CLUSTER_PATH = PINPOINT_CLUSTER_PATH + "/web";
	private static final String PINPOINT_PROFILER_CLUSTER_PATH = PINPOINT_CLUSTER_PATH + "/profiler";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final int DEFAULT_ZOOKEEPER_PORT = 22213;
	private static final int DEFAULT_ACCEPTOR_SOCKET_PORT = 22214;

	private static CollectorConfiguration collectorConfig = null;

	@BeforeClass
	public static void setUp() {
		collectorConfig = new CollectorConfiguration();

		collectorConfig.setClusterEnable(true);
		collectorConfig.setClusterAddress("127.0.0.1:" + DEFAULT_ZOOKEEPER_PORT);
		collectorConfig.setClusterSessionTimeout(3000);
	}

	@Test
	public void simpleTest1() throws Exception {
		TestingServer ts = null;
		try {
			ts = createZookeeperServer(DEFAULT_ZOOKEEPER_PORT);

			ZookeeperClusterService service = new ZookeeperClusterService(collectorConfig);
			service.setUp();

			PinpointServerSocket pinpointServerSocket = new PinpointServerSocket();
			pinpointServerSocket.setMessageListener(new PinpointSocketManagerHandler());
			pinpointServerSocket.bind("127.0.0.1", DEFAULT_ACCEPTOR_SOCKET_PORT);

			ZookeeperClient client = new ZookeeperClient("127.0.0.1:" + DEFAULT_ZOOKEEPER_PORT, 3000, new ZookeeperEventWatcher() {

				@Override
				public void process(WatchedEvent event) {

				}

				@Override
				public boolean isConnected() {
					return true;
				}
			});
			client.createPath(PINPOINT_WEB_CLUSTER_PATH, true);
			client.createNode(PINPOINT_WEB_CLUSTER_PATH + "/" + "127.0.0.1:" + DEFAULT_ACCEPTOR_SOCKET_PORT, "127.0.0.1".getBytes());

			Thread.sleep(5000);

			List<ChannelContext> channelContextList = pinpointServerSocket.getDuplexCommunicationChannelContext();
			Assert.assertEquals(1, channelContextList.size());

			client.close();

			Thread.sleep(5000);
			channelContextList = pinpointServerSocket.getDuplexCommunicationChannelContext();
			Assert.assertEquals(0, channelContextList.size());

			service.tearDown();
		} finally {
			closeZookeeperServer(ts);
		}
	}

	private TestingServer createZookeeperServer(int port) throws Exception {
		TestingServer mockZookeeperServer = new TestingServer(port);
		mockZookeeperServer.start();

		return mockZookeeperServer;
	}

	private void closeZookeeperServer(TestingServer mockZookeeperServer) throws Exception {
		try {
			mockZookeeperServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private PinpointServerSocketStateCode getCode(Map channelContextData) {
		String state = (String) channelContextData.get("state");
		return PinpointServerSocketStateCode.getStateCode(state);
	}

	private class PinpointSocketManagerHandler implements ServerMessageListener {
		@Override
		public void handleSend(SendPacket sendPacket, SocketChannel channel) {
			logger.warn("Unsupport send received {} {}", sendPacket, channel);
		}

		@Override
		public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
			logger.warn("Unsupport request received {} {}", requestPacket, channel);
		}

		@Override
		public void handleStream(StreamPacket streamPacket, ServerStreamChannel streamChannel) {
			logger.warn("unsupported streamPacket received {}", streamPacket);
		}

		@Override
		public int handleEnableWorker(Map properties) {
			logger.warn("do handleEnableWorker {}", properties);
			return ControlEnableWorkerConfirmPacket.SUCCESS;
		}
	}

}
