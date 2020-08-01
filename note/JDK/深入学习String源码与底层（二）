## 前言

不想成为一个只会使用API的攻城狮，也不甘于现状想深入学习JDK源码。
【版本JDK1.8】

在前一篇文章中，已经对String的创建和String在常量池中的对应关系进行了讲解，本片将继续深入String的源码学习。

## 正文

### 1. String的equals方法

String源码的equals方法如下：
```
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = value.length;
            if (n == anotherString.value.length) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }
```
从源码中可知，equals方法比较是"字符串对象的地址"，如果不相同则比较字符串的内容，实际也就是char数组的内容。


### 2. String的hashcode方法

String源码中hashcode方法如下：
```
    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }
```
在String类中，有个字段hash存储着String的哈希值，如果字符串为空，则hash的值为0。String类中的hashCode计算方法就是以31为权，每一位为字符的ASCII值进行运算，用自然溢出来等效取模，经过第一次的hashcode计算之后，属性hash就会赋哈希值。从源码的英文注释可以了解到哈希的计算公式：
```
s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
```

### 3. String的hashcode()和equals()

这是一个很经典的话题了，下面来深入研究一下这两个方法。由上面的介绍，可以知道String的equals()方法实际比较的是两个字符串的内容，而String的hashCode()方法比较的是字符串的hash值，那么单纯的a.equals(b)为true，就可以断定a字符串等于b字符串了吗？或者单纯的a.hash == b.hash为true，就可以断定a字符串等于b字符串了吗？答案是否定的。
比如下面两个字符串：
```
        String a = "gdejicbegh";
        String b = "hgebcijedg";
        System.out.println("a.hashcode() == b.hashcode() " + (a.hashCode() == b.hashCode()));
        System.out.println("a.equals(b) " + (a.equals(b)));
```
结果为：
true
false

这个回文字符串就是典型的hash值相同，但是字符串却不相同。对于算法这块领域，回文字符串和字符串匹配都是比较重要的一块，比如马拉车算法、KMP算法等，有兴趣的小伙伴可以在网上搜索相关的算法学习一下。

其实Java中任何一个对象都具备equals()和hashCode()这两个方法，因为他们是在Object类中定义的。

在Java中定义了关于hashCode()和equals()方法的规范，总结来说就是：
1. 如果两个对象equals()，则它们的hashcode一定相等。
2. 如果两个对象不equals()，它们的hashcode可能相等。
3. 如果两个对象的hashcode相等，则它们不一定equals。
4. 如果两个对象的hashcode不相等，则它们一定不equals。

### 4. String的compareTo()方法

```
    public int compareTo(String anotherString) {
        int len1 = value.length;
        int len2 = anotherString.value.length;
        int lim = Math.min(len1, len2);
        char v1[] = value;
        char v2[] = anotherString.value;

        int k = 0;
        while (k < lim) {
            char c1 = v1[k];
            char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }
```
从compareTo()的源码可知，这方法时先比较两个字符串内的字符串数组的ASCII值，如果最小字符串都比较完了都还是相等的，则返回字符串长度的差值；否则在最小字符串比较完之前，字符不相等，则返回不相等字符的ASCII值。这里本人也有点困惑这个方法的有什么实际的用处，有了解的小伙伴可以留言，大家互相学习。

### 5. String的startWith(String prefix)方法

```
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }
    
    public boolean startsWith(String prefix, int toffset) {
        char ta[] = value;
        int to = toffset;
        char pa[] = prefix.value;
        int po = 0;
        int pc = prefix.value.length;
        // Note: toffset might be near -1>>>1.
        if ((toffset < 0) || (toffset > value.length - pc)) {
            return false;
        }
        while (--pc >= 0) {
            if (ta[to++] != pa[po++]) {
                return false;
            }
        }
        return true;
    }
```
如果参数字符序列是该字符串字符序列的前缀，则返回true；否则返回false；

示例：
```
        String a = "abc";
        String b = "abcd";
        System.out.println(b.startsWith(a));
```
运行结果：
true

### 6. String的endsWith(String suffix)方法

查看String的endsWith(String suffix)方法源码：
```
    public boolean endsWith(String suffix) {
        return startsWith(suffix, value.length - suffix.value.length);
    }
```
其实endsWith()方法就是服用了startsWith()方法而已，传进的toffset参数值时value和suffix长度差值。

示例：
```
        String a = "abcd";
        String b = "d";
        System.out.println(a.endsWith(b));
```
运行结果：
true

### 7. String的indexOf(int ch)方法

```
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
        final int max = value.length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }

        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            final char[] value = this.value;
            for (int i = fromIndex; i < max; i++) {
                if (value[i] == ch) {
                    return i;
                }
            }
            return -1;
        } else {
            return indexOfSupplementary(ch, fromIndex);
        }
    }
```
对于String的indexOf(int ch)方法，查看其源码可知其方法入参为ASCII码值，然后和目标字符串的ASCII值来进行比较的。其中常量Character.MIN_SUPPLEMENTARY_CODE_POINT表示的是0x010000——十六进制的010000，十进制的值为65536，这个值表示的是十六进制的最大值。

