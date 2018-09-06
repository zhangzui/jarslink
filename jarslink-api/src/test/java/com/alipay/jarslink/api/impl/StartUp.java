package com.alipay.jarslink.api.impl;

import com.alipay.jarslink.api.Module;
import com.alipay.jarslink.api.ModuleConfig;
import com.alipay.jarslink.api.ModuleLoader;
import com.alipay.jarslink.api.ModuleManager;
import com.google.common.collect.ImmutableList;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static com.alipay.jarslink.api.impl.ModuleLoaderImplTest.buildModuleConfigZZZ;

/**
 * @author zhangzuizui
 * @date 2018/9/6 11:02
 */
public class StartUp {

    public static void main(String[] args) throws InterruptedException {

        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("META-INF/spring/jarslink.xml","META-INF/spring/jarslink-schedule.xml");

        ModuleManager moduleManager = (ModuleManager) classPathXmlApplicationContext.getBean("moduleManager");
        //ModuleLoader moduleLoader = (ModuleLoader) classPathXmlApplicationContext.getBean("moduleLoader");
        AbstractModuleRefreshSchedulerImpl abstractModuleRefreshSchedulerImpl = (AbstractModuleRefreshSchedulerImpl) classPathXmlApplicationContext.getBean("abstractModuleRefreshSchedulerImpl");

        abstractModuleRefreshSchedulerImpl.setModuleConfigs(ImmutableList.of(buildModuleConfigZZZ
                ("helloworld",true,"1.0.0")));
        abstractModuleRefreshSchedulerImpl.run();

        while (true){
            Module helloWorldModule = moduleManager.find("helloWorld","1.0.0");
            String result = helloWorldModule.doAction("helloWorld", "zzz");
            System.out.println("result = " + result);
            Thread.sleep(5000);
        }
    }
}
