## EasyPoi

使用easypoi对excel导出，excel模板导出，excel导入，word模板导出

**功能**

Excel自适应xls和xlsx两种格式,word只支持docx模式

1.Excel导入

- 注解导入
- Map导入
- 大数据量导入sax模式
- 导入文件保存
- 文件校验
- 字段校验

2.Excel导出

- 注解导出
- 模板导出
- html导出

3.Excel转html

4.word导出

5.pdf导出

**使用**

maven导入

~~~xml
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-base</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-web</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-annotation</artifactId>
    <version>4.0.0</version>
</dependency>
~~~

**注解介绍**

- @Excel 作用到filed上面,是对Excel一列的一个描述
- @ExcelCollection 表示一个集合,主要针对一对多的导出,比如一个老师对应多个科目,科目就可以用集合表示
- @ExcelEntity 表示一个继续深入导出的实体,但他没有太多的实际意义,只是告诉系统这个对象里面同样有导出的字段
- @ExcelIgnore 和名字一样表示这个字段被忽略跳过这个导导出
- @ExcelTarget 这个是作用于最外层的对象,描述这个对象的id,以便支持一个对象可以针对不同导出做出不同处理

##### @Excel

这个是必须使用的注解,如果需求简单只使用这一个注解也是可以的,涵盖了常用的Excel需求,需要大家熟悉这个功能,主要分为基础,图片处理,时间处理,合并处理几块,name_id是上面讲的id用法,这里就不累言了

| 属性           | 类型     | 默认值           | 功能                                                         |
| -------------- | -------- | ---------------- | ------------------------------------------------------------ |
| name           | String   | null             | 列名,支持name_id                                             |
| needMerge      | boolean  | fasle            | 是否需要纵向合并单元格(用于含有list中,单个的单元格,合并list创建的多个row) |
| orderNum       | String   | "0"              | 列的排序,支持name_id                                         |
| replace        | String[] | {}               | 值得替换  导出是{a_id,b_id} 导入反过来                       |
| savePath       | String   | "upload"         | 导入文件保存路径,如果是图片可以填写,默认是upload/className/ IconEntity这个类对应的就是upload/Icon/ |
| type           | int      | 1                | 导出类型 1 是文本 2 是图片,3 是函数,10 是数字 默认是文本     |
| width          | double   | 10               | 列宽                                                         |
| height         | double   | 10               | **列高,后期打算统一使用@ExcelTarget的height,这个会被废弃,注意** |
| isStatistics   | boolean  | fasle            | 自动统计数据,在追加一行统计,把所有数据都和输出 **这个处理会吞没异常,请注意这一点** |
| isHyperlink    | boolean  | false            | 超链接,如果是需要实现接口返回对象                            |
| isImportField  | boolean  | true             | 校验字段,看看这个字段是不是导入的Excel中有,如果没有说明是错误的Excel,读取失败,支持name_id |
| exportFormat   | String   | ""               | 导出的时间格式,以这个是否为空来判断是否需要格式化日期        |
| importFormat   | String   | ""               | 导入的时间格式,以这个是否为空来判断是否需要格式化日期        |
| format         | String   | ""               | 时间格式,相当于同时设置了exportFormat 和 importFormat        |
| databaseFormat | String   | "yyyyMMddHHmmss" | 导出时间设置,如果字段是Date类型则不需要设置 数据库如果是string 类型,这个需要设置这个数据库格式,用以转换时间格式输出 |
| numFormat      | String   | ""               | 数字格式化,参数是Pattern,使用的对象是DecimalFormat           |
| imageType      | int      | 1                | 导出类型 1 从file读取 2 是从数据库中读取 默认是文件 同样导入也是一样的 |
| suffix         | String   | ""               | 文字后缀,如% 90 变成90%                                      |
| isWrap         | boolean  | true             | 是否换行 即支持\n                                            |
| mergeRely      | int[]    | {}               | 合并单元格依赖关系,比如第二列合并是基于第一列 则{0}就可以了  |
| mergeVertical  | boolean  | fasle            | 纵向合并内容相同的单元格                                     |
| fixedIndex     | int      | -1               | 对应excel的列,忽略名字                                       |
| isColumnHidden | boolean  | false            | 导出隐藏列                                                   |

可以添加以下依赖解决一定程度的内存溢出问题

~~~xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>4.0.0</version>
</dependency>
~~~

但是依然很消耗内存

## easyexcel

easypoi非常耗内存，所以可以使用阿里的easyexcel。

教程在github上：https://github.com/alibaba/easyexcel

