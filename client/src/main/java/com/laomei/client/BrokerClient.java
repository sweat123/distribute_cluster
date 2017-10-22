package com.laomei.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;

/**
 * author luobo
 */
public class BrokerClient {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerClient.class);

    public static final String ROOT = "/local/task";

    private CuratorFramework zkClient;

    public BrokerClient(String zkHost) {
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkHost)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zkClient.start();
    }

    public void registry() throws Exception {
        PathChildrenCache childrenCache = new PathChildrenCache(zkClient, ROOT, true);
        childrenCache.start();
        childrenCache.getListenable().addListener((client, event) -> {
            switch (event.getType()) {
            case CHILD_ADDED:
                LOG.info("child add; node path: {}", event.getData().getPath());
                lockAndWork(client, event);
                break;
            case CHILD_REMOVED:
                LOG.info("child removed; node path: {}", event.getData().getPath());
                break;
            }
        });
    }

    public void close() {
        zkClient.close();
    }

    private void lockAndWork(CuratorFramework zkClient, PathChildrenCacheEvent event) {
        String path = event.getData().getPath();
        byte[] nodeData = event.getData().getData();
        if (!tryLock(zkClient, path + "/lock")) {
            LOG.info("lock node failed.");
            return;
        }
        //lock succeed
        createTask(nodeData);
    }

    private void createTask(byte[] data) {
        String dataStr = Utils.convertJsonByteArrToAssignedObj(data, "key", String.class);
        LOG.info("create task with data {}", dataStr);
    }

    private boolean tryLock(CuratorFramework zkClient, String path) {
        try {
            zkClient.create().withMode(EPHEMERAL).forPath(path);
        } catch (Exception e) {
            LOG.error("create node failed; node path: {}", path, e);
            return false;
        }
        return true;
    }

    public void work() {
        BrokerClient client = new BrokerClient("localhost:2181");
        try {
            client.registry();
        } catch (Exception e) {
            LOG.error("registry failed.", e);
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(client::close));
        try {
            TimeUnit.SECONDS.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0; i < 3; i++) {
            service.submit(() -> {
                BrokerClient client = new BrokerClient("localhost:2181");
                client.work();
            });
        }
    }
}
