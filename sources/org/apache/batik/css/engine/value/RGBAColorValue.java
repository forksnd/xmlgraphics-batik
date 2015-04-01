/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.css.engine.value;

import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;

/**
 * This class represents RGBA colors.
 *
 * @author <a href="mailto:gadams@apache.org">Glenn Adams</a>
 * @version $Id$
 */
public class RGBAColorValue extends RGBColorValue {

    /**
     * The alpha component.
     */
    protected Value alpha;

    /**
     * Creates a new RGBAColorValue.
     */
    public RGBAColorValue(Value r, Value g, Value b, Value a) {
        super(r, g, b);
        alpha = a;
    }

    /**
     * A string representation of the current value.
     */
    public String getCssText() {
        return "rgba(" +
            red.getCssText() + ", " +
            green.getCssText() + ", " +
            blue.getCssText() + ", " +
            alpha.getCssText() + ')';
    }

    /**
     * Implements {@link Value#getAlpha()}.
     */
    public Value getAlpha() throws DOMException {
        return alpha;
    }
}