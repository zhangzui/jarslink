/*
 *
 *  * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.alipay.jarslink.api.impl;

import com.alipay.jarslink.api.Module;
import com.alipay.jarslink.api.ModuleConfig;
import com.alipay.jarslink.api.ModuleLoader;
import com.alipay.jarslink.api.ModuleManager;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static com.alipay.jarslink.api.impl.ModuleLoaderImplTest.buildModuleConfig;
import static com.alipay.jarslink.api.impl.ModuleLoaderImplTest.buildModuleConfigZZZ;

/**
 * JarsLink API入口,使用TITAN API必须继承AbstractModuleRefreshScheduler然后提供模块信息
 *
 * @author tengfei.fangtf
 * @version $Id: AbstractModuleRefreshSchedulerTest.java, v 0.1 2017年06月26日 10:01 AM tengfei.fangtf Exp $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:META-INF/spring/jarslink.xml",
        "classpath*:META-INF/spring/jarslink-schedule.xml"})
public class ZRefreshSchedulerTest {

    @Autowired
    private AbstractModuleRefreshSchedulerImpl abstractModuleRefreshSchedulerImpl;

    @Autowired
    private ModuleManager moduleManager;

    @Autowired
    private ModuleLoader moduleLoader;

    @Test
    public void shouldUpdateModulezzz() throws InterruptedException {
        //装载模块
        Module module = moduleLoader.load(buildModuleConfigZZZ("helloworld",true,"1.0.0"));
        moduleManager.register(module);

        //修改模块
        Module module2 = moduleLoader.load(buildModuleConfigZZZ("helloWorld",true,"1.0.1"));
        moduleManager.register(module2);


        Module demo1 = moduleManager.find("helloWorld","1.0.0");
        Module demo2 = moduleManager.find("helloWorld","1.0.1");

        System.out.println("demo1 = " + demo1.doAction("helloWorld", "zzz"));

        System.out.println("demo2 = " + demo2.doAction("helloWorld", "zzz"));

    }
    @Test
    public void versionTest() throws InterruptedException {
        //注册版本1
        ModuleConfig moduleConfig1 = buildModuleConfigZZZ("helloworld",true,"1.0.0");
        Module module1 = moduleLoader.load(moduleConfig1);
        moduleManager.register(module1);

        //注册版本2
        ModuleConfig moduleConfig2 = buildModuleConfigZZZ("helloworld",true,"1.0.1");
        Module module2 = moduleLoader.load(moduleConfig2);
        moduleManager.register(module2);

        //测试1.0.0
        Module demo1 = moduleManager.find("helloWorld","1.0.0");
        String result1 = demo1.doAction("helloWorld", "xxxx");
        System.out.println("result1 = " + result1);

        //测试1.0.1
        Module demo2 = moduleManager.find(moduleConfig2.getName(), moduleConfig2.getVersion());
        String result2 = demo2.doAction("helloWorld", "xxxx");
        System.out.println("result2 = " + result2);
    }

    /**
     * 同时注册两个，如果name一样，会失败
     * 不同的name可以注册进去
     * @throws InterruptedException
     */
    @Test
    public void shouldUpdateModule2() throws InterruptedException {
        //装载模块
        List<ModuleConfig> moduleConfigs = new ArrayList<>();
        moduleConfigs.add(buildModuleConfigZZZ("helloworld",true,"1.0.0"));
        moduleConfigs.add(buildModuleConfigZZZ("helloworld2",true,"1.0.1"));
        abstractModuleRefreshSchedulerImpl.setModuleConfigs(moduleConfigs);

        abstractModuleRefreshSchedulerImpl.run();

        //此处由于此前已经存在该模块，所以必须要激活才能使用
        //测试1.0.0
        Module demo1 = moduleManager.find("helloWorld","1.0.0");
        String result = demo1.doAction("helloWorld", "1.0.0");
        System.out.println("result = " + result);

        //测试1.0.1
        Module demo2 = moduleManager.find("helloWorld2","1.0.1");
        String result2 = demo2.doAction("helloWorld", "1.0.1");
        System.out.println("result2 = " + result2);

    }

    /**
     *
     * @throws InterruptedException
     */
    @Test
    public void shouldUpdateModule3() throws InterruptedException {
        abstractModuleRefreshSchedulerImpl.setModuleConfigs(ImmutableList.of(buildModuleConfigZZZ("helloworld",true,"1.0.0")));
        abstractModuleRefreshSchedulerImpl.run();
        //修改模块
        ModuleConfig moduleConfig = buildModuleConfigZZZ("helloworld",true,"1.0.1");
        abstractModuleRefreshSchedulerImpl.setModuleConfigs(ImmutableList.of(moduleConfig));
        abstractModuleRefreshSchedulerImpl.run();
        //此处由于此前已经存在该模块，所以必须要激活才能成为默认，不需带版本号
        moduleManager.activeVersion(moduleConfig.getName(), moduleConfig.getVersion());

        //测试1.0.1
        Module demo2 = moduleManager.find("helloWorld","1.0.1");
        String result2 = demo2.doAction("helloWorld", "1.0.1");
        System.out.println("result2 = " + result2);

        //测试1.0.0
        Module demo1 = moduleManager.find("helloWorld","1.0.0");
        String result = demo1.doAction("helloWorld", "1.0.0");
        System.out.println("result = " + result);
    }
}