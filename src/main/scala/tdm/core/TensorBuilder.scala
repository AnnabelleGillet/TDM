package tdm.core

import java.util.Properties

import org.apache.spark.sql.{DataFrame, SparkSession}
import shapeless.{::, HList, HMap, HNil}
import shapeless.IsDistinctConstraint
import tdm._

/**
 * TensorBuilder with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilder[T, DL <: HList] private[core](typeList: List[TensorDimension[_]])
												 (implicit val spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType],
													ev: IsDistinctConstraint[D :: DL]):
	TensorBuilder[T, D :: DL] = {
		type DT = dimension.DimensionType
		new TensorBuilder[T, D :: DL](typeList :+ dimension)
	}
	
	/**
	 * Build the tensor with the dimensions added to the tensor builder.
	 *
	 * @return a new tensor.
	 */
	final def build(): Tensor[T, DL] = {
		var dimensions = HMap.empty[DimensionMap]
		for (dimension <- typeList) {
			type DT = dimension.DimensionType
			dimensions = dimensions + (dimension -> new Dimension[DT]())
		}
		new Tensor[T, DL](typeList, dimensions)
	}
}
/**
 * TensorBuilderFromDataSource with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilderFromDataSource[T, DL <: HList] private[core](connectionProperties: Properties, dimensionsName: Map[TensorDimension[_], String], typeList: List[TensorDimension[_]])
															   (implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType],
													ev: IsDistinctConstraint[D :: DL]):
	TensorBuilderFromDataSource[T, D :: DL] = {
		type DT = dimension.DimensionType
		new TensorBuilderFromDataSource[T, D :: DL](connectionProperties, dimensionsName + (dimension -> name), typeList :+ dimension)
	}
	
	/**
	 * Build the tensor with the dimensions added to the tensor builder.
	 *
	 * @return a new tensor.
	 */
	final def build(query: String, tensorValueName: String): Tensor[T, DL] = {
		var dimensions = HMap.empty[DimensionMap]
		for (dimension <- typeList) {
			type DT = dimension.DimensionType
			dimensions = dimensions + (dimension -> new Dimension[DT]())
		}
		new Tensor[T, DL](connectionProperties, query, tensorValueName, dimensionsName, typeList, dimensions)
	}
}
/**
 * TensorBuilderFromDataFrame with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilderFromDataFrame[T, DL <: HList] private[core](df: DataFrame, dimensionsName: Map[TensorDimension[_], String], typeList: List[TensorDimension[_]])
															  (implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType],
													ev: IsDistinctConstraint[D :: DL]):
	TensorBuilderFromDataFrame[T, D :: DL] = {
		type DT = dimension.DimensionType
		new TensorBuilderFromDataFrame[T, D :: DL](df, dimensionsName + (dimension -> name), typeList :+ dimension)
	}
	
	/**
	 * Build the tensor with the dimensions added to the tensor builder.
	 *
	 * @return a new tensor.
	 */
	final def build(tensorValueName: String): Tensor[T, DL] = {
		var dimensions = HMap.empty[DimensionMap]
		for (dimension <- typeList) {
			type DT = dimension.DimensionType
			dimensions = dimensions + (dimension -> new Dimension[DT]())
		}
		new Tensor[T, DL](df, tensorValueName, dimensionsName, typeList, dimensions)
	}
}

/**
 * TensorBuilder with just the tensor type defined.
 */
class EmptyTensorBuilder[T] private[core](implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilder[T, D :: HNil] = {
		type DT = dimension.DimensionType
		new TensorBuilder[T, D :: HNil](List[TensorDimension[_]](dimension))
	}
}
/**
 * TensorBuilderFromDataSource with just the tensor type defined.
 */
class EmptyTensorBuilderFromDataSource[T] private[core](connectionProperties: Properties)(implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilderFromDataSource[T, D :: HNil] = {
		type DT = dimension.DimensionType
		new TensorBuilderFromDataSource[T, D :: HNil](connectionProperties, Map[TensorDimension[_], String](dimension -> name), List[TensorDimension[_]](dimension))
	}
}
/**
 * TensorBuilderFromDataFrame with just the tensor type defined.
 */
class EmptyTensorBuilderFromDataFrame[T] private[core](df: DataFrame)(implicit spark: SparkSession, tensorTypeAuthorized: Numeric[T]) {
	/**
	 * Add a dimension to the tensor builder.
	 *
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
												   (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilderFromDataFrame[T, D :: HNil] = {
		type DT = dimension.DimensionType
		new TensorBuilderFromDataFrame[T, D :: HNil](df, Map[TensorDimension[_], String](dimension -> name), List[TensorDimension[_]](dimension))
	}
}

/**
 * Object to start the building of a tensor.
 */
object TensorBuilder {
	/**
	 * Start the tensor builder with the type of the future tensor.
	 */
	final def apply[T](implicit spark: SparkSession, typeAuthorized: Numeric[T]): EmptyTensorBuilder[T] = {
		new EmptyTensorBuilder[T]()
	}
	
	/**
	 * Start the tensor builder with the type of the future tensor.
	 */
	final def apply[T](connectionProperties: Properties)(implicit spark: SparkSession, typeAuthorized: Numeric[T]): EmptyTensorBuilderFromDataSource[T] = {
		new EmptyTensorBuilderFromDataSource[T](connectionProperties)
	}
	
	/**
	 * Start the tensor builder with the type of the future tensor.
	 */
	final def apply[T](df: DataFrame)(implicit spark: SparkSession, typeAuthorized: Numeric[T]): EmptyTensorBuilderFromDataFrame[T] = {
		new EmptyTensorBuilderFromDataFrame[T](df)
	}
}