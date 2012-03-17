/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
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
