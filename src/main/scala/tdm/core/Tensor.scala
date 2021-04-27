package tdm.core

import java.util.Properties

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{typedLit, udf}

import scala.reflect.runtime.universe.TypeTag
import shapeless.{::, HList, HMap, HNil}
import shapeless.IsDistinctConstraint
import shapeless.ops.hlist.{Partition, Union}

import tdm._
import tdm.core.Tensor.TENSOR_VALUES_COLUMN_NAME
import tdm.core.decomposition.{CPTensor, Norm}
import tdm.core.decomposition.Norm.Norm

class Tensor[T, DL <: HList] private[core]
(val typeList: List[TensorDimension[_]], val dimensions: HMap[DimensionMap])
(implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	
	private[core] var values: DataFrame = spark.emptyDataFrame
	private[core] var empty: Boolean = true
	
	private[core] def this(connectionProperties: Properties,
						   query: String,
						   tensorValueName: String,
						   dimensionsName: scala.collection.immutable.Map[TensorDimension[_], String],
						   typeList: List[TensorDimension[_]],
						   dimensions: HMap[DimensionMap])
						  (implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) = {
		this(typeList, dimensions)
		
		val jdbcReader = spark.read.format("jdbc")
		connectionProperties.forEach((key, value) => jdbcReader.option(key.toString, value.toString))
		values = jdbcReader.option("query", query).load()
		
		// Convert dimensions' name
		for ((dimension, name) <- dimensionsName.iterator) {
			values = values.withColumnRenamed(name, dimension.name)
		}
		
		empty = false
		values = values.withColumnRenamed(tensorValueName, Tensor.TENSOR_VALUES_COLUMN_NAME)
	}
	
	private[core] def this(df: DataFrame, tensorValueName: String, dimensionsName: scala.collection.immutable.Map[TensorDimension[_], String], typeList: List[TensorDimension[_]], dimensions: HMap[DimensionMap])
						  (implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) = {
		this(typeList, dimensions)
		values = df
		
		// Convert dimensions' name
		for ((dimension, name) <- dimensionsName.iterator) {
			values = values.withColumnRenamed(name, dimension.name)
		}
		
		empty = false
		values = values.withColumnRenamed(tensorValueName, Tensor.TENSOR_VALUES_COLUMN_NAME)
	}
	
	/**
	 * Get the value of the tensor for the given dimensions' values.
	 *
	 * @param dimensionsValues : the product of dimensions' values
	 *
	 * @return Option[T]: None if the dimensions' values doesn't correspond to a tensor's value, Some(T) otherwise
	 */
	def apply[P <: Product](dimensionsValues: P)
						   (implicit dimensionsConstraint: ContainsAllDimensionsConstraint[DL, P]): Option[T] = {
		
		if (empty) {
			None
		} else {
			var dimensionFound: Boolean = true
			var currentDataFrame = values
			for (dimensionValue <- dimensionsValues.productIterator if dimensionFound) {
				val (dimension: TensorDimension[_], value) = dimensionValue
				type DT = dimension.DimensionType
				
				currentDataFrame = currentDataFrame.filter(currentDataFrame.col(dimension.name) === value)
				if (currentDataFrame.isEmpty) {
					dimensionFound = false
				}
			}
			
			if (dimensionFound && !currentDataFrame.isEmpty) {
				Some(currentDataFrame.select(Tensor.TENSOR_VALUES_COLUMN_NAME).first().get(0).asInstanceOf[T])
			} else {
				None
			}
		}
	}
	
	/**
	 * Add a value to the tensor.
	 *
	 * @param dimensionsValues : the product of dimensions' values
	 * @param tensorValue : the value to add to the tensor
	 */
	def addValue[P <: Product](dimensionsValues: P)(tensorValue: T)
							  (implicit dimensionsConstraint: ContainsAllDimensionsConstraint[DL, P],
							   typeTag: TypeTag[T]): Unit = {
		
		import spark.implicits._
		
		// Convert the type of the tensor value to be accepted by the DataFrame
		var newValue: DataFrame = spark.emptyDataFrame
		
		tensorValue match {
			case v: Double => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
			case v: Float => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
			case v: Long => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
			case v: Int => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
			case v: Short => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
			case v: Byte => newValue = Seq(v).toDF(Tensor.TENSOR_VALUES_COLUMN_NAME)
		}
		
		// Add the dimensions' value
		for (dimensionValue <- dimensionsValues.productIterator) {
			
			val (dimension: TensorDimension[_], value) = dimensionValue
			type DT = dimension.DimensionType
			
			newValue = newValue.withColumn(dimension.name, typedLit(value))
		}
		
		if (empty) {
			values = newValue
			empty = false
		} else {
			if (values.columns.length > 1) {
				values = values.union(newValue.select(values.columns.head, values.columns.tail:_*))
			} else {
				values = values.union(newValue)
			}
		}
	}
	
	/**
	 * Returns the number of elements of the tensor.
	 *
	 */
	def count(): Long = {
		values.count()
	}
	
	/**
	 * Selection operator for tensor.
	 * Create a tensor from the values of the current tensor that respect the given condition.
	 *
	 * @param condition: the function to applied to the tensor's values to determine which to keep
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def selection(condition: T => Boolean): Tensor[T, DL] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val tensor = new Tensor[T, DL](typeList, newDimensions)
		
		// Adding values to the new tensor
		if (!empty) {
			val dimensionIndex: Int = values.columns.indexOf(Tensor.TENSOR_VALUES_COLUMN_NAME)
			tensor.values = values.filter(r => {
				condition(r.get(dimensionIndex).asInstanceOf[T])
			})
			tensor.empty = false
		}
		
		tensor
	}
	
	/**
	 * Restriction operator for tensor.
	 * Create a tensor dimensions' values of the current tensor that fit the given dimensions' condition.
	 *
	 * @param dimensionsRestriction: the restriction to applied on dimensions' values
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def restriction[P <: Product](dimensionsRestriction: P)
								 (implicit restrictionConstraint: ContainsDimensionsConditionConstraint[DL, P]): Tensor[T, DL] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val tensor = new Tensor[T, DL](typeList, newDimensions)
		
		// Addin value to the new tensor
		if (!empty) {
			dimensionsRestriction match {
				case (dimension: TensorDimension[_], condition) => {
					type DT = dimension.DimensionType
					val dimensionIndex: Int = values.columns.indexOf(dimension.name)
					val typedCondition: DT => Boolean = condition.asInstanceOf[DT => Boolean]
					tensor.values = values.filter(r => {
						typedCondition(r.get(dimensionIndex).asInstanceOf[DT])
					})
				}
				case _ => {
					for (dimensionRestriction <- dimensionsRestriction.productIterator) {
						dimensionRestriction match {
							case (dimension: TensorDimension[_], condition) => {
								type DT = dimension.DimensionType
								val dimensionIndex: Int = values.columns.indexOf(dimension.name)
								val typedCondition: DT => Boolean = condition.asInstanceOf[DT => Boolean]
								tensor.values = values.filter(r => {
									typedCondition(r.get(dimensionIndex).asInstanceOf[DT])
								})
							}
						}
					}
				}
			}
			tensor.empty = false
		}
		
		tensor
	}
	
	/**
	 * Projection operator for tensor.
	 * Keep only the values that match the given dimension's value, and remove this dimension from the tensor result.
	 *
	 * @param _dimension: The dimension on which to do the projection
	 * @param value: the value to keep for the given dimension
	 *
	 * @return A tensor with the dimension parameter removed and with only the values that match the parameter value
	 */
	def projection[CT, D <: TensorDimension[_], DLOut <: HList](_dimension: D)(value: CT)
															   (implicit newTypeList: Partition.Aux[DL, D, D :: HNil, DLOut],
																eq: _dimension.DimensionType =:= CT): Tensor[T, DLOut] = {
		
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList.filterNot(_ == _dimension)) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val tensor = new Tensor[T, DLOut](typeList.filterNot(_ == _dimension), newDimensions)
		
		// Adding values to the new tensor
		if (!empty) {
			tensor.values = values.filter(values.col(_dimension.name) === value).drop(_dimension.name)
			tensor.empty = false
		}
		
		tensor
	}
	
	/**
	 * Union operator for tensor.
	 * Create a tensor from the values of the current tensor and the parameter tensor. If there are common entries for
	 * the same dimensions' value in both tensor, the commonValueOperator function is applied.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the union
	 * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def union[DL2 <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
						   (implicit sameSchema: SameSchemaConstraint[DL, DL2],
							typeTag: TypeTag[T]): Tensor[T, DL] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val newTensor = new Tensor[T, DL](typeList, newDimensions)
		
		// Adding values to the new tensor
		if (!empty && !tensor.empty) {
			val columnsName = typeList.map(_.name)
			newTensor.values = values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value1")
				.join(tensor.values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value2"), columnsName)
			
			val u = udf(commonValueOperator.asInstanceOf[(T, T) => T])
			newTensor.values = newTensor.values
				.withColumn(Tensor.TENSOR_VALUES_COLUMN_NAME, u(newTensor.values.col("value1"), newTensor.values.col("value2")))
			
			newTensor.values = newTensor.values.drop("value1").drop("value2")
			newTensor.values = newTensor.values
				.union(values.select(newTensor.values.columns.head, newTensor.values.columns.tail:_*).except(newTensor.values))
				.union(tensor.values.select(newTensor.values.columns.head, newTensor.values.columns.tail:_*).except(newTensor.values))
			newTensor.empty = false
		} else {
			if (empty && !tensor.empty) {
				newTensor.values = tensor.values
				newTensor.empty = false
			} else if (!empty) {
				newTensor.values = values
				newTensor.empty = false
			}
		}
		
		newTensor
	}
	
	/**
	 * Union operator for tensor.
	 * Create a tensor from the values of the current tensor and the parameter tensor. If there are common entries for
	 * the same dimensions' value in both tensor, the commonValueOperator function is applied.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the union
	 * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def ∪[DL2 <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
					   (implicit sameSchema: SameSchemaConstraint[DL, DL2],
						typeTag: TypeTag[T]): Tensor[T, DL] = {
		this.union(tensor)(commonValueOperator)
	}
	
	/**
	 * Intersection operator for tensor.
	 * Create a tensor from the common values of the current tensor and the parameter tensor.
	 * The commonValueOperator function is applied to create the new values.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the intersection
	 * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def intersection[DL2 <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
								  (implicit sameSchema: SameSchemaConstraint[DL, DL2],
								   typeTag: TypeTag[T]): Tensor[T, DL] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val newTensor = new Tensor[T, DL](typeList, newDimensions)
		
		// Adding values to the new tensor
		if (!empty && !tensor.empty) {
			val columnsName = typeList.map(_.name)
			newTensor.values = values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value1")
				.join(tensor.values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value2"), columnsName)
			
			val u = udf(commonValueOperator.asInstanceOf[(T, T) => T])
			newTensor.values = newTensor.values
				.withColumn(Tensor.TENSOR_VALUES_COLUMN_NAME, u(newTensor.values.col("value1"), newTensor.values.col("value2")))
			
			newTensor.values = newTensor.values.drop("value1").drop("value2")
			newTensor.empty = false
		}
		
		newTensor
	}
	
	/**
	 * Intersection operator for tensor.
	 * Create a tensor from the common values of the current tensor and the parameter tensor.
	 * The commonValueOperator function is applied to create the new values.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the intersection
	 * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def ∩[DL2 <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
					   (implicit sameSchema: SameSchemaConstraint[DL, DL2],
						typeTag: TypeTag[T]): Tensor[T, DL] = {
		this.intersection(tensor)(commonValueOperator)
	}
	
	/**
	 * Natural join operator for tensor.
	 * Create a tensor that join the entries that have the same dimensions' value for the common dimensions. The new tensor schema
	 * is composed of the dimensions of the current tensor to which the dimensions that are only in the second tensor are added.
	 * The commonValueOperator function is applied to create the new values.
	 *
	 * @param tensor: the tensor with which perform the natural join.
	 * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors.
	 *
	 * @return a new tensor, with dimensions of the current and of the second tensor.
	 *
	 */
	def naturalJoin[DL2 <: HList, DLOut <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
												 (implicit commonDimension: ContainsAtLeastOneConstraint[DL, DL2],
												  newTL: Union.Aux[DL, DL2, DLOut],
												  typeTag: TypeTag[T]): Tensor[T, DLOut] = {
		var newDimensions = HMap.empty[DimensionMap]
		val newTypeList = typeList.union(tensor.typeList).distinct
		
		for (dimension <- newTypeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val newTensor = new Tensor[T, DLOut](newTypeList, newDimensions)
		
		// Adding values to the new tensor
		val columnsName = typeList.map(_.name).filter(e => tensor.typeList.map(_.name).contains(e))
		if (!empty && !tensor.empty) {
			newTensor.values = values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value1")
				.join(tensor.values.withColumnRenamed(Tensor.TENSOR_VALUES_COLUMN_NAME, "value2"), columnsName)
			
			val u = udf(commonValueOperator.asInstanceOf[(T, T) => T])
			newTensor.values = newTensor.values
				.withColumn(Tensor.TENSOR_VALUES_COLUMN_NAME, u(newTensor.values.col("value1"), newTensor.values.col("value2")))
			
			newTensor.values = newTensor.values.drop("value1").drop("value2")
			newTensor.empty = false
		}
		
		newTensor
	}
	
	/**
	 * Natural join operator for tensor.
	 * Create a tensor that join the entries that have the same dimensions' value for the common dimensions. The new tensor schema
	 * is composed of the dimensions of the current tensor to which the dimensions that are only in the second tensor are added.
	 * The commonValueOperator function is applied to create the new values.
	 * *
	 * * @param tensor: the tensor with which perform the natural join.
	 * * @param commonValueOperator: the function to applied for entries with the same dimensions' value in both tensors.
	 *
	 * @return a new tensor, with dimensions of the current and of the second tensor.
	 *
	 */
	def ⋈[DL2 <: HList, DLOut <: HList](tensor: Tensor[T, DL2])(commonValueOperator: (T, T) => T)
									   (implicit commonDimension: ContainsAtLeastOneConstraint[DL, DL2],
										newDL: Union.Aux[DL, DL2, DLOut],
										typeTag: TypeTag[T]): Tensor[T, DLOut] = {
		this.naturalJoin(tensor)(commonValueOperator)
	}
	
	/**
	 * Difference operator for tensor.
	 * Create a tensor from the values of the current tensor that are not in the parameter tensor.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the difference
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def difference[DL2 <: HList](tensor: Tensor[_, DL2])(implicit sameSchema: SameSchemaConstraint[DL, DL2]): Tensor[T, DL] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		// Intializing the new tensor
		val newTensor = new Tensor[T, DL](typeList, newDimensions)
		
		// Adding values to the new tensor
		if (!empty && !tensor.empty) {
			newTensor.values = values.drop(Tensor.TENSOR_VALUES_COLUMN_NAME)
			newTensor.values = newTensor.values.except(tensor.values.drop(Tensor.TENSOR_VALUES_COLUMN_NAME).select(newTensor.values.columns.head, newTensor.values.columns.tail:_*))
			
			// Get back the tensor's values
			val columnsName = typeList.map(_.name)
			newTensor.values = newTensor.values.join(values, columnsName)
			newTensor.empty = false
		} else {
			if (empty && !tensor.empty) {
				newTensor.values = tensor.values
				newTensor.empty = false
			}
		}
		
		newTensor
	}
	
	/**
	 * Difference operator for tensor.
	 * Create a tensor from the values of the current tensor that are not in the parameter tensor.
	 * The two tensors must have the same dimensions.
	 *
	 * @param tensor: the tensor with wich to perform the difference
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def -[DL2 <: HList](tensor: Tensor[_, DL2])(implicit sameSchema: SameSchemaConstraint[DL, DL2]): Tensor[T, DL] = {
		this.difference(tensor)
	}
	
	/**
	 * Rename operator for a dimension of the tensor.
	 * Create a tensor with the same values as the current tensor, but with a dimension renamed. The new and
	 * the old dimensions must have the same type.
	 *
	 * @param oldDimension: the dimension to rename
	 * @param _newDimension: the new dimension to use instead of oldDimension
	 *
	 * @return a new tensor, with the same values as the current tensor, but with a dimension renamed.
	 */
	def withDimensionRenamed[OD <: TensorDimension[_], ND <: TensorDimension[_],
		DLOut <: HList](oldDimension: OD, _newDimension: ND)
					   (implicit sameDimensionType: oldDimension.DimensionType =:= _newDimension.DimensionType,
						oldDimensionConstraint: ContainsConstraint[DL, OD],
						newDimensionConstraint: IsDistinctConstraint[ND :: DL],
						newTypeList: Partition.Aux[ND :: DL, OD, OD :: HNil, DLOut]): Tensor[T, DLOut] = {
		// Initializing the dimensions of the new tensor
		var newDimensions = HMap.empty[DimensionMap]
		
		for (dimension <- typeList.filterNot(_ == oldDimension)) {
			type DimensionType = dimension.DimensionType
			val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
			val newDimension = dimension.produceDimension()
			newDimensions = newDimensions + (newTensorDimension -> newDimension)
		}
		
		type DimensionType = _newDimension.DimensionType
		val newTensorDimension = _newDimension.asInstanceOf[TensorDimension[DimensionType]]
		val newDimension = _newDimension.produceDimension()
		newDimensions = newDimensions + (newTensorDimension -> newDimension)
		
		// Intializing the new tensor
		val tensor = new Tensor[T, DLOut]((newDimension +: typeList.filterNot(_ == oldDimension)).asInstanceOf[List[TensorDimension[_]]], newDimensions)
		
		// Adding values to the new tensor
		if (!empty) {
			tensor.values = values.withColumnRenamed(oldDimension.name, _newDimension.name)
			tensor.empty = false
		}
		
		tensor
	}
	
	/**
	 * Run the CP decomposition for this tensor.
	 *
	 * @param rank the rank of the CP decomposition
	 * @param nbIterations the maximum number of iterations
	 * @param norm the norm to use on the columns of the factor matrices
	 * @param minFms the Factor Match Score limit to stop the algorithm
	 * @param highRank improve the computation of the pinverse if set to true. By default, is true when rank >= 100
	 * @param computeCorcondia set to true to compute the core consistency diagnostic (CORCONDIA)
	 * @return A [[KruskalTensor]] containing one tensor per dimension.
	 *         Each [[Tensor]] has 3 dimensions: one with the values of the original dimension, one with the values of the rank,
	 *         and the last one with the values found with the CP.
	 */
	def canonicalPolyadicDecomposition(rank: Int, nbIterations: Int = 25, norm: Norm = Norm.L1,
									   minFms: Double = 0.99, highRank: Option[Boolean] = None,
									   computeCorcondia: Boolean = false): KruskalTensor[DL] = {
		val cpTensor = CPTensor(values, TENSOR_VALUES_COLUMN_NAME)
		val kruskal = cpTensor.runCPALS(rank, nbIterations, norm, minFms, highRank, computeCorcondia)
		
		new KruskalTensor[DL](typeList, kruskal.lambda, kruskal.factorMatrices, kruskal.corcondia)
	}
	
	/**
	 * Collect the values of the tensor to accede them directly.
	 *
	 * @return A [[CollectedTensor]] to manipulate values locally.
	 */
	def collect(): CollectedTensor[T, DL] = {
		new CollectedTensor[T, DL](typeList, dimensions, values.collect())
	}
}

object Tensor {
	private[core] val TENSOR_VALUES_COLUMN_NAME: String = "value"
	
	/**
	 * Selection operator for tensor.
	 * Create a tensor from the values of the current tensor that respect the given condition.
	 *
	 * @param condition: the function to applied to the tensor's values to determine which to keep
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def σ[T, DL <: HList](tensor: Tensor[T, DL])(condition: T => Boolean): Tensor[T, DL] = {
		tensor.selection(condition)
	}
	
	/**
	 * Restriction operator for tensor.
	 * Create a tensor dimensions' values of the current tensor that fit the given dimensions' condition.
	 *
	 * @param dimensionsRestriction: the restriction to applied on dimensions' values
	 *
	 * @return a new tensor, with dimensions in the same order as the current tensor.
	 *
	 */
	def ρ[P <: Product, T, DL <: HList](tensor: Tensor[T, DL])(dimensionsRestriction: P)
									   (implicit restrictionConstraint: ContainsDimensionsConditionConstraint[DL, P]): Tensor[T, DL] = {
		tensor.restriction(dimensionsRestriction)
	}
	
	/**
	 * Projection operator for tensor.
	 * Keep only the values that match the given dimension's value, and remove this dimension from the tensor result.
	 *
	 * @param tensor: The tensor on which to do the projection
	 * @param _dimension: The dimension on which to do the projection
	 * @param value: the value to keep for the given dimension
	 *
	 * @return A tensor with the dimension parameter removed and with only the values that match the parameter value
	 */
	def π[CT, D <: TensorDimension[_], T, DL <: HList, DLOut <: HList](tensor: Tensor[T, DL])(_dimension: D)(value: CT)
																	  (implicit newTypeList: Partition.Aux[DL, D, D :: HNil, DLOut],
																	   eq: _dimension.DimensionType =:= CT): Tensor[T, DLOut] = {
		tensor.projection(_dimension)(value)
	}
}