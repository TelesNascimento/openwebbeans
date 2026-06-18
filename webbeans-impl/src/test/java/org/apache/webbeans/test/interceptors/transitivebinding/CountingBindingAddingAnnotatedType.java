/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.interceptors.transitivebinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * Wraps the AnnotatedType of a binding annotation and adds @CountingBinding to it, so the
 * binding becomes transitively bound to @CountingBinding through the
 * addInterceptorBinding(AnnotatedType) registration form.
 */
public class CountingBindingAddingAnnotatedType<X> implements AnnotatedType<X>
{
    private final AnnotatedType<X> delegate;
    private final Set<Annotation> annotations;

    public CountingBindingAddingAnnotatedType(AnnotatedType<X> delegate)
    {
        this.delegate = delegate;
        this.annotations = new HashSet<>(delegate.getAnnotations());
        this.annotations.add(CountingBinding.Literal.INSTANCE);
    }

    @Override
    public Set<Annotation> getAnnotations()
    {
        return annotations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType)
    {
        if (annotationType.equals(CountingBinding.class))
        {
            return (T) CountingBinding.Literal.INSTANCE;
        }
        return delegate.getAnnotation(annotationType);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
        return annotationType.equals(CountingBinding.class) || delegate.isAnnotationPresent(annotationType);
    }

    @Override
    public Class<X> getJavaClass()
    {
        return delegate.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors()
    {
        return delegate.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods()
    {
        return delegate.getMethods();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields()
    {
        return delegate.getFields();
    }

    @Override
    public Type getBaseType()
    {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure()
    {
        return delegate.getTypeClosure();
    }
}