下面再看看indexOfSupplementary(ch, fromIndex)方法
```
    private int indexOfSupplementary(int ch, int fromIndex) {
        if (Character.isValidCodePoint(ch)) {
            final char[] value = this.value;
            final char hi = Character.highSurrogate(ch);
            final char lo = Character.lowSurrogate(ch);
            final int max = value.length - 1;
            for (int i = fromIndex; i < max; i++) {
                if (value[i] == hi && value[i + 1] == lo) {
                    return i;
                }
            }
        }
        return -1;
    }
```
java中特意对超过两个字节的字符进行了处理，例如emoji之类的字符。处理逻辑就在indexOfSupplementary(int ch, int fromIndex)方法里。

Character.class
```
    public static boolean isValidCodePoint(int codePoint) {
        // Optimized form of:
        //     codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT
        int plane = codePoint >>> 16;
        return plane < ((MAX_CODE_POINT + 1) >>> 16);
    }
    
```
对于方法isValidCodePoint(int codePoint)方法，用于确定指定代码点是否是一个有效的Unicode代码点。代码
```
int plane = codePoint >>> 16;
return plane < ((MAX_CODE_POINT + 1) >>> 16);
```
表达的就时判断codePoint是否在MIN_CODE_POINT和MAX_CODE_POINT值之间，如果是则返回true，否则返回false。

### 8. String的split(String regex, int limit)方法

```
    public String[] split(String regex, int limit) {
        char ch = 0;
        if (((regex.value.length == 1 &&
             ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
             (regex.length() == 2 &&
              regex.charAt(0) == '\\' &&
              (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
              ((ch-'a')|('z'-ch)) < 0 &&
              ((ch-'A')|('Z'-ch)) < 0)) &&
            (ch < Character.MIN_HIGH_SURROGATE ||
             ch > Character.MAX_LOW_SURROGATE))
        {
            int off = 0;
            int next = 0;
            // 如果limit > 0，则limited为true
            boolean limited = limit > 0;
            ArrayList<String> list = new ArrayList<>();
            while ((next = indexOf(ch, off)) != -1) {
                if (!limited || list.size() < limit - 1) {
                    list.add(substring(off, next));
                    off = next + 1;
                } else {    // last one
                    // limit > 0，直接返回原字符串
                    list.add(substring(off, value.length));
                    off = value.length;
                    break;
                }
            }
            // 如果没匹配到，则返回原字符串
            if (off == 0)
                return new String[]{this};

            // 添加剩余的字字符串
            if (!limited || list.size() < limit)
                list.add(substring(off, value.length));

            int resultSize = list.size();
            if (limit == 0) {
                while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                    resultSize--;
                }
            }
            String[] result = new String[resultSize];
            return list.subList(0, resultSize).toArray(result);
        }
        return Pattern.compile(regex).split(this, limit);
    }
```

#### 8.1 源码分析1

split(String regex, int limit)方法内部逻辑非常复杂，需要静下心来分析。

if判断中**第一个括号**先判断一个字符的情况，并且这个字符不是任何特殊的正则表达式。也就是下面的代码：
```
(regex.value.length == 1 &&
             ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1)
```
如果要根据特殊字符来截取字符串，则需要使用```\\```来进行字符转义。


在if判断中，**第二个括号**判断有两个字符的情况，并且如果这两个字符是以```\```开头的，并且不是字母或者数字的时候。如下列代码所示：
```
(regex.length() == 2 && regex.charAt(0) == '\\' && (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 && ((ch-'a')|('z'-ch)) < 0 && ((ch-'A')|('Z'-ch)) < 0)
```
判断完之后，在进行**第三个括号**判断，判断是否是两字节的unicode字符。如下列代码所示：
```
(ch < Character.MIN_HIGH_SURROGATE ||
             ch > Character.MAX_LOW_SURROGATE)
```

对于下面这段复杂的代码，我们结合示例一句一句来分析。
```
            int off = 0;
            int next = 0;
            boolean limited = limit > 0;
            ArrayList<String> list = new ArrayList<>();
            while ((next = indexOf(ch, off)) != -1) {
                if (!limited || list.size() < limit - 1) {
                    list.add(substring(off, next));
                    off = next + 1;
                } else {    // last one
                    //assert (list.size() == limit - 1);
                    list.add(substring(off, value.length));
                    off = value.length;
                    break;
                }
            }
            // If no match was found, return this
            if (off == 0)
                return new String[]{this};

            // Add remaining segment
            if (!limited || list.size() < limit)
                list.add(substring(off, value.length));

            // Construct result
            int resultSize = list.size();
            if (limit == 0) {
                while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                    resultSize--;
                }
            }
            String[] result = new String[resultSize];
            return list.subList(0, resultSize).toArray(result);
