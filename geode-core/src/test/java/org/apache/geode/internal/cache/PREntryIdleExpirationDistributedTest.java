/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.geode.cache.ExpirationAction.DESTROY;
import static org.apache.geode.cache.RegionShortcut.PARTITION;
import static org.apache.geode.test.dunit.Host.getHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Stopwatch;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.PartitionAttributesFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.rules.CacheRule;
import org.apache.geode.test.dunit.rules.DistributedTestRule;
import org.apache.geode.test.junit.categories.DistributedTest;

@Category(DistributedTest.class)
public class PREntryIdleExpirationDistributedTest implements Serializable {

  @ClassRule
  public static DistributedTestRule distributedTestRule = new DistributedTestRule();

  private static final AtomicBoolean KEEP_READING = new AtomicBoolean(true);

  private static final String KEY = "KEY";
  private static final String VALUE = "VALUE";

  private final VM member1 = getHost(0).getVM(0);
  private final VM member2 = getHost(0).getVM(1);
  private final VM member3 = getHost(0).getVM(2);
  private final String regionName = getClass().getSimpleName();

  @Rule
  public CacheRule cacheRule = CacheRule.builder().createCacheIn(member1).createCacheIn(member2)
      .createCacheIn(member3).createCacheIn(getHost(0).getVM(3)).build();

  @Before
  public void setUp() throws Exception {
    VM[] vms = new VM[] {member1, member2, member3};
    for (VM vm : vms) {
      vm.invoke(() -> {
        KEEP_READING.set(true);
        ExpiryTask.suspendExpiration();
        createRegion();
      });
    }

    // make member1 the primary bucket for KEY
    member1.invoke(() -> {
      Region<String, String> region = cacheRule.getCache().getRegion(regionName);
      region.put(KEY, VALUE);
    });
  }

  @After
  public void tearDown() throws Exception {
    VM[] vms = new VM[] {member1, member2, member3};
    for (VM vm : vms) {
      vm.invoke(() -> {
        KEEP_READING.set(false);
        ExpiryTask.permitExpiration();
      });
    }
  }

  @Test
  public void readsInOtherMemberShouldPreventExpiration() throws Exception {
    AsyncInvocation<?> memberReading = member3.invokeAsync(() -> {
      Region<String, String> region = cacheRule.getCache().getRegion(regionName);
      region.put(KEY, VALUE);
      while (KEEP_READING.get()) {
        region.get(KEY);
        Thread.sleep(10);
      }
    });

    member2.invoke(() -> {
      Region<String, String> region = cacheRule.getCache().getRegion(regionName);
      await().atMost(30, SECONDS).until(() -> region.containsKey(KEY));
      assertThat(region.containsKey(KEY)).isTrue();
    });

    member1.invoke(() -> {
      Region<String, String> region = cacheRule.getCache().getRegion(regionName);
      await().atMost(30, SECONDS).until(() -> region.containsKey(KEY));
      assertThat(region.containsKey(KEY)).isTrue();

      ExpiryTask.permitExpiration();
      Stopwatch stopwatch = Stopwatch.createStarted();
      while (stopwatch.elapsed(SECONDS) <= 5 && region.containsKey(KEY)) {
        Thread.sleep(10);
      }
      assertThat(region.containsKey(KEY)).isTrue();
    });

    member3.invoke(() -> KEEP_READING.set(false));

    memberReading.await();
  }

  private void createRegion() {
    RegionFactory<String, String> factory = cacheRule.getCache().createRegionFactory(PARTITION);
    factory.setPartitionAttributes(
        new PartitionAttributesFactory<String, String>().setRedundantCopies(2).create());
    factory.setEntryIdleTimeout(new ExpirationAttributes(1, DESTROY));
    factory.create(regionName);
  }
}
