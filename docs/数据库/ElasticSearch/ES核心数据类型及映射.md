## ES查询API

### 对指定的索引进行查询 

`/_search `: 不指定索引名称，将对集群上的所有索引进行查询 

`/index1/_search`：对index1 索引进行查询 

`/index1,index2/_search`：对index1,index2 进行查询 

`/index*/_search`：对以index 开头的索引进行查询

### URI查询

在URL中使用参数进行查询

数据如下：

~~~json
PUT query_index/_doc/1
{
  "title": "java in action",
  "content": "Java is the best language",
  "create_date": "2021-09-01",
  "read": 1000
}
PUT query_index/_doc/2
{
  "title": "java core",
  "content": "Java is very popular",
  "create_date": "2021-08-01",
  "read": 100
}
PUT query_index/_doc/3
{
  "title": "python in action",
  "content": "Java is the best language",
  "create_date": "2021-09-02",
  "read": 120
}
~~~

URL查询语句如下：

~~~
GET query_index/_search?q=java&df=title&sort=read:desc&from=0&size=3&timeout=1s
~~~

其中：

q:指定查询条件，使用Query String Syntax

df:默认字段，不指定，则对所有的字段进行查询

sort:排序，后面指定是升序还是降序

from/size:分页查询

timeout:可以指定查询超时时间，时间到了，直接返回已经查到的数据

基于上面这种URL的查询比较简单，但是不支持复杂的查询

### 请求体查询

ES提供的基于JSON格式的查询。

比如上面的查询可以改成：

~~~json
GET query_index/_search //使用POST也可以
{
  "from": 0,
  "size": 3,
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "read": {
        "order": "desc"
      }
    }
  ],
  "_source": ["title","read"] //只显示title和read字段
}
~~~

#### match查询

match表示匹配条件

先添加一条文档

~~~json
PUT query_index/_doc/4
{
    "title":"Thinking in Java",
    "content":"Java is the best language",
    "create_date":"2021-09-02",
    "read":120
}

~~~

然后查询

~~~json
GET query_index/_search //使用POST也可以
{
  "query": {
    "match": {
      "title": "in action"
    }
  }
}
~~~

查询出来的结果中，title为"Thinking in Java"这条记录也会被查出来，因为**match查询时，默认使用的是or**，也就是说，词条匹配in OR action都会被查询出来

但是我就想查title中都包含了in action的记录怎么办呢？使用下面的方式

~~~json
GET query_index/_search //使用POST也可以
{
  "query": {
    "match": {
      "title":{
        "query": "in action",
        "operator": "and"
      }
    }
  }
}
~~~

即给这个词的查询，新增operator字段，使用and，match匹配时，将同时匹配in action

#### 短语查询

使用`match_phrase`进行短语查询，搜索出同时包含in和action的文档

~~~json
GET query_index/_search
{
  "query": {
    "match_phrase": {
      "title": {
        "query": "in action",
        "slop": 1
      }
    }
  }
}
~~~

其中`slop`指定这个短语中间能出现一个间隔，比如title是`in c action`，那么还是能查到，但如果是`in c d action`，那么就查不到这个文档了。需要注意分词出现的顺序要一致

#### Term查询

当你希望进行精准查询，比如查询价格，查询商品名称时时，可以用term 查询，如果要实现精准查询， 避免使用text类型（因为字符串类型是全文索引，并且es底层会做分词，转换为小写等操作，导致使用精准查询的时候查询不到记录）。比如下面这个例子：

先将索引中的某个字段类型改成text类型

~~~json
PUT term_query_index
{
  "mappings": {
    "properties": {
      "full_text": {
        "type": "text"
      }
    }
  }
}
~~~

mappings是映射，后面会说

然后添加一条文档

~~~json
PUT term_query_index/_doc/1
{
  "full_text": "Quick Brown Foxes!"
}
~~~

这时候使用term查询

~~~json
GET term_query_index/_search?pretty
{
  "query": {
    "term": {
      "full_text": "Quick Brown Foxes!"
    }
  }
}
~~~

这样是查不出结果的，因为`full_text`是text类型的（如果将上面的term换成match，能查出来结果），所以在插入这条记录的时候，ES底层会对它做分词、将大写转换成小写等，而term是精准查询，查询的内容必须要匹配才行，比如将上面的查询条件改成：

~~~json
GET term_query_index/_search?pretty
{
  "query": {
    "term": {
      "full_text": "quick"
    }
  }
}
~~~

那么就能查出来了。

#### query string查询

POST /索引名称/_search，操作符需要大写

例如：

~~~json
POST term_query_index/_search
{
  "query": {
    "query_string": {
      "default_field": "full_text",
      "query": "quick AND brown"
    }
  }
}
~~~

只要full_text字段中同时包含有quick和brown，那么就能查出结果来。将AND换成OR，那么full_text中只要有一个满足条件就能查出来。