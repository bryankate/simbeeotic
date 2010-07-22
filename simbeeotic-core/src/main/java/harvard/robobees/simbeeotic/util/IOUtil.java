package harvard.robobees.simbeeotic.util;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A class with utility methods for dealing with IO.
 *
 * @author bkate
 */
public class IOUtil {

    /**
     * Converts a list of objects to a byte stream. The objects can only
     * be of the type {@code Number}, {@code Boolean}, {@code String}, or {@code byte[]}.
     * Any other type of object will be skipped over.
     *
     * @see DataOutputStream for a description of the resulting byte order.
     *
     * @param objects The list of objects to encode.
     *
     * @return The list of objects, represented as a stream of bytes.
     */
    public static byte[] toBytes(Object ... objects) {

        ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(arrayStream);

        try {

            for (Object obj : objects) {

                if (obj instanceof Number) {

                    Number num = (Number)obj;

                    if (num instanceof Byte) {
                        out.writeByte(num.byteValue());
                    }
                    else if (num instanceof Short) {
                        out.writeShort(num.shortValue());
                    }
                    else if (num instanceof Integer) {
                        out.writeInt(num.intValue());
                    }
                    else if (num instanceof Long) {
                        out.writeLong(num.longValue());
                    }
                    else if (num instanceof Float) {
                        out.writeFloat(num.floatValue());
                    }
                    else if (num instanceof Double) {
                        out.writeDouble(num.doubleValue());
                    }
                }
                else if (obj instanceof Boolean) {
                    out.writeBoolean((Boolean)obj);
                }
                else if (obj instanceof String) {
                    out.writeUTF((String)obj);
                }
                else if (obj instanceof byte[]) {
                    out.write((byte[])obj);
                }
            }
        }
        catch(IOException ioe) {
            throw new RuntimeException("Problem converting number to bytes.", ioe);
        }

        return arrayStream.toByteArray();
    }
}
