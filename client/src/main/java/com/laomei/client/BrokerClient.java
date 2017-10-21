package com.laomei.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zookeeper.CreateMode.PERSISTENT;

/**
 * author luobo
 */
public class BrokerClient {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerClient.class);

    public static final String ROOT = "/task";

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
}
