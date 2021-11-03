## 核心概念

#### 文档

文档（Document）：对应一行记录（关系型数据库），文档在ES中会被序列化成一个JSON数据进行存储，JSON对象有多个字段，每个字段都有对应的字段类型（字符串，数值，布尔，日期，二进制， 范围），每个文档都有一个唯一ID，可以自己指定，或者通过ES自动生成

例如:

~~~json
{
    "email": "john@smith.com",
    "first_name": "John",
    "last_name": "Smith",
    "info": {
        "bio": "Eco-warrior and defender of the weak",
        "age": 25,
        "interests": [ "dolphins", "whales" ]
    },
    "join_date": "2014/05/01"
}
~~~

#### 索引

索引（Index）：索引是文档的容器，是一类文档的集合。每个索引都有自己的Mapping定义，用于 定义包含的字段名和字段类型，索引的Setting 定义数据的分布

#### 类型

类型（type）：每个索引里面都有一个或多个Type,Type 是index中的一个逻辑数据分类，一个type 下的document，都有相同的field， 在 6.0 开始 Type已经被标注为过时， 7.0 开始一个索引，只能创建 一个Type "_doc"

#### 分片

 数据分片,将一个数据库，分成多份，每个节点只是存储了一部分数据（创建索引时 一次设置，不能修改，除非reindex）。分片是基于索引来分的

## 基本操作

#### 创建文档

创建文档不用像关系型数据库那样先建数据库才能创建记录，可以在创建文档的时候创建索引。

格式如下：

1、**自动生成ID的创建文档生成方式**

~~~json
POST /索引名称/_doc
{
	//数据
}
~~~

上面的`_doc`是因为7.0以后只能创建一种type，就是_doc

例：

~~~json
POST /myindex/_doc
{
  "userName":"zhangsan",
  "email":"zhangsan@163.com",
  "age":"18"
}
~~~

结果：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "2dUE5nwBRGXsk16PgwPJ",//自动生成的ID
  "_version" : 1,
  "result" : "created",//表示该文档是创建
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 0,
  "_primary_term" : 1
}
~~~

2、**创建文档自定义ID，如果ID已经存在，则操作失败**

格式：

~~~json
PUT /索引名称/_create/自定义ID
{
 // 数据
}
~~~

结果：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 1,//自定义的ID
  "result" : "created",
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 1,
  "_primary_term" : 1
}
~~~

#### 创建索引文档

PUT /索引名称/_doc/ID 

如果文档不存在则创建，否则现有文档会被删除，新的文档被创建，版本信息自增

例如：

~~~json
PUT /myindex/_doc/1
{
  "userName":"zhangsan",
  "age":"18"
}
~~~

因为你ID=1这条文档已经存在，所以原来的文档会被删除：GET /myindex/_doc/1

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 2,
  "_seq_no" : 2,
  "_primary_term" : 1,
  "found" : true,
  "_source" : {
    "userName" : "zhangsan",
    "age" : "18"
  }
}
~~~

新的文档版本自增：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 2,//版本自增
  "result" : "updated",//显示为更新
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 2,
  "_primary_term" : 1
}
~~~

#### 获取文档

GET 索引名称/_doc/ID

#### 更新文档

格式：

~~~json
POST /索引名称/_update/1
{
	“doc”:{
		//data
	}
}

~~~

文档必须已经存在，更新只会对相应字段做增量修改。Update 方法不会删除原来的文档，更新数据时，需要将数据放到 doc 字段下面

例如：

~~~json
POST /myindex/_update/1
{
  "doc":{
    "userName":"zhangsan",
    "age":"17"
  }
}
~~~

结果：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 3,//版本变了
  "result" : "updated",
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 3,
  "_primary_term" : 1
}
~~~

查看文档，只有age变了

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 3,
  "_seq_no" : 3,
  "_primary_term" : 1,
  "found" : true,
  "_source" : {
    "userName" : "zhangsan",
    "age" : "17"
  }
}
~~~

#### 删除文档

DELETE /索引名称/_doc/ID

例如：

DELETE /myindex/_doc/1，结果：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 4,
  "result" : "deleted",
  "_shards" : {
    "total" : 2,
    "successful" : 1,
    "failed" : 0
  },
  "_seq_no" : 4,
  "_primary_term" : 1
}
~~~

此时再查下，就找不到该条文档了：

~~~json
{
  "_index" : "myindex",
  "_type" : "_doc",
  "_id" : "1",
  "found" : false
}
~~~

#### 创建索引

PUT /索引名称

### 批量操作

上面的操作都是单个的，有时为了减少网络请求次数，我们可能需要批量操作：

#### Bulk 批量操作 Index/Create/Update/Delete

格式如下：

~~~json
POST _bulk //固定写法
# index 操作
{
	"index":{
		"_index":"test", //不存在则创建，和上面的语义一样
		"_id":"1"
	}
}
# payload
{
	"field1":"value1"
}
# create 操作
{
    "create":{
        "_index":"test",
        "_id":"100"
    }
}
#payload
{"field1":"value1"}
#update
{
    "update":{
        "_index":"test",
        "_id":"1"
    }
}
#payload
{
    "doc":{ //更新要放在doc里面，和上面单个操作的语义一样
        "field1":"valueUpdate",
        "addfield":"addValue"
    }
}
#delete
{
    "delete":{
        "_index":"test2",
        "_id":"100"
    }
}
~~~

其实就是分成了两部分：第一部分说明了操作类型，比如是index操作、create操作等，然后要指定操作的索引名称是哪个（不存在则创建），操作的文档是哪个（每个文档都有一个唯一的ID，所以上面就是用ID表示操作的文档），第二部分payload就是数据部分，比如要增加的字段名称和值、要修改的字段名称和值。

例如（写成上面这种格式在kibana中的控制台会报错，所以写成下面这种方式）：

~~~json
PUT _bulk
{"index":{"_index":"test","_id":"1"}}
{"field":"_id:1"}
{"create":{"_index":"test","_id":"100"}}
{"field":"_id:100"}
{"update":{"_index":"test","_id":1}}
{"doc":{"addfield":"_id:1:addvalue"}}
{"delete":{"_index":"myindex","_id":1}}
~~~

上面做的事就是：首先创建一个索引文档，索引是test，文档ID是1，然后创建一条数据`field="_id:1"`，然后再对test索引指定ID为100创建一个文档，并创建一条数据`field="_id:100"`，然后更新test索引中ID为1的文档中`addfield="_id:1:addvalue"`，由于没有这条记录，所以会创建一条记录，最后删除myindex索引中ID为1的文档。

#### 批量读取

GET _mget

例如，批量读取上面批量操作后的结果

~~~json 
GET _mget
{
    "docs":[
        {
            "_index":"test",
            "_id":1
        }
		,
		{
            "_index":"test",
            "_id":100
		}
	]
}
~~~

结果：

~~~json
{
  "docs" : [
    {
      "_index" : "test",
      "_type" : "_doc",
      "_id" : "1",
      "_version" : 2,
      "_seq_no" : 2,
      "_primary_term" : 1,
      "found" : true,
      "_source" : {
        "field" : "_id:1",
        "addfield" : "_id:1:addvalue"
      }
    },
    {
      "_index" : "test",
      "_type" : "_doc",
      "_id" : "100",
      "_version" : 1,
      "_seq_no" : 1,
      "_primary_term" : 1,
      "found" : true,
      "_source" : {
        "field" : "_id:100"
      }
    }
  ]
}
~~~

