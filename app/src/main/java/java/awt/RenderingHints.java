/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package java.awt;

import android.support.annotation.NonNull;

import org.apache.poi2.util.Internal;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Yegor Kozlov
 */
public class RenderingHints implements Map<Object, Object>, Cloneable  {

    public RenderingHints(int i){
//        super(i);
    }

//    @Override
//    public boolean isCompatibleValue(Object val) {
//        return true;
//    }

    public static final RenderingHints GSAVE = new RenderingHints(1);
    public static final RenderingHints GRESTORE = new RenderingHints(2);

    /**
     * Use a custom image rendener
     *
     */
    public static final RenderingHints IMAGE_RENDERER = new RenderingHints(3);

    /**
     *  how to render text:
     *
     *  {@link #TEXT_AS_CHARACTERS} (default) means to draw via
     *   {@link and.awt.Graphics2D#drawString(java.text.AttributedCharacterIterator, float, float)}.
     *   This mode draws text as characters. Use it if the target graphics writes the actual
     *   character codes instead of glyph outlines (PDFGraphics2D, SVGGraphics2D, etc.)
     *
     *   {@link #TEXT_AS_SHAPES} means to render via
     *   {@link and.awt.font.TextLayout#draw(and.awt.Graphics2D, float, float)}.
     *   This mode draws glyphs as shapes and provides some advanced capabilities such as
     *   justification and font substitution. Use it if the target graphics is an image.
     *
     */
    public static final RenderingHints TEXT_RENDERING_MODE = new RenderingHints(4);

    /**
     * draw text via {@link and.awt.Graphics2D#drawString(java.text.AttributedCharacterIterator, float, float)}
     */
    public static final int TEXT_AS_CHARACTERS = 1;

    /**
     * draw text via {@link and.awt.font.TextLayout#draw(and.awt.Graphics2D, float, float)}
     */
    public static final int TEXT_AS_SHAPES = 2;

    @Internal
    static final RenderingHints GROUP_TRANSFORM = new RenderingHints(5);

    /**
     * Use this object to resolve unknown / missing fonts when rendering slides
     */
    public static final RenderingHints FONT_HANDLER = new RenderingHints(6);

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public Object put(Object key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(@NonNull Map<?, ?> m) {

    }

    @Override
    public void clear() {

    }

    @NonNull
    @Override
    public Set<Object> keySet() {
        return null;
    }

    @NonNull
    @Override
    public Collection<Object> values() {
        return null;
    }

    @NonNull
    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return null;
    }

    public abstract static class Key {
        private static HashMap<Object, Object> identitymap = new HashMap(17);
        private int privatekey;

        private String getIdentity() {
            return this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this.getClass())) + ":" + Integer.toHexString(this.privatekey);
        }

        private static synchronized void recordIdentity(RenderingHints.Key var0) {
            String var1 = var0.getIdentity();
            Object var2 = identitymap.get(var1);
            if (var2 != null) {
                RenderingHints.Key var3 = (RenderingHints.Key)((WeakReference)var2).get();
                if (var3 != null && var3.getClass() == var0.getClass()) {
                    throw new IllegalArgumentException(var1 + " already registered");
                }
            }

            identitymap.put(var1, new WeakReference(var0));
        }

        protected Key(int var1) {
            this.privatekey = var1;
            recordIdentity(this);
        }

        public abstract boolean isCompatibleValue(Object var1);

        protected final int intKey() {
            return this.privatekey;
        }

        public final int hashCode() {
            return super.hashCode();
        }

        public final boolean equals(Object var1) {
            return this == var1;
        }
    }

}
