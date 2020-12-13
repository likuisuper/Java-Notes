## CAS基本知识

CAS具体实现在Unsafe类中。拿compareAndSwapInt()这个方法来说

~~~java
public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);
~~~

因为是native方法，需要查看对应的c++实现，源码在hotspot/src/share/vm/unsafe.cpp中(jdk8)

保存方法对应的静态数组

~~~cpp
static JNINativeMethod methods_18[] = {
    //方法名                //方法签名            //函数入口地址
    {CC"getObject",        CC"("OBJ"J)"OBJ"",   FN_PTR(Unsafe_GetObject)},
    {CC"putObject",        CC"("OBJ"J"OBJ")V",  FN_PTR(Unsafe_SetObject)},
    {CC"getObjectVolatile",CC"("OBJ"J)"OBJ"",   FN_PTR(Unsafe_GetObjectVolatile)},
    {CC"putObjectVolatile",CC"("OBJ"J"OBJ")V",  FN_PTR(Unsafe_SetObjectVolatile)},

    DECLARE_GETSETOOP(Boolean, Z),
    DECLARE_GETSETOOP(Byte, B),
    DECLARE_GETSETOOP(Short, S),
    DECLARE_GETSETOOP(Char, C),
    DECLARE_GETSETOOP(Int, I),
    DECLARE_GETSETOOP(Long, J),
    DECLARE_GETSETOOP(Float, F),
    DECLARE_GETSETOOP(Double, D),

    DECLARE_GETSETNATIVE(Byte, B),
    DECLARE_GETSETNATIVE(Short, S),
    DECLARE_GETSETNATIVE(Char, C),
    DECLARE_GETSETNATIVE(Int, I),
    DECLARE_GETSETNATIVE(Long, J),
    DECLARE_GETSETNATIVE(Float, F),
    DECLARE_GETSETNATIVE(Double, D),

    {CC"getAddress",         CC"("ADR")"ADR,             FN_PTR(Unsafe_GetNativeAddress)},
    {CC"putAddress",         CC"("ADR""ADR")V",          FN_PTR(Unsafe_SetNativeAddress)},

    {CC"allocateMemory",     CC"(J)"ADR,                 FN_PTR(Unsafe_AllocateMemory)},
    {CC"reallocateMemory",   CC"("ADR"J)"ADR,            FN_PTR(Unsafe_ReallocateMemory)},
    {CC"freeMemory",         CC"("ADR")V",               FN_PTR(Unsafe_FreeMemory)},

    {CC"objectFieldOffset",  CC"("FLD")J",               FN_PTR(Unsafe_ObjectFieldOffset)},
    {CC"staticFieldOffset",  CC"("FLD")J",               FN_PTR(Unsafe_StaticFieldOffset)},
    {CC"staticFieldBase",    CC"("FLD")"OBJ,             FN_PTR(Unsafe_StaticFieldBaseFromField)},
    {CC"ensureClassInitialized",CC"("CLS")V",            FN_PTR(Unsafe_EnsureClassInitialized)},
    {CC"arrayBaseOffset",    CC"("CLS")I",               FN_PTR(Unsafe_ArrayBaseOffset)},
    {CC"arrayIndexScale",    CC"("CLS")I",               FN_PTR(Unsafe_ArrayIndexScale)},
    {CC"addressSize",        CC"()I",                    FN_PTR(Unsafe_AddressSize)},
    {CC"pageSize",           CC"()I",                    FN_PTR(Unsafe_PageSize)},

    {CC"defineClass",        CC"("DC_Args")"CLS,         FN_PTR(Unsafe_DefineClass)},
    {CC"allocateInstance",   CC"("CLS")"OBJ,             FN_PTR(Unsafe_AllocateInstance)},
    {CC"monitorEnter",       CC"("OBJ")V",               FN_PTR(Unsafe_MonitorEnter)},
    {CC"monitorExit",        CC"("OBJ")V",               FN_PTR(Unsafe_MonitorExit)},
    {CC"tryMonitorEnter",    CC"("OBJ")Z",               FN_PTR(Unsafe_TryMonitorEnter)},
    {CC"throwException",     CC"("THR")V",               FN_PTR(Unsafe_ThrowException)},
    {CC"compareAndSwapObject", CC"("OBJ"J"OBJ""OBJ")Z",  FN_PTR(Unsafe_CompareAndSwapObject)},
    {CC"compareAndSwapInt",  CC"("OBJ"J""I""I"")Z",      FN_PTR(Unsafe_CompareAndSwapInt)},
    {CC"compareAndSwapLong", CC"("OBJ"J""J""J"")Z",      FN_PTR(Unsafe_CompareAndSwapLong)},
    {CC"putOrderedObject",   CC"("OBJ"J"OBJ")V",         FN_PTR(Unsafe_SetOrderedObject)},
    {CC"putOrderedInt",      CC"("OBJ"JI)V",             FN_PTR(Unsafe_SetOrderedInt)},
    {CC"putOrderedLong",     CC"("OBJ"JJ)V",             FN_PTR(Unsafe_SetOrderedLong)},
    {CC"park",               CC"(ZJ)V",                  FN_PTR(Unsafe_Park)},
    {CC"unpark",             CC"("OBJ")V",               FN_PTR(Unsafe_Unpark)}
};
~~~

