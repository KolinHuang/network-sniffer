package com.hyc.backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * @author kol Huang
 * @date 2021/3/23
 */
@SuppressWarnings("all")
public class NetworkUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * 获取本地IP地址以及MAC地址，遍历多网卡，找出en0
     * @return
     */
    public static Map<String, String> getLocalAddress(){
        Map<String, String> addrs;
        try{
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while(inetAddresses.hasMoreElements()){
                    InetAddress inetAddress = inetAddresses.nextElement();
                    addrs = chooseLocalInetAddress(inetAddress, networkInterface);
                    if(addrs != null){
                        logger.info("local ip and mac address acquired!", addrs);
                        return addrs;
                    }

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取公网地址
     * @return
     */
    public static Map<String, String> getPublicAddress(){
        Map<String, String> addrs;
        try{
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while(inetAddresses.hasMoreElements()){
                    InetAddress inetAddress = inetAddresses.nextElement();
                    addrs = choosePublicAddress(inetAddress, networkInterface);
                    if(addrs != null){
                        logger.info("public ip and mac address acquired!", addrs);
                        return addrs;
                    }

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> choosePublicAddress(InetAddress inetAddress, NetworkInterface networkInterface){
        try{
            String name = networkInterface.getDisplayName();
            if(name.contains("Adapter") ||
                name.contains("Virtual") ||
                name.contains("VMnet") ||
                name.contains("#")){
                return null;
            }
            if(networkInterface.isVirtual() ||
                    !networkInterface.isUp() ||
                    !networkInterface.supportsMulticast()){
                return null;
            }
            if(!inetAddress.isSiteLocalAddress() &&
                    !inetAddress.isLoopbackAddress() &&
                    inetAddress.getHostAddress().indexOf(":") == -1){
                Formatter formatter = new Formatter();
                String srcMAC = null;
                byte[] macBuf = networkInterface.getHardwareAddress();
                for(int i = 0; i < macBuf.length; ++i){
                    srcMAC = formatter.format(Locale.getDefault(),
                            "%02X%s",macBuf[i],
                            (i < macBuf.length - 1) ? "-" : "")
                            .toString();
                }
                formatter.close();
                Map<String, String> info = new HashMap<>();
                info.put("hostname", inetAddress.getHostName());
                info.put("ip", inetAddress.getHostAddress());
                info.put("ipnet", inetAddressTypeName(inetAddress));
                info.put("os", System.getProperty("os.name"));
                info.put("cpu-arch", System.getProperty("os.arch"));
                info.put("network-arch", networkInterface.getDisplayName());
                return info;

            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> chooseLocalInetAddress(InetAddress inetAddress, NetworkInterface networkInterface){
        try{
            String name = networkInterface.getDisplayName();
            if(name.contains("Adapter") ||
                    name.contains("Virtual") ||
                    name.contains("VMnet") ||
                    name.contains("#")){
                return null;
            }
            if(networkInterface.isVirtual() ||
                    !networkInterface.isUp() ||
                    !networkInterface.supportsMulticast()){
                return null;
            }
            if(inetAddress.isSiteLocalAddress() &&
                    !inetAddress.isLoopbackAddress() &&
                    inetAddress.getHostAddress().indexOf(":") == -1){
                Formatter formatter = new Formatter();
                String srcMAC = null;
                byte[] macBuf = networkInterface.getHardwareAddress();
                for(int i = 0; i < macBuf.length; ++i){
                    srcMAC = formatter.format(Locale.getDefault(),
                            "%02X%s",macBuf[i],
                            (i < macBuf.length - 1) ? "-" : "")
                            .toString();
                }
                formatter.close();
                Map<String, String> info = new HashMap<>();
                info.put("hostname", inetAddress.getHostName());
                info.put("ip", inetAddress.getHostAddress());
                info.put("mac", srcMAC);
                info.put("ipnet", inetAddressTypeName(inetAddress));
                info.put("os", System.getProperty("os.name"));
                info.put("cpu-arch", System.getProperty("os.arch"));
                info.put("network-arch", networkInterface.getDisplayName());
                return info;

            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String inetAddressTypeName(InetAddress inetAddress) {
        return (inetAddress instanceof Inet4Address) ? "ipv4" : "ipv6";
    }

    public static byte[] stomac(String s){
        if(StringUtils.isEmpty(s))  return null;

        byte[] mac = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        String[] s1 = s.split(":");
        if (s1 == null || s1.length == 0)
            s1 = s.split("-");
        for (int x = 0; x < s1.length; x++) {
            mac[x] = (byte) ((Integer.parseInt(s1[x], 16)) & 0xff);
        }
        return mac;
    }

    public static Set<String> lookup(String domain){
        if(!StringUtils.hasLength(domain))  return null;
        Set<String> inetAddresses = new HashSet<>();
        try {
            InetAddress[] arr = InetAddress.getAllByName(domain);
            for (InetAddress inetAddress : arr) {
                inetAddresses.add(inetAddress.getHostAddress());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return inetAddresses;
    }

    public static String bytesToIp(byte[] src){
        if(src == null && src.length == 0)  return null;
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff)
                + "." + (src[3] & 0xff);
    }

    public static String bytesToMac(byte[] macBytes) {
        if (macBytes == null)
            return null;
        String value = "";
        for (int i = 0; i < macBytes.length; i++) {
            String sTemp = Integer.toHexString(0xFF & macBytes[i]);
            value = value + sTemp + ":";
        }
        value = value.substring(0, value.lastIndexOf(":"));
        return value;
    }

    public static String bytesToHexString(byte[] b) {
        if (b == null || b.length == 0)
            return null;
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1)
                hs = hs + "0" + stmp;
            else
                hs = hs + stmp;
        }
        return hs.toUpperCase();
    }

    public static Map<String, String> readHttpHeadersByStr(String headersStr) {
        Map<String, String> httpHeaders = new HashMap<>();
        try {
            BufferedReader in = new BufferedReader(new StringReader(new String(headersStr)));
            String method = in.readLine();
            if (method == null || method.indexOf("HTTP") == -1) {
                System.out.println("Not HTTP Header: " + new String(headersStr));
                return httpHeaders;
            }
            httpHeaders.put("METHOD", method); //request or response can be seen here

            String l;
            //read headers
            while ((l = in.readLine()) != null && l.length() > 0) {
                String[] header = l.split(":");
                if (header.length > 1) {
                    httpHeaders.put(header[0].toLowerCase(), header[1].trim());
                } else {
                    System.out.println("------------- unknown header: " + l);
                }
            }
            return httpHeaders;
        } catch (IOException e) {
            return httpHeaders;
        }
    }


    public static byte[] toBytes(String str) {
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }

        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }

        return bytes;
    }

    public static String parseGzipContent(String hexData) {
        if (hexData == null)
            return null;
        int indexOfGzipContent = hexData.indexOf("0D0A");
        if (indexOfGzipContent > 0) {
            String contentHex;
            if (indexOfGzipContent + 4 != hexData.length())
                contentHex = hexData.substring(indexOfGzipContent + 4);
            else
                contentHex = hexData.substring(0, indexOfGzipContent);

            int indexOfGzipEnd = contentHex.indexOf("0D0A300D0A");
            if (indexOfGzipEnd > 0) {
                contentHex = contentHex.substring(0, indexOfGzipEnd);
            }
            return contentHex;
        }
        return hexData;
    }


    public static byte[] concatBytes(byte[] data1, byte[] data2) {
        if(data1 == null){
            data1 = new byte[0];
        }
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    public static String[] concatStrings(String[] data1, String[] data2) {
        String[] data3 = new String[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
    public static byte[] unGzip(byte[] data) {
        byte[] b;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            b = baos.toByteArray();
            baos.flush();
            baos.close();
            gzip.close();
            bis.close();
        } catch (ZipException e) {
            System.err.println("-------Not gzip--------");
            return data;
        } catch (Exception ex) {
            ex.printStackTrace();
            return data;
        }
        return b;
    }

    private static List<String> reqMethods = Arrays.asList("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TARCE");

    public static boolean isRequestHttpHeader(Map<String, String> httpHeaders) {
        if (httpHeaders != null && httpHeaders.containsKey("METHOD")) {
            for (String reqMethod : reqMethods) {
                if (httpHeaders.get("METHOD").startsWith(reqMethod)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isResponseHttpHeader(Map<String, String> httpHeaders) {
        if (httpHeaders != null && httpHeaders.containsKey("METHOD") && httpHeaders.get("METHOD").startsWith("HTTP/1")) {
            return true;
        }
        return false;
    }

    public static boolean isHttpHeader(Map<String, String> httpHeaders) {
        if (httpHeaders != null && httpHeaders.containsKey("METHOD"))
            return true;
        else
            return false;
    }
}