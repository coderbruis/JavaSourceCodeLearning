## 从linux kernel内核出发，IO底层原理

### 1. BIO

```
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lhy
 *
 * 在windows服务器下，可以使用telnet来合serversocket建立连接
 */
public class BIO {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(666);
        System.out.println("Server started...");
        while (true) {
            System.out.println("socket accepting...");
            Socket socket = serverSocket.accept();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] bytes = new byte[1024];
                        InputStream inputStream = socket.getInputStream();
                        while (true) {
                            System.out.println("reading...");
                            int read = inputStream.read(bytes);
                            if (read != -1) {
                                System.out.println(new String(bytes, 0, read));
                            } else {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
```

#### 1.1 从kernel内核的角度来分析BIO的运行机制

### 2. IO多路复用

#### 2.1 select

#### 2.2 poll

#### 2.3 epoll

### 3. 零拷贝