可以看到其中有一个compareAndSwapInt方法的函数入口地址为Unsafe_CompareAndSwapInt，

所以直接搜索该方法，得到如下实现

~~~cpp
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);//获取obj在内存中OOP实例p
  /*根据value的内存偏移值offset去内存中取指针addr*/(获取偏移地址)
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  /*获得更新值x,指针addr,期待值e参数后调用Atomic::cmpxchg(x,addr,e)*/(交换)
  //这里可以看到，把期望值e(即原始值)赋值给了cmpxchg方法，
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END
~~~

### Atomic::cmpxchg

最终实现在hotspot/src/os_cpu/linux_x86/vm/atomic_linux_x86的atomic_linux_x86.inline.hpp中，针对不同的操作系统，JVM对应Atomic::cmpxchg有不同实现，这里查看linux_x86的实现。

~~~cpp
inline jint     Atomic::cmpxchg    (jint     exchange_value, volatile jint*     dest, jint     compare_value) {
  int mp = os::is_MP();
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  //返回更新后的值
  return exchange_value;
}
~~~

解释：

~~~cpp
os::is_MP:判断是否为多核处理器，如果是返回true
_asm_:内嵌汇编代码
    
volatile：告诉编译器对访问该变量的代码就不再进行优化，和Java中的volatile不同
(c/c++的volatile修饰符只是阻止编译器对变量进行优化，防止将地址p里的值缓冲到寄存器，而不是从cache或者内存读。volatile这个一般在操作IO寄存器或者多线程编程的时候有用。)
    
LOCK_IF_MP:
比较cmp $0,mp,如果mp是0，则跳到标号1，否则加上lock
#define LOCK_IF_MP(mp) "cmp $0, " #mp "; je 1f; lock; 1: "
根据当前系统是否为多核处理器决定是否为cmpxchg指令添加LOCK前缀
    
cmpxchgl:
汇编指令，用于实现交换，单核处理器使用cmpxchgl命令实现CAS操作，多核处理器使用带lock前缀的cmpxchql命令实现CAS操作
~~~

intel的手册对lock前缀的说明如下：

1. 确保对内存的读-改-写操作原子执行。在Pentium及Pentium之前的处理器中，带有lock前缀的指令在执行期间会锁住总线，使得其他处理器暂时无法通过总线访问内存。很显然，这会带来昂贵的开销。从Pentium 4，Intel Xeon及P6处理器开始，intel在原有总线锁的基础上做了一个很有意义的优化：如果要访问的内存区域（area of memory）在lock前缀指令执行期间已经在处理器内部的缓存中被锁定（即包含该内存区域的缓存行当前处于独占或以修改状态），并且该内存区域被完全包含在单个缓存行（cache line）中，那么处理器将直接执行该指令。由于在指令执行期间该缓存行会一直被锁定，其它处理器无法读/写该指令要访问的内存区域，因此能保证指令执行的原子性。这个操作过程叫做缓存锁定（cache locking），缓存锁定将大大降低lock前缀指令的执行开销，但是当多处理器之间的竞争程度很高或者指令访问的内存地址未对齐时，仍然会锁住总线。
2. 禁止该指令与之前和之后的读和写指令重排序。
3. 把写缓冲区中的所有数据刷新到内存中

## OrderAccess类

JMM具体到Hotspot VM的实现，主要是由OrderAccess类定义的一系列的读写屏障来实现JMM的语义

源码位置：hotspot/src/os_cpu/linux_x86/vm/orderAccess_linux_x86.inline.h,不同的操作系统由不同的实现，这里查看的是linux_x86的实现

这里先说一下c++的一些关键字

~~~cpp
inline:在c/c++中，为了解决一些频繁调用的小函数大量消耗栈空间的问题，特别的引入了inline修饰符，表示内联函数。它只是一个对编译器的建议，最后能否真正内联，看编译器的意思，并且建议将它定义在头文件中。
#ifndef:在编译期间，如果没有定义xxx，就执行下面的操作
::表示作用域，和所属关系，例如：
class A{
    public:
      int test();
}
存在一个函数test是属于A的，如下：
int A::test()//表示test是属于A的
{
    return 0;
}

void Parker::park，表示park是Parker中的方法

::用在类上表示内部类，如
os::PlatformParker ，表示PlatformParker是os的内部类
~~~

