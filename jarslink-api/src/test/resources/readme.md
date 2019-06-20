#阿里JarsLink1.6模块化开发实践

###JarsLink

最近看了一下jvm的类加载器以及热部署相关的知识，之前有了解过JarsLink能实现动态的加载jar包实现热部署，模块热插拨。JarsLink是并发编程网创始人，阿里方腾飞大神的一个开源项目。源码其实并不多，结合spring实现模块之间的互相隔离，运用自定义类加载器实现突破双亲委托机制。下面一简单的例子来看看如何使用。
###Action模块
首先需要实现一个模块，action模块是一个具体业务的实现类，加载一个实现JarsLink-Action的模块，然后将模块打包成一个jar文件，放到classpath目录下，就像引其他jar包一样，只不过该jar是通过自定义的类加载器去加载的。

简单的action实现类：
```
import com.alipay.jarslink.api.Action;

/**
 * 一个简单的Action实现：实现Action接口，execute方法处理具体的业务逻辑
 * Action泛型第一个参数是返回参数类型，第二个是请求入参，这里都是String简单测试
 */
public class HelloWorldAction implements Action<String, String> {

    @Override
    public String execute(String res) {
        return "HelloWorldAction----1.0.0:"+res;
    }

    @Override
    public String getActionName() {
        return "helloWorld";
    }
}
```
action-模块的Spring配置文件：
```
<!--创建Action-->
<bean id="helloWorldAction" class="com.zz.opensdk.jarslink.action.HelloWorldAction"/>

<bean id="moduleAction" class="com.zz.opensdk.jarslink.action.ModuleAction"/>

<bean id="openSdkModuleAction" class="com.zz.opensdk.jarslink.action.OpenSdkModuleAction"/>
```
实现一个spring启动类Application，当主项目加载jar包是Spring通过扫描该类，将action模块依赖注入到上下文中。
```
@Configuration
@ImportResource({"classpath:META-INF/spring/jarslink.xml"})
public class Application
{
}
```
###主项目
依赖1.6版本
```
<dependency>
	<groupId>com.alipay.jarslink</groupId>
	<artifactId>jarslink-api</artifactId>
	<version>1.6.1.20180301</version>
</dependency>
```

#Spring配置文件
```
<!-- 模块加载引擎 -->
<bean name="moduleLoader" class="com.alipay.jarslink.api.impl.ModuleLoaderImpl"></bean>

<!-- 模块管理器 -->
<bean name="moduleManager" class="com.alipay.jarslink.api.impl.ModuleManagerImpl"></bean>

<!-- 模块服务 -->
<bean name="moduleService" class="com.alipay.jarslink.api.impl.ModuleServiceImpl">
	<property name="moduleLoader" ref="moduleLoader"></property>
	<property name="moduleManager" ref="moduleManager"></property>
</bean>

<!-- 配置模块刷新调度 -->
<bean name="abstractModuleRefreshSchedulerImpl" class="com.zz.opensdk.web.aop.AbstractModuleRefreshSchedulerImpl">
	<property name="moduleLoader" ref="moduleLoader"/>
	<property name="moduleManager" ref="moduleManager"/>
</bean>
```

###单元测试

这里同时注册两个jar模块：
1.两个jar包版本一样，修改moduleConfig的version通过moduleLoader.load和moduleManager.register(module)实现，并且
注意需要设置moduleConfig.setNeedUnloadOldVersion(false)，才能注册相同模块，不同的版本，新老版本才能相互隔离;
相同的jar包，需要手动打包，并且将jar包发布到项目target/classpath的根目录下。这里大家可以手动sleep几秒，手动操作将jar包copy进去。

2.jar包版本不同就可以通过abstractModuleRefreshSchedulerImpl.setModuleConfigs(moduleConfigs)，注册一组模块;

