# IDEA常用设置及推荐插件

>本文主要记录IDEA的一些常用设置，IDEA与Eclipse的常用快捷键对比及推荐一些好用的插件。

## 基本设置

#### Apperance

![](https://z3.ax1x.com/2021/09/13/4ibg4H.png)

#### font

![](https://z3.ax1x.com/2021/09/13/4iL26I.png)

#### 主题字体

使用主题后，设置字体要更改这个设置

![](https://z3.ax1x.com/2021/09/13/4iLLXq.png)

#### Console Font

![](https://z3.ax1x.com/2021/09/13/4iOGHf.png)

#### file and code template

includes新增`File Header`

~~~java
/**
 * @Classname ${NAME}
 * @Description TODO
 * @Author likui
 * @Date ${DATE} ${TIME}
**/
~~~

#### tabs多行显示

![](https://z3.ax1x.com/2021/09/13/4iX1G4.png)

#### material theme

![](https://z3.ax1x.com/2021/09/13/4iXLwV.png)

## IDEA和Eclipse常用快捷键对比

> 友情提示：IDEA可以设置为Eclipse风格的快捷键，在File->Settings->Keymap处，如需更改部分快捷键可按如下表格中的英文描述进行搜索，并改为相应快捷键。

| Eclipse       | IDEA                | 英文描述                      | 中文描述                                |
| ------------- | ------------------- | ----------------------------- | --------------------------------------- |
| ctrl+shift+r  | ctrl+shift+n        | Navigate->File                | 找工作空间的文件                        |
| ctrl+shift+t  | ctrl+n              | Navigate->Class               | 找类定义                                |
| ctrl+shift+g  | **alt+f7**          | Edit->Find->Find Usages       | **查找方法在哪里调用.变量在哪里被使用** |
| ctrl+t        | ctrl+t              | Other->Hierarchy Class        | 看类继承结构                            |
| ctrl+o        | **ctrl+f12**        | Navigate->File Structure      | **搜索一个类里面的方法**                |
| shift+alt+z   | **ctrl+alt+t**      | Code->Surround With           | **生成常见的代码块**                    |
| shift+alt+l   | ctrl+alt+v          | Refactor->Extract->Variable   | 抽取变量                                |
| shift+alt+m   | ctrl+alt+m          | Refactor->Extract->Method     | 抽取方法                                |
| alt+左箭头    | **ctrl+alt+左箭头** | Navigate->Back                | **回退上一个操作位置**                  |
| alt+右箭头    | **ctrl+alt+右键头** | Navigate->Forward             | **前进上一个操作位置**                  |
| ctrl+home     | ctrl+home           | Move Caret to Text Start      | 回到类最前面                            |
| ctrl+end      | ctrl+end            | Move Caret to Text End        | 回到类最后面                            |
| ctrl+e        | **ctrl+e**          | View->Recent Files            | **最近打开的文件**                      |
| alt+/         | ctrl+space          | Code->Completion->Basic       | 提示变量生成                            |
| ctrl+1        | alt+enter           | Other->Show Intention Actions | 提示可能的操作                          |
| ctrl+h        | double shift        | Find in Path                  | 全局搜索                                |
| alt+上/下箭头 | alt+shift+上/下箭头 | Code->Move Line Up/Down       | 移动一行代码                            |
| ctrl+/        | ctrl+/              | Other->Fix doc comment        | 方法注释                                |
| ctrl+alt+s    | alt+insert          | Generate                      | 生成getter,setter,tostring等            |

command+shift+f：全局搜索

command+f12：查找类方法

## 推荐插件

### CodeGlance

右侧显示代码条

### Grep Console

设置

![](https://z3.ax1x.com/2021/09/13/4ivGHx.png)

### Material Theme UI

### Maven Helper

### maven-search

### MyBatis Log Plugin

### Nyan Progress Bar

彩虹条

### RestfulTookit

### Translation

### jclasslib Bytecode Viewer

字节码查看工具

### Free MyBatis plugin

可以从mapper接口和mapper.xml文件中相互跳转

### Lombok plugin

### SequenceDiagram

查看源码的好帮手，能够查看调用的方法时序图

### GitToolBox

### EasyCode

代码生成插件



