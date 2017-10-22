package com.laomei.server;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.apache.zookeeper.CreateMode.PERSISTENT;

/**
 * author luobo
 */
public class BrokerServer {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerServer.class);

    public static final String ROOT = "/local/task";

    private CuratorFramework zkClient;

    public BrokerServer(String zkHost) {
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkHost)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zkClient.start();
    }

    public void start() {
        if (!nodeExists(ROOT)) {
            try {
                zkClient.create().creatingParentsIfNeeded().withMode(PERSISTENT).forPath(ROOT);
            } catch (Exception e) {
                LOG.error("create root failed.", e);
            }
        }
    }

    public void createTask(String taskName, byte[] data) {
        String path = ROOT + "/" + taskName;
        if (nodeExists(path)) {
            LOG.error("task already exists.");
            return;
        }
        try {
            zkClient.create().withMode(PERSISTENT).forPath(path, data);
        } catch (Exception e) {
            LOG.error("create task failed. node path: {}", path, e);
        }
    }

    public void close() {
        zkClient.close();
    }

    private boolean nodeExists(String path) {
        Stat stat = null;
        try {
            stat = zkClient.checkExists().forPath(path);
        } catch (Exception e) {
            LOG.error("check node failed. node path: {}", path, e);
            return false;
        }
        return null != stat;
    }

    public static void main(String[] args) throws InterruptedException {
        BrokerServer server = new BrokerServer("localhost:2181");
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        TimeUnit.SECONDS.sleep(5);
        LOG.info("begin to create task...");
        server.createTask("test",
                Utils.convertObjToJsonByteArr("hello world", "key"));
        LOG.info("finished creating task...");
        TimeUnit.SECONDS.sleep(3000);
    }
}
