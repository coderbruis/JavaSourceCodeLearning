<!-- TOC -->
- [前言](#前言)
- [正文](#正文)    
  - [1 基础](#1-基础)        
    - [1.1 String的修饰符与实现类](#11-string的修饰符与实现类)   
    - [1.2 String类的成员变量](#12-string类的成员变量)            
      - [1.2.1 String是通过char数组来保存字符串的](#121-string是通过char数组来保存字符串的)            
      - [1.2.2 String类的属性hash](#122-string类的属性hash)            
      - [1.2.3 serialVersionUID属性作为String类的序列化ID](#123-serialversionuid属性作为string类的序列化id)            
      - [1.2.4 serialPersistentFields属性](#124-serialpersistentfields属性)       
    - [1.3 创建String对象](#13-创建string对象)
    - [1.4 String被设计为不可变性的原因](#14-string被设计为不可变性的原因)    
  - [2 深入String](#2-深入string)        
    - [2.1 先了解一下JAVA内存区域](#21-先了解一下java内存区域)        
    - [2.2 String与JAVA内存区域](#22-string与java内存区域)       
    - [2.3 String的intern方法](#23-string的intern方法)            
      - [2.3.1 重新理解使用new和字面量创建字符串的两种方式](#231-重新理解使用new和字面量创建字符串的两种方式)            
      - [2.3.2 解析](#232-解析)
<!-- /TOC -->

## 前言

不想成为一个只会使用API的攻城狮，也不甘于现状想深入学习JDK。
【版本JDK1.8】

## 正文

### 1 基础

#### 1.1 String的修饰符与实现类

打开String源码，可以看到String类的由final修饰的，并且实现了Serializable，Comparable，CharSequence接口。
```
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
}
```

1. String类是由final修饰的，表明String类不能被继承，并且String类中的成员方法都默认是final方法。
2. String类是由final修饰的，表明String类一旦被创建，就无法改变，对String对象的任何操作都不会影响到原对象，==任何的change操作都会产生新的String对象。==

#### 1.2 String类的成员变量

```
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    
    private final char value[];

    private int hash; // Default to 0

    private static final long serialVersionUID = -6849794470754667710L;

    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];
}
```

##### 1.2.1 String是通过char数组来保存字符串的
由于String由final修饰的，所以String的值一旦创建就无法更改，String的值就被保存在了char数组里了。

##### 1.2.2 String类的属性hash
hash值将用于String类的hashCode()方法的计算，这里先不作具体讲解。

##### 1.2.3 serialVersionUID属性作为String类的序列化ID

##### 1.2.4 serialPersistentFields属性
了解过JAVA序列化的，应该清楚transient是用于指定哪个字段不被默认序列化，对于不需要序列化的属性直接用transient修饰即可。而serialPersistentFields用于指定哪些字段需要被默认序列化，具体用法如下：
```
private static final ObjectStreamField[] serialPersistentFields = {
    new ObjectStreamField("name", String.class),
    new ObjectStreamField("age", Integer.Type)
}
```
这里需要另外注意的是，如果同时定义了serialPersistentFields与transient，transient会被忽略。 

#### 1.3 创建String对象

1. 直接使用""，换句话说就是使用"字面量"赋值
    ```
    String name = "bruis";
    ```
2. 使用连接符"+"来赋值
    ```
    String name = "ca" + "t";
    ```
3. 使用关键字new来创建对象
    ```
    String name = new String("bruis");
    ```
4. 除了上面最常见的几种创建String对象的方式外，还有以下方法可以创建String对象
    - 使用clone()方法
    - 使用反射
    - 使用反序列化

#### 1.4 String被设计为不可变性的原因

- 主要是为了“效率” 和 “安全性” 的缘故。若 String允许被继承, 由于它的高度被使用率, 可能会降低程序的性能，所以String被定义成final。

- 由于字符串常量池的存在，为了更有效的管理和优化字符串常量池里的对象，将String设计为不可变性。

- 安全性考虑。因为使用字符串的场景非常多，设计成不可变可以有效的防止字符串被有意或者无意的篡改。

- 作为HashMap、HashTable等hash型数据key的必要。因为不可变的设计，jvm底层很容易在缓存String对象的时候缓存其hashcode，这样在执行效率上会大大提升。


### 2 深入String

#### 2.1 先了解一下JAVA内存区域
JAVA的运行时数据区包括以下几个区域：
1. 方法区（Method Area）
2. Java堆区（Heap）
3. 本地方法栈（Native Method Stack）
4. 虚拟机栈（VM Stack）
5. 程序计数器（Program Conter Register）

具体内容不在这里进行介绍。为方便读者能够理解下面的内容，请学习下[总结Java内存区域和常量池](https://blog.csdn.net/CoderBruis/article/details/85240273)

对于String类来说，存在一个字符串常量池，对于字符串常量池，在HotSpot VM里实现的string pool功能的是一个StringTable类，它是一个哈希表，里面存的是驻留字符串(也就是我们常说的用双引号括起来的)的引用（而不是驻留字符串实例本身），也就是说在堆中的某些字符串实例被这个StringTable引用之后就等同被赋予了”驻留字符串”的身份。这个StringTable在每个HotSpotVM的实例只有一份，被所有的类共享。

总结一下：
1. 字符串常量池在每个VM中只有一份，存放的是字符串常量的引用值。
2. 字符串常量池——string pool，也叫做string literal pool。
3. 字符串池里的内容是在类加载完成，经过验证，准备阶段之后在堆中生成字符串对象实例，然后将该字符串对象实例的引用值存到string pool中。
4. string pool中存的是引用值而不是具体的实例对象，具体的实例对象是在堆中开辟的一块空间存放的。

#### 2.2 String与JAVA内存区域
下面看看使用""和new的方式创建的字符串在底层都发生了些什么
```
    public class TestString {
	public static void main(String[] args) {
		String name = "bruis";
		String name2 = "bruis";
		String name3 = new String("bruis");
		//System.out.println("name == name2 : " + (name == name2));// true
		//System.out.println("name == name3 : " + (name == name3));// false
	}
}
```
因为语句String name = "bruis";已经将创建好的字符串对象存放在了常量池中，所以name引用指向常量池中的"bruis"对象，而name2就直接指向已经存在在常量池中的"bruis"对象，所以name和name2都指向了同一个对象。这就能理解为什么name == name2 为true了。

使用new 方式创建字符串。首先会在堆上创建一个对象，然后判断字符串常量池中是否存在字符串的常量，如果不存在则在字符串常量池上创建常量；如果没有则不作任何操作。所以name是指向字符串常量池中的常量，而name3是指向堆中的对象，所以name == name3 为false。


下面来看看反编译之后的内容，使用命令
```
javap -c TestString
```
TestString类进行反编译。

进入TestString.class的目录下，对TestString类进行反编译
```
$ javap -c TestString.class
Compiled from "TestString.java"
public class org.springframework.core.env.TestString {
  public org.springframework.core.env.TestString();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: ldc           #2                  // String bruis
       2: astore_1
       3: ldc           #2                  // String bruis
       5: astore_2
       6: new           #3                  // class java/lang/String
       9: dup
      10: ldc           #2                  // String bruis
      12: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
      15: astore_3
      16: return
}
```
从反编译的结果中可以看到，首先是进行无参构造方法的调用。
```
0: aload_0       // 表示对this进行操作，把this装在到操作数栈中
1: invokespecial #1        // 调用<init>

0: ldc   #2      //将常量池中的bruis值加载到虚拟机栈中
2: astore_1      //将0中的引用赋值给第一个局部变量，即String name="bruis"
3: ldc  #2       //将常量池中的bruis值加载到虚拟机栈中
5: astore_2      //将3中的引用赋值给第二个局部变量，即String name2= "bruis"
6: new           //调用new指令，创建一个新的String对象，并存入堆中。因为常量池中已经存在了"bruis"，所以新创建的对象指向常量池中的"bruis"
9: dup           //复制引用并并压入虚拟机栈中
10: ldc          //加载常量池中的"bruis"到虚拟机栈中
12: invokespecial //调用String类的构造方法
15: astore_3      //将引用赋值给第三个局部变量，即String name3=new String("bruis")
```

使用如下命令来查看常量池的内容
```
javap -verbose TestString
```

结果如下：
```
$ javap -verbose TestString
▒▒▒▒: ▒▒▒▒▒▒▒ļ▒TestString▒▒▒▒org.springframework.core.env.TestString
Classfile /D:/bruislearningcode/springframeworksources/spring-framework-master/spring-framework-master/out/test/classes/org/springframework/core/env/TestString.class
  Last modified 2019-7-3; size 600 bytes
  MD5 checksum 85315424cab60ed8f47955dfd577f6e0
  Compiled from "TestString.java"
public class org.springframework.core.env.TestString
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #6.#24         // java/lang/Object."<init>":()V
   #2 = String             #25            // bruis
   #3 = Class              #26            // java/lang/String
   #4 = Methodref          #3.#27         // java/lang/String."<init>":(Ljava/lang/String;)V
   #5 = Class              #28            // org/springframework/core/env/TestString
   #6 = Class              #29            // java/lang/Object
   #7 = Utf8               <init>
   #8 = Utf8               ()V
   #9 = Utf8               Code
  #10 = Utf8               LineNumberTable
  #11 = Utf8               LocalVariableTable
  #12 = Utf8               this
  #13 = Utf8               Lorg/springframework/core/env/TestString;
  #14 = Utf8               main
  #15 = Utf8               ([Ljava/lang/String;)V
  #16 = Utf8               args
  #17 = Utf8               [Ljava/lang/String;
  #18 = Utf8               name
  #19 = Utf8               Ljava/lang/String;
  #20 = Utf8               name2
  #21 = Utf8               name3
  #22 = Utf8               SourceFile
  #23 = Utf8               TestString.java
  #24 = NameAndType        #7:#8          // "<init>":()V
  #25 = Utf8               bruis
  #26 = Utf8               java/lang/String
  #27 = NameAndType        #7:#30         // "<init>":(Ljava/lang/String;)V
  #28 = Utf8               org/springframework/core/env/TestString
  #29 = Utf8               java/lang/Object
  #30 = Utf8               (Ljava/lang/String;)V
{
  public org.springframework.core.env.TestString();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 3: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       5     0  this   Lorg/springframework/core/env/TestString;

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=4, args_size=1
         0: ldc           #2                  // String bruis
         2: astore_1
         3: ldc           #2                  // String bruis
         5: astore_2
         6: new           #3                  // class java/lang/String
         9: dup
        10: ldc           #2                  // String bruis
        12: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        15: astore_3
        16: return
      LineNumberTable:
        line 5: 0
        line 6: 3
        line 7: 6
        line 10: 16
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      17     0  args   [Ljava/lang/String;
            3      14     1  name   Ljava/lang/String;
            6      11     2 name2   Ljava/lang/String;
           16       1     3 name3   Ljava/lang/String;
}
SourceFile: "TestString.java"
```
可以看到值"bruis"已经存放在了常量池中了
```
#2 = String             #25            // bruis
```
以及局部变量表LocalVariableTable中存储的局部变量：
```
        LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      17     0  args   [Ljava/lang/String;
            3      14     1  name   Ljava/lang/String;
            6      11     2 name2   Ljava/lang/String;
           16       1     3 name3   Ljava/lang/String;
```

这里有一个需要注意的地方，在java中使用"+"连接符时，一定要注意到"+"的连接符效率非常低下，因为"+"连接符的原理就是通过StringBuilder.append()来实现的。所以如：String name = "a" + "b";在底层是先new 出一个StringBuilder对象，然后再调用该对象的append()方法来实现的，调用过程等同于：
```
// String name = "a" + "b";
String name = new StringBuilder().append("a").append("b").toString();
```
可以通过反编译来验证，这里就不再进行验证了。

#### 2.3 String的intern方法

官方文档解释为字符串常量池由String独自维护，当调用intern()方法时，如果字符串常量池中包含该字符串，则直接返回字符串常量池中的字符串。否则将此String对象添加到字符串常量池中，并返回对此String对象的引用。

下面先看看这几句代码，猜猜结果是true还是false
```
        String a1 = new String("AA") + new String("BB");
        System.out.println("a1 == a1.intern() " + (a1 == a1.intern()));
        
        String test = "ABABCDCD";
        String a2 = new String("ABAB") + new String("CDCD");
        String a3 = "ABAB" + "CDCD";
        System.out.println("a2 == a2.intern() " + (a2 == a2.intern()));
        System.out.println("a2 == a3 " + (a2 == a3));
        System.out.println("a3 == a2.intern() " + (a3 == a2.intern()));
```

##### 2.3.1 重新理解使用new和字面量创建字符串的两种方式

1. 使用字面量的方式创建字符串
使用字面量的方式创建字符串，要分两种情况。

① 如果字符串常量池中没有值，则直接创建字符串，并将值存入字符串常量池中；
```
String name = "bruis";
```
对于字面量形式创建出来的字符串，JVM会在编译期时对其进行优化并将字面量值存放在字符串常量池中。运行期在虚拟机栈栈帧中的局部变量表里创建一个name局部变量，然后指向字符串常量池中的值，如图所示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190706171739464.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)
② 如果字符常量池中存在字面量值，此时要看这个是真正的**字符串值**还是**引用**。如果是字符串值则将局部变量指向常量池中的值；否则指向引用指向的地方。比如常量池中的值时指向堆中的引用，则name变量为将指向堆中的引用，如图所示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019070617454730.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0NvZGVyQnJ1aXM=,size_16,color_FFFFFF,t_70)


2. 使用new的方式创建字符串
```
String name = new String("bruis");
```
首先在堆中new出一个对象，然后常量池中创建一个指向堆中"bruis"的引用。



##### 2.3.2 解析
```
        /**
        * 首先对于new出的两个String()对象进行字符串连接操作，编译器无法进行优化，只有等到运行期期间，通过各自的new操作创建出对象之后，然后使		    用"+"连接符拼接字符串，再从字符串常量池中创建三个分别指向堆中"AA"、"BB"，而"AABB"是直接在池中创建的字面量值，这一点可以通过类的反编译来证明，这里就不具体展开了。
		*/
        String a7 = new String("AA") + new String("BB");
        System.out.println("a7 == a7.intern() " + (a7 == a7.intern())); //true

		
		/**
		*  对于下面的实例，首先在编译期就是将"ABABCDCD"存入字符串常量池中，其对于"ABABCDCD"存入的是具体的字面量值，而不是引用。
		*  因为在编译器在编译期无法进行new 操作，所以就无法知道a8的地址，在运行期期间，使用a8.intern()可以返回字符串常量池的字面量。而a9
		*  在编译期经过编译器的优化，a9变量会指向字符串常量池中的"ABABCDCD"。所以a8 == a8.intern()为false；a8 == a9为false；a9 == a8.intern()为
		*  true。
		*/
        String test = "ABABCDCD";
        String a8 = new String("ABAB") + new String("CDCD");
        String a9 = "ABAB" + "CDCD";
        System.out.println("a8 == a8.intern() " + (a8 == a8.intern())); //false
        System.out.println("a8 == a9 " + (a8 == a9)); //false
        System.out.println("a9 == a8.intern() " + (a9 == a8.intern())); //true
        
```

针对于编译器优化，总结以下两点：
 1. 常量可以被认为运行时不可改变，所以编译时被以常量折叠方式优化。
 2. 变量和动态生成的常量必须在运行时确定值，所以不能在编译期折叠优化
