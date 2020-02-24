package tdm.core

import java.sql.Connection
import java.util.Properties

import shapeless.ops.hlist.{Reverse, Tupler}
import shapeless.{HList, HMap, HNil, ::, :+:}
import shapeless.IsDistinctConstraint

import tdm._

/**
 * TensorBuilder with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilder[T, DL <: HList] private[core](typeList: List[TensorDimension[_]])
        (implicit tensorTypeAuthorized: AuthorizedType[T]) {
    import TensorBuilder._
    
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
        new TensorBuilder[T, D :: DL](typeList :+ dimension)(tensorTypeAuthorized)
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
        (implicit tensorTypeAuthorized: AuthorizedType[T]) {
    import TensorBuilder._
    
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
        new TensorBuilderFromDataSource[T, D :: DL](connectionProperties, dimensionsName + (dimension -> name), typeList :+ dimension)(tensorTypeAuthorized)
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
        new Tensor[/*P, */T, DL](connectionProperties, query, tensorValueName, dimensionsName, typeList, dimensions)
    }
}

/**
 * TensorBuilder with just the tensor type defined.
 */
class EmptyTensorBuilder[T] private[core](implicit tensorTypeAuthorized: AuthorizedType[T]) {
    /**
	 * Add a dimension to the tensor builder.
	 * 
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D)
	        (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilder[T, D :: HNil] = {
		type DT = dimension.DimensionType
        new TensorBuilder[T, D :: HNil](List[TensorDimension[_]](dimension))(tensorTypeAuthorized)
    }
}
/**
 * TensorBuilderFromDataSource with just the tensor type defined.
 */
class EmptyTensorBuilderFromDataSource[T] private[core](connectionProperties: Properties)(implicit tensorTypeAuthorized: AuthorizedType[T]) {
    /**
	 * Add a dimension to the tensor builder.
	 * 
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
	        (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilderFromDataSource[T, D :: HNil] = {
		type DT = dimension.DimensionType
        new TensorBuilderFromDataSource[T, D :: HNil](connectionProperties, Map[TensorDimension[_], String](dimension -> name), List[TensorDimension[_]](dimension))(tensorTypeAuthorized)
    }
}

/**
 * Object to start the building of a tensor.
 */
object TensorBuilder {    
    /**
     * Start the tensor builder with the type of the future tensor.
     */
    final def apply[T](implicit typeAuthorized: AuthorizedType[T]): EmptyTensorBuilder[T] = {
        new EmptyTensorBuilder[T]()
    }
    
    /**
     * Start the tensor builder with the type of the future tensor.
     */
    final def apply[T](connectionProperties: Properties)(implicit typeAuthorized: AuthorizedType[T]): EmptyTensorBuilderFromDataSource[T] = {
        new EmptyTensorBuilderFromDataSource[T](connectionProperties)
    }
}