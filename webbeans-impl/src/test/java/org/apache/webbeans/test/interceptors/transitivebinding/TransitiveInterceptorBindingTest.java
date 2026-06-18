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

import static org.junit.Assert.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * A portable extension registers interceptor bindings programmatically and gives them a
 * transitive binding (both the addInterceptorBinding(Class, Annotation...) form and the
 * addInterceptorBinding(AnnotatedType) form). A bean annotated only with such a binding must
 * be intercepted by the interceptor of its transitive binding, exactly as it is on Weld.
 */
public class TransitiveInterceptorBindingTest extends AbstractUnitTest
{
    private void start(Class<?>... beans)
    {
        Class<?>[] all = new Class<?>[beans.length + 4];
        all[0] = CountingInterceptor.class;
        all[1] = ValueInterceptor.class;
        all[2] = CountedInterceptor.class;
        all[3] = ParentBoundInterceptor.class;
        System.arraycopy(beans, 0, all, 4, beans.length);
        addExtension(new TransitiveBindingExtension());
        startContainer(all);
        CountingInterceptor.reset();
        ValueInterceptor.reset();
        CountedInterceptor.reset();
        ParentBoundInterceptor.reset();
    }

    @Test
    public void classForm()
    {
        start(ClassFormService.class);
        getInstance(ClassFormService.class).run();
        assertEquals(1, CountingInterceptor.count.get());
    }

    @Test
    public void multiLevelChain()
    {
        start(MultiLevelService.class);
        getInstance(MultiLevelService.class).run();
        assertEquals("Level1 -> Level2 -> @CountingBinding must resolve", 1, CountingInterceptor.count.get());
    }

    @Test
    public void cycleTerminatesAndResolves()
    {
        start(CycleService.class);
        getInstance(CycleService.class).run();
        assertEquals("@CycleA -> @CycleB -> @CycleA cycle must terminate and still apply @CountingBinding",
                1, CountingInterceptor.count.get());
    }

    @Test
    public void annotatedTypeForm()
    {
        start(AnnotatedTypeService.class);
        getInstance(AnnotatedTypeService.class).run();
        assertEquals(1, CountingInterceptor.count.get());
    }

    @Test
    public void lifecycleInterceptionViaTransitiveBinding()
    {
        start(ClassFormService.class);
        getInstance(ClassFormService.class).run();
        assertEquals("@PostConstruct interceptor must fire for a transitively bound bean", 1,
                CountingInterceptor.postConstructCount.get());
    }

    @Test
    public void nonbindingMemberOnExpandedBinding()
    {
        start(NonbindingService.class);
        getInstance(NonbindingService.class).run();
        assertEquals("@ToValue -> @ValueBinding(\"registered\") must match @ValueBinding() (value is @Nonbinding)",
                1, ValueInterceptor.count.get());
    }

    @Test
    public void unrelatedBeanIsNotOverIntercepted()
    {
        start(PlainService.class);
        getInstance(PlainService.class).run();
        assertEquals("a bean with no registered binding must not be intercepted", 0, CountingInterceptor.count.get());
    }

    @Test
    public void aroundConstructViaTransitiveBinding()
    {
        start(AroundConstructService.class);
        getInstance(AroundConstructService.class);
        assertEquals("@AroundConstruct interceptor must fire for a transitively bound bean", 1,
                CountingInterceptor.aroundConstructCount.get());
    }

    @Test
    public void interceptorBoundToParentBindingDoesNotMatchLeafBoundBean()
    {
        start(LeafBoundService.class);
        getInstance(LeafBoundService.class).run();
        assertEquals("an interceptor bound to the parent binding must not intercept a bean bound only to the leaf",
                0, ParentBoundInterceptor.count.get());
        assertEquals("the leaf-bound interceptor must still intercept the leaf-bound bean", 1,
                CountingInterceptor.count.get());
    }

    @Test
    public void diamondTransitiveBindingResolvesExactlyOnce()
    {
        start(DiamondService.class);
        getInstance(DiamondService.class).run();
        assertEquals("@DiamondTop -> {@DiamondLeft, @DiamondRight} -> @CountingBinding must resolve once", 1,
                CountingInterceptor.count.get());
    }

    @Test
    public void crossFormTransitiveChainResolves()
    {
        start(CrossFormService.class);
        getInstance(CrossFormService.class).run();
        assertEquals("@CrossTop (Class form) -> @CrossMid (AnnotatedType form) -> @CountingBinding must resolve", 1,
                CountingInterceptor.count.get());
    }

    @Test
    public void selfMetaAnnotatedBindingTerminatesAndResolves()
    {
        start(SelfMetaService.class);
        getInstance(SelfMetaService.class).run();
        assertEquals("@SelfMeta -> {@SelfMeta, @CountingBinding} must terminate and apply @CountingBinding", 1,
                CountingInterceptor.count.get());
    }

    @Test
    public void repeatableBindingType()
    {
        start(RepeatableService.class);
        getInstance(RepeatableService.class).run();
        assertEquals("@ToCounted -> @Counted (a @Repeatable binding) must resolve", 1, CountedInterceptor.count.get());
    }

    @Test
    public void stereotypeCarryingTransitiveBinding()
    {
        start(StereotypeService.class);
        getInstance(StereotypeService.class).run();
        assertEquals("a stereotype carrying @TransitiveBinding must expand to @CountingBinding", 1,
                CountingInterceptor.count.get());
    }

    @ApplicationScoped
    @TransitiveBinding
    public static class ClassFormService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @Level1
    public static class MultiLevelService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @CycleA
    public static class CycleService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @AtBinding
    public static class AnnotatedTypeService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @ToValue
    public static class NonbindingService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    public static class PlainService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @ToCounted
    public static class RepeatableService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @TransitivelyBoundStereotype
    public static class StereotypeService
    {
        public void run()
        {
        }
    }

    @Dependent
    @TransitiveBinding
    public static class AroundConstructService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @CountingBinding
    public static class LeafBoundService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @DiamondTop
    public static class DiamondService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @CrossTop
    public static class CrossFormService
    {
        public void run()
        {
        }
    }

    @ApplicationScoped
    @SelfMeta
    public static class SelfMetaService
    {
        public void run()
        {
        }
    }
}