```
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
    /**
     * 装载一个模块，并修改模块jar的版本，实现相同jar不同版本的注入
     * @throws InterruptedException
     */
    @Test
    public void test001(){
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
    /**
     * 使用abstractModuleRefreshSchedulerImpl
     * 注册两个helloworldh和helloworld2，如果name一样，会失败
     * 不同的name可以注册进去
     */
    @Test
    public void test002(){
        //装载模块
        List<ModuleConfig> moduleConfigs = new ArrayList<>();
        moduleConfigs.add(buildModuleConfigZZZ("helloworld",true,"1.0.0"));
        moduleConfigs.add(buildModuleConfigZZZ("helloworld2",true,"1.0.1"));
        abstractModuleRefreshSchedulerImpl.setModuleConfigs(moduleConfigs);
        abstractModuleRefreshSchedulerImpl.run();
        //测试1.0.0
        Module demo1 = moduleManager.find("helloWorld","1.0.0");
        String result = demo1.doAction("helloWorld", "1.0.0");
        System.out.println("result = " + result);
        //测试1.0.1
        Module demo2 = moduleManager.find("helloWorld2","1.0.1");
        String result2 = demo2.doAction("helloWorld", "1.0.1");
        System.out.println("result2 = " + result2);
    }
}
```
###创建模块
name：全局唯一，建议使用英文，忽略大小写。
enabled：当前模块是否可用，默认可用，卸载模块时可以设置成false。
version：模块的版本，如果版本号和之前加载的不一致，框架则会重新加载模块。
Properties：spring属性配置文件。
moduleUrl：模块的本地存放地址。
overridePackages：需要突破双亲委派的包名,一般不推荐使用，范围越小越好，如com.alipay.XX。
```
public ModuleConfig buildModuleConfigZZZ(String name, String version, boolean enabled) {
    ModuleConfig moduleConfig = new ModuleConfig();
    String scanBase = "com.zz.opensdk.jarslink.main";
    moduleConfig.addScanPackage(scanBase);
    moduleConfig.removeScanPackage(scanBase);
    Map<String, Object> properties = new HashMap();
    moduleConfig.setName(name);
    moduleConfig.setEnabled(enabled);
    moduleConfig.setVersion(version);
    properties.put("url", "127.0.0.1");
    moduleConfig.setProperties(properties);
    //开启多个版本
    moduleConfig.setNeedUnloadOldVersion(false);

    URL demoModule = Thread.currentThread().getContextClassLoader().getResource("my_jarslink-"+version+".jar");
    moduleConfig.setModuleUrl(ImmutableList.of(demoModule));
    return moduleConfig;
}
```

###test001运行结果：
```
demo1 = HelloWorldAction----1.0.0::zzz
demo2 = HelloWorldAction----1.0.1::zzz
```

注意：要想实现相同name模块，不同的版本，需要设置moduleConfig.setNeedUnloadOldVersion(false)来支持版本。
实际的开发中，一般的都会是新增一个不同的版本jar，一般的也会卸载unload老的模块，只使用新的版本。除非我们现在对外已经有一分用户在使用1.0.0版本功能，
又不想升级接口。可以保留老版本，新版本功能只开放给新的用户。

###JarsLink架构
JarsLink的执行流程是依赖Spring，初始化一个延时线程池，定时去更新加载和卸载模块
    1.加载模块：
    JarsLink为每个模块创建一个新的URLClassLoader来加载模块，并且为每个模块创建一个单独的IOC容器来存放本模块的BEAN。支持突破双亲委派，申明了overridePackages的包将由子类加载进行加载。
    2.卸载模块：
    关闭资源：关闭HTTP连接池或线程池。
    关闭IOC容器：调用applicationContext.close()方法关闭IOC容器。
    移除类加载器：去掉模块的引用。
    卸载JVM租户（开发中）：卸载该模块使用的JVM租户，释放资源。

1.ModuleLoader：模块加载引擎，负责模块加载。
2.ModuleManager：模块管理者，负责在运行时注册，卸载，查找模块和执行Action。
    moduleManager负责查找获取模块。
3.Module：模块，一个模块有多个Action。
    调用模块中的doAction来处理相应的业务逻辑：doAction("actionName", "版本号");
4.Action：模块里的执行者。






