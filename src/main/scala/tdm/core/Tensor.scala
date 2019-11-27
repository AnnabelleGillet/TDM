package tdm.core

import java.sql.{Connection, Statement, ResultSet}

import scala.collection.mutable.Map

import shapeless.{HList, HMap, HNil, ::}
import shapeless.BasisConstraint
import shapeless.ops.hlist.{Partition, Reverse, Tupler, Union}
import shapeless.ops.product.ToHList

import tdm._
import tdm.ContainsConstraint._

class Tensor[P <: Product, T, TL <: HList, ValueList <: HList, ConditionList <: HList] private[core]
        (val typeList: List[TensorDimension[_]], val dimensions: HMap[DimensionMap])
        (implicit tensorTypeAuthorized: AuthorizedType[T]) {
        
	private[core] def this(connection: Connection, query: String, tensorValueName: String, dimensionsName: scala.collection.immutable.Map[TensorDimension[_], String], typeList: List[TensorDimension[_]], dimensions: HMap[DimensionMap])
            (implicit tensorTypeAuthorized: AuthorizedType[T]) = {
		this(typeList, dimensions)
		val stmt: Statement = connection.createStatement()
		val rs: ResultSet = stmt.executeQuery(query)
		
		while (rs.next()) {
			val keyArray = new Array[String](typeList.size)
            for ((dimension, name) <- dimensionsName.iterator) {
            	val value: Any = rs.getObject(name)
            	type DT = dimension.DimensionType
            	val (associatedDimension, dimensionIndex) = addIndex((dimension, value.asInstanceOf[DT]))
            	keyArray(typeList.indexOf(associatedDimension)) = dimensionIndex + "_"
            }
			val tensorValue: Any = rs.getObject(tensorValueName)
			values.put(keyArray.mkString(""), tensorValue.asInstanceOf[T])
		}
		
		rs.close
		stmt.close
	}
	
	private val values = Map[String, T]()
	
	/**
	 * Get the value of the tensor for the given dimensions' values.
	 * 
	 * @param dimensionsValues : the product of dimensions' values
	 * 
	 * @return Option[T]: None if the dimensions' values doesn't correspond to a tensor's value, Some(T) otherwise
	 */
	def apply[P1 <: Product](dimensionsValues: P1)
		    (implicit dimensionsConstraint: ContainsAllDimensionsConstraint[P, P1, ValueList]): Option[T] = {
		
		var dimensionFound: Boolean = true
		val keyArray = new Array[String](typeList.size)
        for (dimensionValue <- dimensionsValues.productIterator if dimensionFound) {
        	val dimensionIndex = getIndex(dimensionValue)
        	if (dimensionIndex.isEmpty) {
        		dimensionFound = false
        	} else {
        		val (dimension, index) = dimensionIndex.get
        		keyArray(typeList.indexOf(dimension)) = index + "_"
        	}
        }
		
		if (dimensionFound) {
            values.get(keyArray.mkString(""))
		} else {
			None
		}
	}
	
	/**
	 * Add a value to the tensor.
	 * 
	 * @param dimensionsValues : the product of dimensions' values
	 * @param tensorValue : the value to add to the tensor
	 */
	def addValue[P1 <: Product](dimensionsValues: P1)(tensorValue: T)
	        (implicit dimensionsConstraint: ContainsAllDimensionsConstraint[P, P1, ValueList]): Unit = {
        val newKeyArray = new Array[String](typeList.size)
        for (dimensionValue <- dimensionsValues.productIterator) {
        	val (dimension, index) = addIndex(dimensionValue)
        	newKeyArray(typeList.indexOf(dimension)) = index + "_"
        }
    	
        values.put(newKeyArray.mkString(""), tensorValue)
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
    def projection[CT, D <: TensorDimension[_], ValueListOut <: HList, ConditionListOut <: HList, TLOut <: HList, POut <: Product, RLOut <: HList](_dimension: D)(value: CT)
	    (implicit newTypeList: Partition.Aux[TL, D, D :: HNil, TLOut], 
	    		newValueList: Partition.Aux[ValueList, (D, CT), (D, CT) :: HNil, ValueListOut],
	    		newConditionList: Partition.Aux[ConditionList, (D, CT => Boolean), (D, CT => Boolean) :: HNil, ConditionListOut],
	    		reverse: Reverse.Aux[ValueListOut, RLOut],
	    		aux: Tupler.Aux[RLOut, POut]): Tensor[POut, T, TLOut, ValueListOut, ConditionListOut] = {
		
    	// Initializing the dimensions of the new tensor
    	var newDimensions = HMap.empty[DimensionMap]

    	for (dimension <- typeList.filterNot(_ == _dimension)) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val tensor = new Tensor[POut, T, TLOut, ValueListOut, ConditionListOut](typeList.filterNot(_ == _dimension), newDimensions)
		
		// Adding values to the new tensor
		var dimensionIndice: Int = typeList.indexOf(_dimension)
    	type DimensionType = _dimension.DimensionType
    	var dimensionValueIndex: Option[Long] = dimensions
    	        .get(_dimension.asInstanceOf[TensorDimension[DimensionType]])
    	        .get
    	        .getIndex(value.asInstanceOf[DimensionType])
    	
    	if (dimensionValueIndex.isDefined) {
        	for (key <- values.keys) {
        		val keySplit = key.split("_")
        		
        		if (keySplit(dimensionIndice) == dimensionValueIndex.get.toString) {
        			var newKey: String = ""
        			for (i <- 0 until typeList.size if (i != dimensionIndice)) {
        				val currentTensorDimension = typeList(i)
        				type CurrentDimensionType = currentTensorDimension.DimensionType
        				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
        				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
        				
        				val newDimensionKey = tensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
        				newKey += newDimensionKey + "_"
        			}
        			tensor.values.put(newKey, values.get(key).get)
        		}
        	}
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
    def union[P2 <: Product, TL2 <: HList, ValueList2 <: HList, ConditionList2 <: HList](tensor: Tensor[P2, T, TL2, ValueList2, ConditionList2])(commonValueOperator: (T, T) => T)
            (implicit evT1T2: BasisConstraint[ValueList2, ValueList], evT2T1: BasisConstraint[ValueList, ValueList2]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	// Initializing the dimensions of the new tensor
    	var newDimensions = HMap.empty[DimensionMap]

    	for (dimension <- typeList) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val newTensor = new Tensor[P, T, TL, ValueList, ConditionList](typeList, newDimensions)(tensorTypeAuthorized)
		
		// Adding values to the new tensor
		// From first tensor
    	for (key <- values.keys) {
    		val keySplit = key.split("_")
    		
			var newKey: String = ""
			for (i <- 0 until typeList.size) {
				val currentTensorDimension = typeList(i)
				type CurrentDimensionType = currentTensorDimension.DimensionType
				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
				
				val newDimensionKey = newTensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
				newKey += newDimensionKey + "_"
			}
			newTensor.values.put(newKey, values.get(key).get)
    	}
		// From second tensor
		for (key <- tensor.values.keys) {
    		val keySplit = key.split("_")
    		
			val newKeyArray = new Array[String](tensor.typeList.size)
			for (i <- 0 until tensor.typeList.size) {
				val currentTensorDimension = tensor.typeList(i)
				type CurrentDimensionType = currentTensorDimension.DimensionType
				val currentDimension = tensor.dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
				val currentDimensionValue = tensor.getValue(currentDimension, keySplit(i).toLong).get
				
				val newDimensionKey = newTensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
				newKeyArray(newTensor.typeList.indexOf(currentTensorDimension)) = newDimensionKey + "_"
				//newKey += newDimensionKey + "_"
			}
    		val newKey = newKeyArray.mkString("")
    		// Common values
    		val oldValue = newTensor.values.get(newKey)
    		var newValue = tensor.values.get(key).get
    		if (oldValue.isDefined) {
    			newValue = commonValueOperator(oldValue.get, newValue)
    		}
			newTensor.values.put(newKey, newValue)
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
    def ∪[P2 <: Product, TL2 <: HList, ValueList2 <: HList, ConditionList2 <: HList](tensor: Tensor[P2, T, TL2, ValueList2, ConditionList2])(commonValueOperator: (T, T) => T)
            (implicit evT1T2: BasisConstraint[ValueList2, ValueList], evT2T1: BasisConstraint[ValueList, ValueList2]): Tensor[P, T, TL, ValueList, ConditionList] = {
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
    def intersection[P2 <: Product, TL2 <: HList, ValueList2 <: HList, ConditionList2 <: HList](tensor: Tensor[P2, T, TL2, ValueList2, ConditionList2])(valueOperator: (T, T) => T)
            (implicit evT1T2: BasisConstraint[ValueList2, ValueList], evT2T1: BasisConstraint[ValueList, ValueList2]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	// Initializing the dimensions of the new tensor
    	var newDimensions = HMap.empty[DimensionMap]

    	for (dimension <- typeList) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val newTensor = new Tensor[P, T, TL, ValueList, ConditionList](typeList, newDimensions)(tensorTypeAuthorized)

		// Adding values to the new tensor
		for (key <- values.keys) {
    		val keySplit = key.split("_")
    		
    		// Getting value from the second tensor if it exists
			val keyArrayTensor2 = new Array[String](tensor.typeList.size)
			var continue = true
			for (i <- 0 until typeList.size if continue) {
				val currentTensorDimension = typeList(i)
				type CurrentDimensionType = currentTensorDimension.DimensionType
				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
				
				val dimensionKeyTensor2 = tensor.getSimpleIndex(currentTensorDimension, currentDimensionValue)
				if (dimensionKeyTensor2.isDefined) {
				    keyArrayTensor2(tensor.typeList.indexOf(currentTensorDimension)) = dimensionKeyTensor2.get + "_"
				} else {
					continue = false
				}
			}
    		// If the dimensions' value association exists in both tensors
    		if (continue) {
        		val keyTensor2 = keyArrayTensor2.mkString("")
        		val valueTensor2 = tensor.values.get(keyTensor2).get
        		var valueTensor1 = values.get(key).get
        		
        		val newValue = valueOperator(valueTensor1, valueTensor2)
        		// New key
        		var newKey: String = ""
    			for (i <- 0 until typeList.size) {
    				val currentTensorDimension = typeList(i)
    				type CurrentDimensionType = currentTensorDimension.DimensionType
    				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
    				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
    				
    				val newDimensionKey = newTensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
    				newKey += newDimensionKey + "_"
    			}
    			newTensor.values.put(newKey, newValue)
    		}
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
    def ∩[P2 <: Product, TL2 <: HList, ValueList2 <: HList, ConditionList2 <: HList](tensor: Tensor[P2, T, TL2, ValueList2, ConditionList2])(valueOperator: (T, T) => T)
            (implicit evT1T2: BasisConstraint[ValueList2, ValueList], evT2T1: BasisConstraint[ValueList, ValueList2]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	this.intersection(tensor)(valueOperator)
    }
    
    /**
     * Restriction operator for tensor.
     * Create a tensor dimensions' values of the current tensor that fit tje given dimensions' condition. 
     * 
     * @param dimensionsRestriction: the restriction to applied on dimensions' values
     * 
     * @return a new tensor, with dimensions in the same order as the current tensor.
     * 
     */
    def restriction[P1 <: Product](dimensionsRestriction: P1)
            (implicit restrictionConstraint: ContainsDimensionsConditionConstraint[P1, ConditionList]): Tensor[P, T, TL, ValueList, ConditionList] = {
    	// Initializing the dimensions of the new tensor
    	var newDimensions = HMap.empty[DimensionMap]

    	for (dimension <- typeList) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val tensor = new Tensor[P, T, TL, ValueList, ConditionList](typeList, newDimensions)
		
		// Transforming the product to a Map[TensorDimension[_], Any => Boolean]
		val dimensionsRestrictionMap = scala.collection.mutable.Map[TensorDimension[_], _ => Boolean]()
		for (dimensionRestriction <- dimensionsRestriction.productIterator) {
			dimensionRestriction match {
    			case (dimension: TensorDimension[_], condition) => {
    				type DimensionType = dimension.DimensionType
    				dimensionsRestrictionMap.put(dimension, condition.asInstanceOf[DimensionType => Boolean])
    			}
    		}
        }
		
		// Adding values to the new tensor    	
    	for (key <- values.keys) {
    		val keySplit = key.split("_")
    		
    		// Checking if the key is valid given the dimensions' condition
    		var validKey = true
    		for (i <- 0 until typeList.size if validKey) {
				val currentTensorDimension = typeList(i)
				type CurrentDimensionType = currentTensorDimension.DimensionType
				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
				
				val currentDimensionRestriction = dimensionsRestrictionMap.get(currentTensorDimension)
				if (currentDimensionRestriction.isDefined) {
				    validKey = currentDimensionRestriction.get.asInstanceOf[CurrentDimensionType => Boolean](currentDimensionValue.asInstanceOf[CurrentDimensionType])
				}
			}
    		
    		if (validKey) {
    			var newKey: String = ""
    			for (i <- 0 until typeList.size) {
    				val currentTensorDimension = typeList(i)
    				type CurrentDimensionType = currentTensorDimension.DimensionType
    				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
    				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
    				
    				val newDimensionKey = tensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
    				newKey += newDimensionKey + "_"
    			}
    			tensor.values.put(newKey, values.get(key).get)
    		}
    	}
		
		tensor
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
    def selection(condition: T => Boolean): Tensor[P, T, TL, ValueList, ConditionList] = {
    	// Initializing the dimensions of the new tensor
    	var newDimensions = HMap.empty[DimensionMap]

    	for (dimension <- typeList) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val tensor = new Tensor[P, T, TL, ValueList, ConditionList](typeList, newDimensions)(tensorTypeAuthorized)
		
		// Adding values to the new tensor
    	for (key <- values.keys) {
    		if (condition(values.get(key).get)) {
        		val keySplit = key.split("_")
        		
    			var newKey: String = ""
    			for (i <- 0 until typeList.size) {
    				val currentTensorDimension = typeList(i)
    				type CurrentDimensionType = currentTensorDimension.DimensionType
    				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
    				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get
    				
    				val newDimensionKey = tensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
    				newKey += newDimensionKey + "_"
    			}
    			tensor.values.put(newKey, values.get(key).get)
    		}
    	}
		
		tensor
    }
    
    /**
     * Natural operator for tensor.
     * Create a tensor that join the entries that have the same dimensions' value for the common dimensions. The new tensor schema
     * is composed of the dimensions of the current tensor to which the dimensions that are only in the second tensor are added.
     * The tensor's values of the first tensor are kept.
     * 
     * @param tensor: the tensor with which perform the natural join.
     * 
     * @return a new tensor, with dimensions of the current and of the second tensor.
     * 
     */
    def naturalJoin[P2 <: Product, TL2 <: HList, ValueList2 <: HList, ConditionList2 <: HList, 
    	    POut <: Product, TLOut <: HList, ValueListOut <: HList, ConditionListOut <: HList]
            (tensor: Tensor[P2, _, TL2, ValueList2, ConditionList2])
            (implicit commonDimension: ContainsAtLeastOneConstraint[TL, TL2],
            		newP: UnionProduct[P, P2, POut],
            		newTL: Union.Aux[TL, TL2, TLOut],
            		newValueList: Union.Aux[ValueList, ValueList2, ValueListOut],
            		newConditionList: Union.Aux[ConditionList, ConditionList2, ConditionListOut]): Tensor[POut, T, TLOut, ValueListOut, ConditionListOut] = {
    	var newDimensions = HMap.empty[DimensionMap]
    	val newTypeList = typeList.union(tensor.typeList).distinct
    	
    	for (dimension <- newTypeList) {
    		type DimensionType = dimension.DimensionType
    		val newTensorDimension = dimension.asInstanceOf[TensorDimension[DimensionType]]
    		val newDimension = dimension.produceDimension().asInstanceOf[Dimension[DimensionType]]
    		newDimensions = newDimensions + (newTensorDimension -> newDimension)
    	}
    	
    	// Intializing the new tensor
		val newTensor = new Tensor[POut, T, TLOut, ValueListOut, ConditionListOut](newTypeList, newDimensions)(tensorTypeAuthorized)
		
		// Adding values to the new tensor
    	for (key <- values.keys) {
    		val keySplit = key.split("_")
    		
    		// For each entry of the first tensor...
    		val newKeyArray = new Array[String](newTypeList.size)
			val dimensionsValue = Map[TensorDimension[_], Any]()
			for (i <- 0 until typeList.size) {
				val currentTensorDimension = typeList(i)
				type CurrentDimensionType = currentTensorDimension.DimensionType
				val currentDimension = dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
				val currentDimensionValue = getValue(currentDimension, keySplit(i).toLong).get.asInstanceOf[CurrentDimensionType]
				
				dimensionsValue.put(currentTensorDimension, currentDimensionValue)
				
				val newDimensionKey = newTensor.addSimpleIndex(currentTensorDimension, currentDimensionValue)
				newKeyArray(newTypeList.indexOf(currentTensorDimension)) = newDimensionKey + "_"
			}
    		// ...we look for entries of the second tensor having the same values for the common dimensions
    		for (key2 <- tensor.values.keys) {
    			val keySplit2 = key2.split("_")
    			var correspondingDimensionsValue = true
    			val dimensionsValueToAdd = Map[TensorDimension[_], Any]()
        		for (i <- 0 until tensor.typeList.size if correspondingDimensionsValue) {
        			val currentTensorDimension = tensor.typeList(i)
    				type CurrentDimensionType = currentTensorDimension.DimensionType
    				val currentDimension = tensor.dimensions.get(currentTensorDimension).get.asInstanceOf[Dimension[CurrentDimensionType]]
    				val currentDimensionValue = tensor.getValue(currentDimension, keySplit2(i).toLong).get.asInstanceOf[CurrentDimensionType]
    				
    				if (dimensionsValue.get(currentTensorDimension).isDefined) {
    				    correspondingDimensionsValue = 
    				    	    dimensionsValue.get(currentTensorDimension).get == currentDimensionValue
    				} else {
    					dimensionsValueToAdd.put(currentTensorDimension, currentDimensionValue)
    				}
        		}
    			// If the entry has the same values for the common dimensions, it is added to the new tensor
    			if (correspondingDimensionsValue) {
    				for ((dimension, value) <- dimensionsValueToAdd.iterator) {
    					val newDimensionIndex = newTensor.addSimpleIndex(dimension, value)
    					newKeyArray(newTypeList.indexOf(dimension)) = newDimensionIndex + "_"
    				}
        		    newTensor.values.put(newKeyArray.mkString(""), values.get(key).get)
    			}
    		}
    	}
		
		newTensor
    }
    
	private def addSimpleIndex(dimensionValue: Any): Long = {
		dimensionValue match {
			case (dimension: TensorDimension[_], value) => {
				type DimensionType = dimension.DimensionType
        		val index = dimensions.get(dimension).get.addValue(value.asInstanceOf[DimensionType])
        		index
			}
		}
	}
	
	private def getSimpleIndex(dimensionValue: Any): Option[Long] = {
		dimensionValue match {
			case (dimension: TensorDimension[_], value) => {
				type DimensionType = dimension.DimensionType
        		val index = dimensions.get(dimension).get.getIndex(value.asInstanceOf[DimensionType])
        		if (index.isEmpty) {
        			None
        		} else {
        			Some(index.get)
        		}
			}
		}
	}
    
    private def addIndex(dimensionValue: Any): (TensorDimension[_], Long) = {
		dimensionValue match {
			case (dimension: TensorDimension[_], value) => {
				type DimensionType = dimension.DimensionType
        		val index = dimensions.get(dimension).get.addValue(value.asInstanceOf[DimensionType])
        		(dimension, index)
			}
		}
	}
	
	private def getIndex(dimensionValue: Any): Option[(TensorDimension[_], Long)] = {
		dimensionValue match {
			case (dimension: TensorDimension[_], value) => {
				type DimensionType = dimension.DimensionType
        		val index = dimensions.get(dimension).get.getIndex(value.asInstanceOf[DimensionType])
        		if (index.isEmpty) {
        			None
        		} else {
        			Some(dimension, index.get)
        		}
			}
		}
	}
	
	private def getValue[DT](dimension: Dimension[DT], dimensionValueIndice: Long): Option[DT] = {
		dimension.getValue(dimensionValueIndice)
	}
}

object Tensor {
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
	def π[CT, D <: TensorDimension[_], P <: Product, T, TL <: HList, ValueList <: HList, ConditionList <: HList, ValueListOut <: HList, ConditionListOut <: HList, TLOut <: HList, POut <: Product, RLOut <: HList]
	    (tensor: Tensor[P, T, TL, ValueList, ConditionList])(_dimension: D)(value: CT)
	    (implicit tensorTypeAuthorized: AuthorizedType[T],
	    		newTypeList: Partition.Aux[TL, D, D :: HNil, TLOut], 
	    		newValueList: Partition.Aux[ValueList, (D, CT), (D, CT) :: HNil, ValueListOut],
	    		newConditionList: Partition.Aux[ConditionList, (D, CT => Boolean), (D, CT => Boolean) :: HNil, ConditionListOut],
	    		reverse: Reverse.Aux[ValueListOut, RLOut],
	    		aux: Tupler.Aux[RLOut, POut]): Tensor[POut, T, TLOut, ValueListOut, ConditionListOut] = {
		tensor.projection(_dimension)(value)
	}
}