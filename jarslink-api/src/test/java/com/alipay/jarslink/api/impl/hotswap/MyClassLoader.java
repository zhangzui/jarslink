package com.alipay.jarslink.api.impl.hotswap;

import java.io.IOException;
import java.io.InputStream;

public class MyClassLoader extends ClassLoader{

     @Override
     public Class<?> findClass(String name) throws ClassNotFoundException{
         try {
             String fileName = name.substring(name.lastIndexOf(".") + 1) + ".class";

             InputStream is = getClass().getResourceAsStream(fileName);
             if (is == null) {
                 return super.loadClass(name);
             }

             byte[] b = new byte[is.available()];

             is.read(b);
             return defineClass(name, b, 0, b.length);

         } catch (IOException e) {
             throw new ClassNotFoundException(name);
         }
     }
}