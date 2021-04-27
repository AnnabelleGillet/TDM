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
* Put the jar in the `lib` directory, at the root of your Scala project, and import the Spark dependencies. For sbt:
```sbt
val sparkVersion = "3.0.1"

// Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
```
* Import the TDM functionnalities with 
```scala
import tdm._
import tdm.core._
```
* Create a Spark session
```scala
import org.apache.spark.sql.SparkSession

implicit val spark = SparkSession.builder().master("local[*]").getOrCreate()
```
* Create the tensor dimensions
```scala
object User extends TensorDimension[String]
object Hashtag extends TensorDimension[String]
object Time extends TensorDimension[Long]
```
* Ceate the tensor empty
```scala
val tensor = TensorBuilder[Int]
	.addDimension(User)
	.addDimension(Hashtag)
	.addDimension(Time)
	.build()
```
from a Spark DataFrame
```scala
val df: DataFrame = ...

val tensor = TensorBuilder[Int](df)
    .addDimension(User, "user")
    .addDimension(Hashtag, "hashtag")
    .addDimension(Time, "time")
    .build("value")
```
or from a data source
```scala
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
```scala
tensor.addValue(User.value("u1"), Hashtag.value("ht1"), Time.value(1))(11)
```

# Accessing elements of a tensor
Elements of a tensor can be accessed by first collecting the tensor
```scala
val collectedTensor = tensor.collect()
```
then, the value of the tensor or of a given dimension can be retrieved
```scala
collectedTensor(0) // Get the value of the tensor
collectedTensor(User, 0) // Get the value of the dimension User
```

The values of the tensor can be ordered in ascending or descending order
```scala
val ascCollectedTensor = collectedTensor.orderByValues()
val descCollectedTensor = collectedTensor.orderByValuesDesc()
```

# Available operators
## Data manipulation
All operators produce a new tensor and does not modifiy tensors on which the operator is applied.

* Projection: remove the dimension specified, and only keep tensor's elements that match the dimension value
```scala
tensor.projection(User)("u1")
```

* Restriction: keep tensor's elements which dimensions' value match the given condition(s)
```scala
tensor.restriction(User.condition(v => v == "u1"), Hashtag.condition(v => v == "ht1"))
```

* Selection: keep tensor's elements which value match the given condition
```scala
tensor.selection(v => v > 10)
```

* Union: keep all the elements of the 2 tensors, and apply the given function for the common elements
```scala
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.union(tensor2)((v1, v2) => v1 + v2)
```

* Intersection: keep only the common elements between the 2 tensors, and apply the given function for each value
```scala
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.intersection(tensor2)((v1, v2) => v1 + v2)
```

* Natural join: join 2 tensors on their common dimension(s), and keep elements that are present for the common dimension(s) in both tensors. Apply the given function for each value
```scala
object Email extends TensorDimension[String]
val tensor3 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Email)
    .build()
tensor.naturalJoin(tensor3)((v1, v2) => v1 + v2)
```
* Difference: remove the elements from the first tensor that are also present in the second tensor
```scala
val tensor2 = TensorBuilder[Int]()
    .addDimension(User)
    .addDimension(Hashtag)
    .addDimension(Time)
    .build()
tensor.difference(tensor2)
```
* Rename a dimension: replace the phantom type of a dimension by another
```scala
object UserName extends TensorDimension[String]
tensor.withDimensionRenamed(User, UserName)
```

## Decompositions
### Canonical polyadic (or CANDECOMP/PARAFAC)
Perform the canonical polyadic decomposition for a given rank. Some optional parameters are also available:
* nbIterations: the maximum number of iterations
* norm: the norm to apply on the columns of the factor matrices. The l1 and l2 norms are available
* minFms: the Factor Match Score limit at which to stop the algorithm. To be performant even at large scale, our decomposition check the convergence by measuring the difference between the factor matrices of two iterations. The minFms set the limit
* highRank: improve the computation of the pinverse if set to true. By default, is true when rank >= 100
* computeCorcondia: set to true to compute the core consistency diagnostic ([CORCONDIA](https://analyticalsciencejournals.onlinelibrary.wiley.com/doi/pdf/10.1002/cem.801))
```scala
tensor.canonicalPolyadicDecomposition(3)
```

# Roadmap
* Develop other decompositions.
* Provide analytics methods based on tensor operations (community detection, centrality, etc.)

