package tdm.core

import java.sql.Connection

import shapeless.ops.hlist.{Reverse, Tupler}
import shapeless.{HList, HMap, HNil, ::, :+:}
import shapeless.IsDistinctConstraint

import tdm.`package`
import tdm._

/**
 * TensorBuilder with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilder[T, ValueList <: HList, ConditionList <: HList, TL <: HList] private[core](typeList: List[TensorDimension[_]])
        (implicit tensorTypeAuthorized: AuthorizedType[T]) {
    import TensorBuilder._
    
    /**
	 * Add a dimension to the tensor builder.
	 * 
	 * @param dimension: the dimension to add.
	 */
    final def addDimension[D <: TensorDimension[_]](dimension: D)
            (implicit typeAuthorized: AuthorizedType[dimension.DimensionType], 
            		ev: IsDistinctConstraint[(D, dimension.DimensionType) :: ValueList]): 
            TensorBuilder[T, (D, dimension.DimensionType) :: ValueList, (D, dimension.DimensionType => Boolean) :: ConditionList, D :: TL] = {
    	type DT = dimension.DimensionType
        new TensorBuilder[T, (D, DT) :: ValueList, (D, DT => Boolean) :: ConditionList, D :: TL](typeList :+ dimension)(tensorTypeAuthorized)
    }
    
    /**
     * Build the tensor with the dimensions added to the tensor builder.
     * 
     * @return a new tensor.
     */
    final def build[P <: Product]()(implicit aux: Tupler.Aux[ValueList, P]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	var dimensions = HMap.empty[DimensionMap]
    	for (dimension <- typeList) {
    		type DT = dimension.DimensionType
    		dimensions = dimensions + (dimension -> new Dimension[DT]())
    	}
        new Tensor[P, T, TL, ValueList, ConditionList](typeList, dimensions)
    }
}

/**
 * TensorBuilderFromDataSource with one or more defined dimension(s). Can be used to build a tensor.
 */
class TensorBuilderFromDataSource[T, ValueList <: HList, ConditionList <: HList, TL <: HList] private[core](connection: Connection, dimensionsName: Map[TensorDimension[_], String], typeList: List[TensorDimension[_]])
        (implicit tensorTypeAuthorized: AuthorizedType[T]) {
    import TensorBuilder._
    
    /**
	 * Add a dimension to the tensor builder.
	 * 
	 * @param dimension: the dimension to add.
	 */
    final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
            (implicit typeAuthorized: AuthorizedType[dimension.DimensionType], 
            		ev: IsDistinctConstraint[(D, dimension.DimensionType) :: ValueList]): 
            TensorBuilderFromDataSource[T, (D, dimension.DimensionType) :: ValueList, (D, dimension.DimensionType => Boolean) :: ConditionList, D :: TL] = {
    	type DT = dimension.DimensionType
        new TensorBuilderFromDataSource[T, (D, DT) :: ValueList, (D, DT => Boolean) :: ConditionList, D :: TL](connection, dimensionsName + (dimension -> name), typeList :+ dimension)(tensorTypeAuthorized)
    }
    
    /**
     * Build the tensor with the dimensions added to the tensor builder.
     * 
     * @return a new tensor.
     */
    final def build[P <: Product](query: String, tensorValueName: String)(implicit aux: Tupler.Aux[ValueList, P]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	var dimensions = HMap.empty[DimensionMap]
    	for (dimension <- typeList) {
    		type DT = dimension.DimensionType
    		dimensions = dimensions + (dimension -> new Dimension[DT]())
    	}
        new Tensor[P, T, TL, ValueList, ConditionList](connection, query, tensorValueName, dimensionsName, typeList, dimensions)
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
	        (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilder[T, (D, dimension.DimensionType) :: HNil, (D, dimension.DimensionType => Boolean) :: HNil, D :: HNil] = {
		type DT = dimension.DimensionType
        new TensorBuilder[T, (D, DT) :: HNil, (D, DT => Boolean) :: HNil, D :: HNil](List[TensorDimension[_]](dimension))(tensorTypeAuthorized)
    }
}
/**
 * TensorBuilderFromDataSource with just the tensor type defined.
 */
class EmptyTensorBuilderFromDataSource[T] private[core](connection: Connection)(implicit tensorTypeAuthorized: AuthorizedType[T]) {
    /**
	 * Add a dimension to the tensor builder.
	 * 
	 * @param dimension: the dimension to add.
	 */
	final def addDimension[D <: TensorDimension[_]](dimension: D, name: String)
	        (implicit typeAuthorized: AuthorizedType[dimension.DimensionType]): TensorBuilderFromDataSource[T, (D, dimension.DimensionType) :: HNil, (D, dimension.DimensionType => Boolean) :: HNil, D :: HNil] = {
		type DT = dimension.DimensionType
        new TensorBuilderFromDataSource[T, (D, DT) :: HNil, (D, DT => Boolean) :: HNil, D :: HNil](connection, Map[TensorDimension[_], String](dimension -> name), List[TensorDimension[_]](dimension))(tensorTypeAuthorized)
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
    final def apply[T](connection: Connection)(implicit typeAuthorized: AuthorizedType[T]): EmptyTensorBuilderFromDataSource[T] = {
        new EmptyTensorBuilderFromDataSource[T](connection)
    }
}