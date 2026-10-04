package org.adam.zenithx.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PacketCompressor {

    private static final int COMPRESSION_THRESHOLD = 256;
    private static final byte COMPRESSED_FLAG = (byte) 0x7E;
    private static final byte UNCOMPRESSED_FLAG = (byte) 0x7F;

    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) return new byte[0];

        byte[] payload;
        byte flag;

        if (data.length < COMPRESSION_THRESHOLD) {
            flag = UNCOMPRESSED_FLAG;
            payload = data;
        } else {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
                gzos.finish();
                payload = baos.toByteArray();
                flag = COMPRESSED_FLAG;
            } catch (Exception e) {
                flag = UNCOMPRESSED_FLAG;
                payload = data;
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(out)) {
            // [ Length (4 bytes) | Flag (1 byte) | Payload ]
            dos.writeInt(payload.length + 1);
            dos.writeByte(flag);
            dos.write(payload);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static byte[] decompressPayload(byte flag, byte[] payload) {
        if (flag == UNCOMPRESSED_FLAG) {
            return payload;
        } else if (flag == COMPRESSED_FLAG) {
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(payload));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            } catch (Exception e) {
                System.err.println("[ZenithX] Decompression error: " + e.getMessage());
            }
        }
        return payload;
    }
}