```

#### 8.2 源码分析2

示例代码1：
```
        String splitStr1 = "what,is,,,,split";
        String[] strs1 = splitStr1.split(",");
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
```
运行结果：
```
what
is

split
6
```

示例代码2：
```
        String splitStr1 = "what,is,,,,";
        String[] strs1 = splitStr1.split(",");
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
```
运行结果：
```
what
is
2
```


示例代码3：
```
        String splitStr1 = "what,is,,,,";
        String[] strs1 = splitStr1.split(",", -1);
        for (String s : strs1) {
            System.out.println(s);
        }
        System.out.println(strs1.length);
```
运行结果：
```
what
is


6
```

对比了一下示例代码和结果之后，小伙伴们是不是很困惑呢？困惑就对了，下面就开始分析代码吧。在split(String regex, int limit)方法的if判断内部，定义了off和next变量，作为拆分整个字符串的两个指针，然后limit作为拆分整个string字符串的一个阈值。在split()方法内部的复杂逻辑判断中，都围绕着这三个变量来进行。

下面将示例代码1的字符串拆分成字符数组，如下(n代表next指针，o代表off指针)：
```
    w h a t , i s , , , ,  s  p  l  i  t
    0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
    n 
    o
```
由于regex为','，所以满足if括号里的判断。一开始next和off指针都在0位置，limit为0，在while里的判断逻辑指的是获取','索引位置，由上图拆分的字符数组可知，next会分别为4,7,8,9,10。由于limited = limit > 0，得知limited为false，则逻辑会走到
```
                if (!limited || list.size() < limit - 1) {
                    list.add(substring(off, next));
                    off = next + 1;
                }
```
进入第一次while循环体，此时的字符数组以及索引关系如下：
```
    w h a t , i s , , , ,  s  p  l  i  t
    0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
            n 
    o
```
所以list集合里就会添加进字符串what。

第二次进入while循环时，此时的字符数组以及索引关系如下：
```
    w h a t , i s , , , ,  s  p  l  i  t
    0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
                  n 
              o
```
list集合里就会添加进字符串is,

第三次进入while循环时，此时的字符数组以及索引关系如下：
```
    w h a t , i s , , , ,  s  p  l  i  t
    0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
                    n 
                  o
```
list集合里就会添加进空字符串""

第四次进入while循环时，此时的字符数组以及索引关系如下：
```
    w h a t , i s , , , ,  s  p  l  i  t
    0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
                      n 
                    o
```
list集合里就会添加进空字符串""

当o指针指向位置10时，while((next = indexOf(ch, off)) != -1)结果为false，因为此时已经获取不到','了。

注意，此时list中包含的元素有：
```
[what,is, , , ,]
```
当程序走到时，
```
            if(!limited || list.size() < limit) {
                list.add(substring(off, value.length);
            }

            int resultSize = list.size();
            if (limit == 0) {
                while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                    resultSize--;
                }
            }
```
会将字符数组off（此时off为10）位置到value.length位置的字符串存进list集合里，也就是split元素,由于list集合最后一个元素为split，其大小不为0，所以就不会进行resultSize--。所以最终list集合里的元素就有6个元素，值为
```
[what,is, , , ,split]
```

这里相信小伙伴们都知道示例1和示例2的去别在那里了，是因为示例2最后索引位置的list为空字符串，所以会调用下面的代码逻辑：
```
                while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                    resultSize--;
                }
```
最终会将list中的空字符串给减少。所以示例2的最终结果为
```
[what,is]
```

对于入参limit，可以总结一下为：
1. limit > 0，split()方法不进行拆分，返回原字符串。
2. limit = 0，split()方法会拆分匹配到的最后一位regex。
3. limit < 0，split()方法会根据regex匹配到的最后一位，如果最后一位为regex，则多添加一位空字符串；如果不是则添加regex到字符串末尾的子字符串。

就以示例代码一为例，对于字符串"what,is,,,,"。

**对于limit > 0**，由于代码：
```
boolean limited = limit > 0;  // limited为true
..
..
if(!limited || list.size() < limit - 1) { // !limited为false
    ...
} else {// 此时执行词句
    list.add(substring(off, value.length));
    off = value.length;
    break;
}
```
所以返回的原字符串：
```
what,is,,,,
1
```


**对于limit = 0**，由于代码：
```
            if (limit == 0) {
                while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
                    resultSize--;
                }
            }
```
会将空字符串从list集合中移除掉，所以返回的是：
```
what
is
2
```

**对于limit < 0**，由于代码：
```
if (!limited || list.size() < limit)
    list.add(substring(off, value.length));
```
会在原来的集合内容上（[what,is,'','','']）再加一个空字符串，也就是[what,is,'','','','']。


### 总结

String作为Java中使用频率最多的类，它在日程开发中起到了至关重要的作用。由于String方法还有很多，这里就不一一总结了。