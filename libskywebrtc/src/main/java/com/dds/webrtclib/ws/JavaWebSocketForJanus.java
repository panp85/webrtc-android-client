package com.dds.webrtclib.ws;

import android.annotation.SuppressLint;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;

import org.webrtc.IceCandidate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Created by dds on 2019/1/3.
 * android_shuai@163.com
 */
public class JavaWebSocketForJanus implements IWebSocket {

    private final static String TAG = "dds_JavaWebSocket";

    private WebSocketClient mWebSocketClient;

    private ISignalingEvents events;

    private boolean isOpen; //是否连接成功过
	private String room_id = "000000";
	private long session_id = -1;
	private String transaction_id;
	private long handle_id;
	private int status = 0;
    public JavaWebSocketForJanus(ISignalingEvents events) {
        this.events = events;
    }

    @Override
    public void connect(String wss) {
        URI uri;
        try {
            uri = new URI(wss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if (mWebSocketClient == null) {
			ArrayList<IProtocol> protocols = new ArrayList<IProtocol>();
			protocols.add(new Protocol("janus-protocol"));
            Draft_6455 proto_janus = new Draft_6455(Collections.<IExtension>emptyList(), protocols);

            mWebSocketClient = new WebSocketClient(uri, proto_janus) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isOpen = true;
                    events.onWebSocketOpen();
                }

                @Override
                public void onMessage(String message) {
                    isOpen = true;
                    Log.d(TAG, message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.e(TAG, "onClose:" + reason);
                    if (events != null) {
                        events.onWebSocketOpenFailed(reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, ex.toString());
                    if (events != null) {
                        events.onWebSocketOpenFailed(ex.toString());
                    }
                }
            };
        }
        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                if (sslContext != null) {
                    sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                }

                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }

                if (factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mWebSocketClient.connect();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }

    }


    //============================需要发送的=====================================
    @Override
    public void joinRoom(String room) {
    	room_id = room;
        Map<String, Object> map = new HashMap<>();
        map.put("janus", "create");
		map.put("p2p", "yes");
		map.put("transaction", UUID.randomUUID().toString());
        //Map<String, String> childMap = new HashMap<>();
        //childMap.put("room", room);
        //map.put("data", childMap);
        //map.put("room", room);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
		mWebSocketClient.send(jsonString);
    }

	public void joinToRoom() {
    	//room_id = room;
        Map<String, Object> map = new HashMap<>();
        map.put("janus", "attach");
		map.put("p2p", "yes");
		map.put("session_id", session_id);
		map.put("room", room_id);
		map.put("plugin", "janus.plugin.echotest");
		map.put("transaction", UUID.randomUUID().toString());

		JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
		mWebSocketClient.send(jsonString);
    }


    public void sendAnswer(String socketId, String sdp) {
        HashMap<String, Object> map = new HashMap();
        map.put("janus", "message");
		map.put("p2p", "yes");
		map.put("session_id", session_id);
		//map.put("room", room_id);
		map.put("handle_id", handle_id);
		long si = Long.parseLong(socketId);
		map.put("peer_id", si);
		
		map.put("transaction", UUID.randomUUID().toString());

		HashMap<String, Object> childMap1 = new HashMap();
		childMap1.put("type", "answer");
		childMap1.put("sdp", sdp);

		map.put("jsep", childMap1);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }


    public void sendOffer(String socketId, String sdp) {
        //HashMap<String, Object> childMap1 = new HashMap();
        //childMap1.put("type", "offer");
        //childMap1.put("sdp", sdp);

        //HashMap<String, Object> childMap2 = new HashMap();
        //childMap2.put("socketId", socketId);
        //childMap2.put("sdp", childMap1);

        HashMap<String, Object> map = new HashMap();
        //map.put("eventName", "__offer");
        map.put("janus", "message");
		map.put("p2p", "yes");
		map.put("session_id", session_id);
        //map.put("data", childMap2);
		//map.put("command", "offer");
		
		map.put("room", room_id);
		map.put("handle_id", handle_id);
		long si = Long.parseLong(socketId);
		map.put("peer_id", si);
		
		map.put("transaction", UUID.randomUUID().toString());

		HashMap<String, Object> childMap1 = new HashMap();
		childMap1.put("type", "offer");
		childMap1.put("sdp", sdp);

		map.put("jsep", childMap1);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);

    }

    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        //HashMap<String, Object> childMap = new HashMap();
        //childMap.put("id", iceCandidate.sdpMid);
        //childMap.put("label", iceCandidate.sdpMLineIndex);
        //childMap.put("candidate", iceCandidate.sdp);
        //childMap.put("socketId", socketId);
        HashMap<String, Object> map = new HashMap();
        //map.put("eventName", "__ice_candidate");
        //map.put("data", childMap);
        map.put("janus", "trickle");
		map.put("p2p", "yes");
		
		HashMap<String, Object> candidate = new HashMap();
		candidate.put("sdpMid", iceCandidate.sdpMid);
		candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
		candidate.put("candidate", iceCandidate.sdp);
		
		map.put("candidate", candidate);
		//map.put("room", room_id);
		map.put("session_id", session_id);
		map.put("handle_id", handle_id);
		map.put("transaction", UUID.randomUUID().toString());

