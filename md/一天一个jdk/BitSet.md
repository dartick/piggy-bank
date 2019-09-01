# 一天一个JDK - BitSet

## 简介

BitSet 简单来说就是存储bit, 同时支持bit相关的各种位操作.

## 原理

### 类结构

BitSet只实现了**Cloneable**和**Serializable**, 说明其支持clone()和序列化.

### bit存储

>>  BitSets are packed into arrays of "words."  Currently a word is a long, which consists of 64 bits, requiring 6 address bits.The choice of word size is determined purely by performance concerns.

在类的注释上已经说的很明确, BitSet通过 long[] 来存储bit的, 也就是一个BitSet存储容量为64的倍数.

### bit定位

通过long[]来进行存储, 那如何进行定位呢? 主要依靠两个方法:

1.  通过该方法来定位到该bit的所在long

```
private static int wordIndex(int bitIndex) {
    return bitIndex >> ADDRESS_BITS_PER_WORD;
}
```

2.  在通过该long & 1L << bitIndex, 便可得到该bit

```
(wordIndex < wordsInUse) && ((words[wordIndex] & (1L << bitIndex)) != 0)
```

这里 1L << bitIndex 这样理解是错误的:

```
1L << 65 = 1 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000 (二进制)
```

而是这样的: 

```
1L << 65 = 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001 (二进制)
```

区别在于得出依旧是一个long类型, 只有64位, 所以向左移动便可得到一个在对应位为1的long类型, 再根据所定位的long进行**与**操作便可得到改位.




