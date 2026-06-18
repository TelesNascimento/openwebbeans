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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Registers, programmatically, several interceptor bindings whose transitive binding chains
 * end at @CountingBinding (or @ValueBinding), exercising both registration forms.
 */
public class TransitiveBindingExtension implements Extension
{
    public void register(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager)
    {
        // Class form: @TransitiveBinding -> @CountingBinding
        beforeBeanDiscovery.addInterceptorBinding(TransitiveBinding.class, CountingBinding.Literal.INSTANCE);

        // Multi level: @Level1 -> @Level2 -> @CountingBinding
        beforeBeanDiscovery.addInterceptorBinding(Level2.class, CountingBinding.Literal.INSTANCE);
        beforeBeanDiscovery.addInterceptorBinding(Level1.class, Level2.Literal.INSTANCE);

        // Cycle: @CycleA -> {@CycleB, @CountingBinding}, @CycleB -> @CycleA (must terminate)
        beforeBeanDiscovery.addInterceptorBinding(CycleA.class, CycleB.Literal.INSTANCE, CountingBinding.Literal.INSTANCE);
        beforeBeanDiscovery.addInterceptorBinding(CycleB.class, CycleA.Literal.INSTANCE);

        // AnnotatedType form: @AtBinding carries @CountingBinding through the wrapped AnnotatedType
        beforeBeanDiscovery.addInterceptorBinding(
                new CountingBindingAddingAnnotatedType<>(beanManager.createAnnotatedType(AtBinding.class)));

        // Nonbinding: @ToValue -> @ValueBinding("registered"); ValueInterceptor is bound to @ValueBinding()
        beforeBeanDiscovery.addInterceptorBinding(ToValue.class, new ValueBinding.Literal("registered"));

        // Repeatable target: @ToCounted -> @Counted (which is @Repeatable)
        beforeBeanDiscovery.addInterceptorBinding(ToCounted.class, Counted.Literal.INSTANCE);

        // Diamond: @DiamondTop -> {@DiamondLeft, @DiamondRight}, both -> @CountingBinding (must dedup to one)
        beforeBeanDiscovery.addInterceptorBinding(DiamondTop.class, DiamondLeft.Literal.INSTANCE, DiamondRight.Literal.INSTANCE);
        beforeBeanDiscovery.addInterceptorBinding(DiamondLeft.class, CountingBinding.Literal.INSTANCE);
        beforeBeanDiscovery.addInterceptorBinding(DiamondRight.class, CountingBinding.Literal.INSTANCE);

        // Cross form: @CrossTop -> @CrossMid via the Class form, @CrossMid -> @CountingBinding via the AnnotatedType form
        beforeBeanDiscovery.addInterceptorBinding(CrossTop.class, CrossMid.Literal.INSTANCE);
        beforeBeanDiscovery.addInterceptorBinding(
                new CountingBindingAddingAnnotatedType<>(beanManager.createAnnotatedType(CrossMid.class)));

        // Self meta annotation: @SelfMeta -> {@SelfMeta, @CountingBinding} (must terminate)
        beforeBeanDiscovery.addInterceptorBinding(SelfMeta.class, SelfMeta.Literal.INSTANCE, CountingBinding.Literal.INSTANCE);
    }
}
