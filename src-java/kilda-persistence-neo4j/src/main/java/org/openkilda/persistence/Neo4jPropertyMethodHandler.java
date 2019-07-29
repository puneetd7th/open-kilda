/* Copyright 2020 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.persistence;

import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.annotations.Property;
import com.syncleus.ferma.framefactories.annotation.CachesReflection;
import com.syncleus.ferma.framefactories.annotation.PropertyMethodHandler;
import com.syncleus.ferma.framefactories.annotation.ReflectionUtility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A method handler that implemented the Property Annotation with specific support of Neo4j data types.
 */
class Neo4jPropertyMethodHandler extends PropertyMethodHandler {

    @Override
    public <E> DynamicType.Builder<E> processMethod(DynamicType.Builder<E> builder, Method method,
                                                    Annotation annotation) {
        java.lang.reflect.Parameter[] arguments = method.getParameters();

        if (ReflectionUtility.isSetMethod(method)
                && arguments != null && arguments.length == 1) {
            return builder.method(ElementMatchers.is(method))
                    .intercept(MethodDelegation.to(SetPropertyInterceptor.class));

        } else if (ReflectionUtility.isGetMethod(method)
                && (arguments == null || arguments.length == 0)) {
            return builder.method(ElementMatchers.is(method))
                    .intercept(MethodDelegation.to(GetPropertyInterceptor.class));
        }

        return super.processMethod(builder, method, annotation);
    }

    /**
     * A method interceptor for getters.
     */
    public static final class GetPropertyInterceptor {
        /**
         * The interceptor implementation.
         */
        @RuntimeType
        public static Object getProperty(@This final ElementFrame thiz, @Origin final Method method) {
            final Property annotation
                    = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Property.class);
            final String value = annotation.value();

            final Object obj = thiz.getProperty(value);
            Class<?> returnType = method.getReturnType();
            // Neo4j implementation doesn't support Integer as a property type
            if (obj != null && (returnType.isAssignableFrom(int.class) || returnType.isAssignableFrom(Integer.class))) {
                return ((Long) obj).intValue();
            } else if (returnType.isEnum()) {
                return getValueAsEnum(method, obj);
            } else {
                return obj;
            }
        }

        private static Enum getValueAsEnum(final Method method, final Object value) {
            final Class<Enum> en = (Class<Enum>) method.getReturnType();
            if (value != null) {
                return Enum.valueOf(en, value.toString());
            }

            return null;
        }
    }

    /**
     * A method interceptor for setters.
     */
    public static final class SetPropertyInterceptor {
        /**
         * The interceptor implementation.
         */
        @RuntimeType
        public static void setProperty(@This final ElementFrame thiz, @Origin final Method method,
                                       @RuntimeType @Argument(0) final Object obj) {
            final Property annotation
                    = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Property.class);
            final String value = annotation.value();

            if ((obj != null) && (obj.getClass().isEnum())) {
                thiz.setProperty(value, ((Enum<?>) obj).name());
            } else {
                if (obj instanceof Integer) {
                    // Neo4j implementation doesn't support Integer as a property type
                    thiz.setProperty(value, ((Integer) obj).longValue());
                } else {
                    thiz.setProperty(value, obj);
                }
            }
        }
    }
}
