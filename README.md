[![Coverage Status](https://coveralls.io/repos/github/AnnabelleGillet/TDM/badge.svg)](https://coveralls.io/github/AnnabelleGillet/TDM)

# Tensor Data Model
## Introduction
TDM is a prototype aiming at developing the utilization of tensors connected to data sources for Big Data analytics.
TDM Scala library is based on Spark DataFrames and uses [shapeless](https://github.com/milessabin/shapeless) to provide users with a type-safe implementation that is centered around dimensions' values instead of integer indexes.

The formal definition of the model is described in the following  article [Polystore and Tensor Data Model for Logical Data Independence and Impedance Mismatch in Big Data Analytics](https://link.springer.com/chapter/10.1007/978-3-662-60531-8_3). 

## Related work and weakness
The most similiar works are:
* [Typesafe Abstractions for Tensor Operations](https://arxiv.org/abs/1710.06892);
* [NamedTensor](https://github.com/harvardnlp/NamedTensor).


The use of tensors in frameworks such as [Tensorflow](https://www.tensorflow.org/), [Theano](http://deeplearning.net/software/theano/), [NumPy](https://numpy.org/), [PyTorch](https://pytorch.org), [TensorLy](http://tensorly.org), is error-prone and brings several bad habits, usually banned from good practices, that reduce evolution, reuse, etc. 
For example, most of tensor libraries implement tensors’ dimensions using integer indexes regardless of their real nature (string, timestamp, etc.). Thus, tensors are usually defined as multidimensional arrays; users assign an implicit meaning to dimensions and at best put comments in their code to remember the meaning of their constructions, dimension names or dimension order before or after operators are applied. This can lead to errors which cannot be easily understood because it is technically correct but functionally incorrect. 

Tensors are also disconected from data sources, failing to take advantage of the expressiveness of data models and  well-defined semantics of data manipulation operators.

## Benefits of TDM

First, TDM library is based on the formal TDM data model (defined [here](https://link.springer.com/chapter/10.1007/978-3-662-60531-8_3)) and allows users to specify  semantically rich tensor data objects by adding schema to tensor mathematical object. It also provides users with a set of well-defined operators that can be combined to express complex transformations. 

Second, TDM library is a type-safe implementation. Strong static typing is of primary importance in Big Data analytics because it allows to detect errors at compile time and thus avoid expensive buggy calculation phases which can end up inconsistencies. 

Third, TDM library connects to multiple data sources using a polystore architecture (see for example M. Stonebraker ["one size fits all"](https://dl.acm.org/doi/10.1109/ICDE.2005.1). TDM can take advantage of several database management systems to handle multiple data models i.e., graph, time series, key-value, relational, and provides users with a uniform query interface to build views as well as to transform data according to algorithms needs.


# How to use TDM library
* Put the jar in the `lib` directory, at the root of your Scala project.
* Import the TDM functionnalities with 
```
import tdm._
import tdm.core._
```
* Create a Spark session
```
import org.apache.spark.sql.SparkSession

SparkSession.builder().master("local[*]").getOrCreate()
```
* Create the tensor dimensions
```
object User extends TensorDimension[String]
object Hashtag extends TensorDimension[String]
object Time extends TensorDimension[Long]
```
* Ceate the tensor empty
```
val tensor = TensorBuilder[Int]
	.addDimension(User)
	.addDimension(Hashtag)
	.addDimension(Time)
	.build()
```
or from a data source
```
val query = """
    SELECT user_screen_name AS user, ht.hashtag AS hashtag, published_hour AS time, COUNT(*) AS value 
    FROM tweet t 
        INNER JOIN hashtag ht ON t.id = ht.tweet_id 
    GROUP BY user_screen_name, hashtag, published_hour
    """
val tensor = TensorBuilder[Int](connection)
    .addDimension(User, "user")
    .addDimension(Hashtag, "hashtag")
    .addDimension(Time, "time")
    .build(query, "value")
```

* Manually add values to tensor
```
tensor.addValue(User.value("u1"), Hashtag.value("ht1"), Time.value(1))(11)
```

# Available operators
All operators produce a new tensor and does not modifiy tensors on which the operator is applied.

* Projection: remove the dimension specified, and only keep tensor's elements that match the dimension value
```
tensor.projection(User)("u1")
```

* Restriction: keep tensor's elements which dimensions' value match the given condition(s)
```
tensor.restriction(User.condition(v => v == "u1"), Hashtag.condition(v => v == "ht1"))
```

* Selection: keep tensor's elements which value match the given condition
```
tensor.selection(v => v > 10)
```

* Union: keep all the elements of the 2 tensors, and apply the given function for the common elements
```
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.union(tensor2)((v1, v2) => v1 + v2)
```

* Intersection: keep only the common elements between the 2 tensors, and apply the given function for each value
```
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.intersection(tensor2)((v1, v2) => v1 + v2)
```

* Natural join: join 2 tensors on their common dimension(s), and keep elements that are present for the common dimension(s) in both tensors
```
object Email extends TensorDimension[String]
val tensor3 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Email)
    .build()
tensor.naturalJoin(tensor3)
```
* Difference: remove the elements from the first tensor that are also present in the second tensor
```
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.difference(tensor2)
```
* Rename a dimension: replace the phantom type of a dimension by another
```
object UserName extends TensorDimension[String]
tensor.withDimensionRenamed(User, UserName)
```

# Roadmap
* Use TDM Library as a backend for machine learning libraries such as TensorFlow, or PyTorch.
* Develop and optimize tensor operators and algebraic expression over tensors.