		long si = Long.parseLong(socketId);
		map.put("peer_id", si);
		
        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }
    //============================需要发送的=====================================


    //============================需要接收的=====================================
    @Override
    public void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
		String header = (String) map.get("janus");
		
        String eventName = null;
        if(header != null){
			eventName = header;
		}
		Log.i(TAG, "ppt, in handleMessage, eventName: " + eventName);
        if (eventName.equals("success")) {
			String reply = (String)map.get("reply");
			//
			if(reply != null && reply.equals("create")){
				handleJoinRoom(map);
			}
			else if(reply != null && reply.equals("attach")){
				Log.i(TAG, "attach reply");
				
				Map data = (Map) map.get("data");
				handle_id = (long)data.get("id");
				String peers = (String)map.get("connections");
				String sid = session_id + "";
					
				if(peers != null){
					String[] peers_ = peers.split(",", -1);			
                    try{
                        //ArrayList<String> connections = new ArrayList<String>(Arrays.asList(peers_));
                        ArrayList<String> connections = new ArrayList<String>();
                        Method method = connections.getClass().getMethod("add" , Object.class);
                        method.invoke(connections, peers_[0]);
                        events.onJoinToRoom(connections, sid);
                    }catch (Exception eeee){
                        eeee.printStackTrace();
                    }
				}
				else{
					ArrayList<String> connections = new ArrayList<String>();
					events.onJoinToRoom(connections, sid);
				}
			}
        }
		if (eventName.equals("_peers")) {//no, attach replace
            handleJoinToRoom(map);
        }
        if (eventName.equals("_new_peer")) {
            handleRemoteInRoom(map);
        }
       	if (eventName.equals("_ice_candidate")) {//trickle
            handleRemoteCandidate(map);
        }
        if (eventName.equals("_remove_peer")) {
            handleRemoteOutRoom(map);
        }
        if (eventName.equals("event")) {//if (eventName.equals("_offer")) {
        	Map jsep = (Map) map.get("jsep");
			String type = (String)jsep.get("type");
			if(type.equals("offer")){
            	handleOffer(map);
			}
			else if(type.equals("answer")){
				handleAnswer(map);
			}
			else{
				Log.d(TAG, "no support command");
			}
        }
        if (eventName.equals("_answer")) {//no, same to offer(event)
            handleAnswer(map);
        }
		if (eventName.equals("trickle")) {
            handleRemoteCandidate(map);
        }
    }


	private void handleJoinRoom(Map map) {
		Map data = (Map) map.get("data");
		JSONArray arr;
		if (data != null) {
			session_id = (long)data.get("id");
			//events.onJoinRoom(connections, session_id);
			Executors.newSingleThreadExecutor().execute(() -> {
		       joinToRoom();
		    });
		}
	}

    // 自己进入房间
    private void handleJoinToRoom(Map map) {//del
        Map data = (Map) map.get("data");
        JSONArray arr;
        if (data != null) {
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String myId = (String) data.get("you");
            events.onJoinToRoom(connections, myId);
        }

    }

    // 自己已经在房间，有人进来
    private void handleRemoteInRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            long peer_id = (long) data.get("peer_id");
			socketId = peer_id + "";
            events.onRemoteJoinToRoom(socketId);
        }
    }

    // 处理交换信息
    private void handleRemoteCandidate(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            //socketId = (String) data.get("socketId");
            long peer_id = (long) map.get("peer_id");
			socketId = peer_id + "";
            //String sdpMid = (String) data.get("id");
            String sdpMid = (String) data.get("sdpMid");
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            //int label = (int) Double.parseDouble(String.valueOf(data.get("label")));
            int label = (int) Double.parseDouble(String.valueOf(data.get("sdpMLineIndex")));
            String candidate = (String) data.get("candidate");
            IceCandidate iceCandidate = new IceCandidate(sdpMid, label, candidate);
            events.onRemoteIceCandidate(socketId, iceCandidate);
        }
    }

    // 有人离开了房间
    private void handleRemoteOutRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            long peer_id = (long) data.get("peer_id");
			socketId = peer_id + "";
            events.onRemoteOutRoom(socketId);
        }
    }

    // 处理Offer
    private void handleOffer(Map map) {
        //Map data = (Map) map.get("data");
		Map jsep = (Map) map.get("jsep");
        //Map sdpDic;
        if (jsep != null) {
            //sdpDic = (Map) data.get("sdp");
            long peer_id = (long) map.get("peer_id");
            String sdp = (String) jsep.get("sdp");
			String socketId = peer_id + "";
            events.onReceiveOffer(socketId, sdp);
        }
    }

    // 处理Answer
    private void handleAnswer(Map map) {
        //Map data = (Map) map.get("data");
		Map jsep = (Map) map.get("jsep");
        //Map sdpDic;
        //if (data != null) {
		if (jsep != null) {
            //sdpDic = (Map) data.get("sdp");
            long peer_id = (long) map.get("peer_id");
			String socketId = peer_id + "";
            //String sdp = (String) sdpDic.get("sdp");
            String sdp = (String) jsep.get("sdp");
            events.onReceiverAnswer(socketId, sdp);
        }

    }
    //============================需要接收的=====================================


    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


}
