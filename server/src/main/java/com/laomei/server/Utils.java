/*
 * Utils.java
 * Copyright 2017 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package com.laomei.server;

import com.alibaba.fastjson.JSONObject;

/**
 * @author luobo
 */
public class Utils {

    public static<T> T convertJsonByteArrToAssignedObj(byte[] object, String key, Class<T> clazz) {
        JSONObject jsonObject = JSONObject.parseObject(new String(object));
        return jsonObject.getObject(key, clazz);
    }

    public static<T> byte[] convertObjToJsonByteArr(T o, String key) {
        JSONObject object = new JSONObject();
        object.put(key, o);
        return object.toJSONString().getBytes();
    }
}
