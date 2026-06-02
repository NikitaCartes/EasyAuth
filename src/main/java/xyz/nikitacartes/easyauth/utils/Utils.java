package xyz.nikitacartes.easyauth.utils;

import com.google.common.net.InetAddresses;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Utils {

    @Nullable
    public static String getIp(SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress() != null ? InetAddresses.toAddrString(inetSocketAddress.getAddress()) : null;
        } else {
            return "<unknown>";
        }
    }
}
