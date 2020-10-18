## 前言

## 正文

就DubboProtocolTest类下的testDubboProtocol()方法，服务引用流程：

1. ProtocolFilterWrapper#refer()
2. ProtocolListenerWrapper#refer()
3. AbstractProtocol#refer()
4. DubboProtocol#protocolBindingRefer()
5. DubboProtocol#getClients()
6. DubboProtocol#getSharedClient()
7. DubboProtocol#buildReferenceCountExchangeClientList()
8. DubboProtocol#buildReferenceCountExchangeClient()
9. DubboProtocol#initClient()
10. Exchangers#connect()
11. HeaderExchanger#connect()
12. Transporters#connect()
13. Transporter$Adaptive，代理出来的类，在服务暴露中有代码
14. NettyTransporter#connect()
15. NettyClient#init()
16. AbstractClient#init()
17. NettyClient#doOpen()