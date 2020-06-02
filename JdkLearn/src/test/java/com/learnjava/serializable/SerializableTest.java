package com.learnjava.serializable;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author LuoHaiYang
 */
@SpringBootTest
public class SerializableTest {

    @Test
    public void testHessanSerial() throws IOException {

        File file = new File("/Users/tianbao/it/BruisProject");

        FileOutputStream outputStream = new FileOutputStream(file);
        FileInputStream fileInputStream = new FileInputStream(file);

        // 输出到磁盘中
        HessianOutput output = new HessianOutput(outputStream);
        // 从磁盘读取
        HessianInput input = new HessianInput(fileInputStream);

        Male male = new Male();
        male.setId(666l);
        male.setMale(true);
        male.setName("m");

        output.writeObject(male);

        Male result = (Male)input.readObject();
        System.out.println(male.getId());
    }
}
