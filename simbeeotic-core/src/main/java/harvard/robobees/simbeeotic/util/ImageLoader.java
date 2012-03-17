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


import java.awt.*;


/**
 * A utility for loading an image from the multiple soruces.
 *
 * @author bkate
 */
public class ImageLoader {

    /**
     * Loads an image from a resource that is available on the classpath. Classpath
     * resources can be accessed by an absolute path that is rooted in the jar file
     * containing the image. For example, if a classpath entry (jar file) contained
     * a relative path {@code images/foo.jpg}, the path passed to this method would
     * be {@code /images/foo.jpg}.
     *
     * @param path The path to the image to be loaded.
     *
     * @return The loaded image.
     */
    public static Image loadImageFromClasspath(String path) {
        return Toolkit.getDefaultToolkit().getImage(ImageLoader.class.getResource(path));
    }


    /**
     * Loads an image that is stored on the local filesystem. The path given should
     * either be relative to the working directory or an absolute path, delimited
     * by the '/' character.
     *
     * @param path The path to the image file on the filesystem.
     *
     * @return The loaded binary image.
     */
    public static Image loadImageFromFilesystem(String path) {
        return Toolkit.getDefaultToolkit().getImage(path);
    }
}
