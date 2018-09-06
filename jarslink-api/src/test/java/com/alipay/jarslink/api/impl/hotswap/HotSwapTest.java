package com.alipay.jarslink.api.impl.hotswap;

public class HotSwapTest {

    public static void main(String[] args) throws Exception {
        while (true) {
            MyClassLoader loader = new MyClassLoader();
            //类实例
            Class<?> class1 = loader.loadClass("com.alipay.jarslink.api.impl.hotswap.Hotswap");

            Object hotSwap = class1.newInstance();
            //执行方法say
            hotSwap.getClass().getMethod("hotswap", new Class[]{}).invoke(hotSwap);

            System. out.println(hotSwap.getClass().getClassLoader());
            System. out.println(hotSwap.getClass().getClassLoader().getParent());
            Thread.sleep(5000);
        }

    }
}